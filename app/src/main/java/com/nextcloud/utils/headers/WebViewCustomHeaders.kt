/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.headers

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.nextcloud.common.CustomHeaders
import com.owncloud.android.lib.common.utils.Log_OC
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Collections
import java.util.Locale

/**
 * Web views cannot add headers to sub-resources: GET requests to a registered origin, including those of scripts, are
 * fetched through the library client, which follows redirects only within that origin. Other requests of the page's
 * own fetch and XMLHttpRequest calls (for example POST) get the headers from a page script; fetch rejects redirects
 * for them, XMLHttpRequest cannot.
 *
 * The page script makes the headers readable for code running in that origin. That code, and the server behind it,
 * is trusted with them anyway, so an XMLHttpRequest redirect needs the cooperation of an already trusted party.
 */
object WebViewCustomHeaders {
    private const val TAG = "WebViewCustomHeaders"
    private const val METHOD_GET = "GET"
    private const val HEADER_COOKIE = "Cookie"
    private const val HEADER_SET_COOKIE = "Set-Cookie"
    private const val HEADER_LOCATION = "Location"
    private const val HEADER_SEPARATOR = ", "
    private const val DEFAULT_REASON = "OK"
    private const val HTML_MIME_TYPE = "text/html"
    private const val UTF_8 = "UTF-8"
    private const val MAX_REDIRECTS = 10
    private const val HEADERS_PLACEHOLDER = "__HEADERS__"
    private const val ORIGIN_PLACEHOLDER = "__ORIGIN__"
    private const val LOCATION_PLACEHOLDER = "__LOCATION__"
    private const val NAVIGATION_PAGE = "<!DOCTYPE html><script>location.replace(__LOCATION__);</script>"
    private const val FIRST_REDIRECTION_STATUS = 300
    private const val LAST_REDIRECTION_STATUS = 399
    private val UNSUPPORTED_STATUS_CODES = FIRST_REDIRECTION_STATUS..LAST_REDIRECTION_STATUS
    private val SCRIPT_SCHEMES = setOf("javascript", "data", "vbscript")

    /**
     * Not forwarded: OkHttp handles encoding itself, cookies are read from the web view per hop, and validators
     * could produce a 304, which a [WebResourceResponse] cannot carry.
     */
    private val EXCLUDED_REQUEST_HEADERS = setOf("accept-encoding", "cookie", "if-none-match", "if-modified-since")

    private val redirectNavigations: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    private val PAGE_SCRIPT = """
        (function () {
          var headers = __HEADERS__;
          var origin = __ORIGIN__;
          function needsHeaders(method, url) {
            if (String(method || 'GET').toUpperCase() === 'GET') { return false; }
            try { return new URL(url, location.href).origin === origin; } catch (e) { return false; }
          }
          var nativeFetch = window.fetch;
          window.fetch = function (input, init) {
            var request = input instanceof Request ? input : undefined;
            var url = request ? request.url : String(input);
            var method = (init && init.method) || (request && request.method);
            if (!needsHeaders(method, url)) { return nativeFetch.apply(this, arguments); }
            var base = (init && init.headers) || (request && request.headers);
            var merged = new Headers(base);
            Object.keys(headers).forEach(function (name) { merged.set(name, headers[name]); });
            var redirect = (init && init.redirect) || (request && request.redirect);
            return nativeFetch.call(this, input, Object.assign({}, init, {
              headers: merged,
              redirect: redirect === 'manual' ? 'manual' : 'error'
            }));
          };
          var nativeOpen = XMLHttpRequest.prototype.open;
          var nativeSend = XMLHttpRequest.prototype.send;
          XMLHttpRequest.prototype.open = function (method, url) {
            this.__ncNeedsHeaders = needsHeaders(method, url);
            return nativeOpen.apply(this, arguments);
          };
          XMLHttpRequest.prototype.send = function () {
            if (this.__ncNeedsHeaders) {
              var xhr = this;
              Object.keys(headers).forEach(function (name) { xhr.setRequestHeader(name, headers[name]); });
            }
            return nativeSend.apply(this, arguments);
          };
        })();
    """.trimIndent()

    /**
     * Each origin gets its own script that is only injected into frames of that origin, so a page never sees the
     * headers of another origin.
     */
    fun installPageScripts(webView: WebView) {
        val origins = CustomHeaders.origins
        if (origins.isEmpty()) {
            return
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            for (origin in origins) {
                val script = PAGE_SCRIPT
                    .replace(HEADERS_PLACEHOLDER, JSONObject(CustomHeaders.forOrigin(origin)).toString())
                    .replace(ORIGIN_PLACEHOLDER, JSONObject.quote(origin))
                WebViewCompat.addDocumentStartJavaScript(webView, script, setOf(origin))
            }
        } else {
            Log_OC.w(TAG, "document start script not supported, page requests will not carry custom headers")
        }
    }

    /**
     * Redirects are followed hop by hop: sub-resources only within the origin of the request, the main frame by
     * navigating to the target, so a body is never served under the URL of another origin.
     */
    fun intercept(context: Context, request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString().toHttpUrlOrNull() ?: return null
        val isGet = request.method.equals(METHOD_GET, ignoreCase = true)
        if (!isGet || CustomHeaders.forDestination(url.scheme, url.host, url.port).isEmpty()) {
            return null
        }

        val forwarded = request.requestHeaders.filterKeys { it.lowercase(Locale.ROOT) !in EXCLUDED_REQUEST_HEADERS }
        var hop = url
        repeat(MAX_REDIRECTS) {
            val response = try {
                execute(context, hop, forwarded)
            } catch (e: IOException) {
                Log_OC.e(TAG, "web view request via library client failed: ${e.javaClass.simpleName}")
                return null
            }
            storeCookies(response)
            if (response.code !in UNSUPPORTED_STATUS_CODES) {
                return response.toWebResourceResponse()
            }

            val location = response.header(HEADER_LOCATION)
            response.close()
            if (location == null) {
                return null
            }
            if (request.isForMainFrame) {
                val target = hop.resolve(location)?.toString() ?: location
                return target.takeUnless { isScriptScheme(it) }?.let { navigationTo(it) }
            }
            hop = hop.resolve(location)?.takeIf { isSameOrigin(it, url) } ?: return null
        }
        return null
    }

    /**
     * Navigations started by [navigationTo] replace a server redirect and must be treated like one by web view
     * clients that open other navigations outside the web view.
     */
    fun isRedirectNavigation(url: Uri): Boolean = redirectNavigations.remove(normalized(url.toString()))

    private fun normalized(url: String): String = url.toHttpUrlOrNull()?.toString() ?: url

    private fun execute(context: Context, url: HttpUrl, forwarded: Map<String, String>): Response {
        val outgoing = Request.Builder().url(url)
        forwarded.forEach { (name, value) -> outgoing.header(name, value) }
        CookieManager.getInstance().getCookie(url.toString())?.let { outgoing.header(HEADER_COOKIE, it) }
        return CustomHeadersHttpClient.withoutRedirects(context).newCall(outgoing.build()).execute()
    }

    private fun storeCookies(response: Response) {
        val cookieManager = CookieManager.getInstance()
        response.headers(HEADER_SET_COOKIE).forEach { cookieManager.setCookie(response.request.url.toString(), it) }
    }

    private fun isScriptScheme(location: String): Boolean {
        val scheme = location.substringBefore(':', missingDelimiterValue = "").trim().lowercase(Locale.ROOT)
        return scheme in SCRIPT_SCHEMES
    }

    private fun isSameOrigin(first: HttpUrl, second: HttpUrl): Boolean =
        first.scheme == second.scheme && first.host == second.host && first.port == second.port

    /**
     * A redirect cannot be returned to the web view, so the main frame navigates to the target itself. The target
     * then gets its own URL, origin and, if registered, its own interception; app schemes reach the URL handling of
     * the web view client.
     */
    private fun navigationTo(location: String): WebResourceResponse {
        redirectNavigations.add(normalized(location))
        val page = NAVIGATION_PAGE.replace(LOCATION_PLACEHOLDER, JSONObject.quote(location))
        return WebResourceResponse(HTML_MIME_TYPE, UTF_8, page.byteInputStream())
    }

    private fun Response.toWebResourceResponse(): WebResourceResponse {
        val contentType = body.contentType()
        val responseHeaders = headers.toMultimap().mapValues { (_, values) -> values.joinToString(HEADER_SEPARATOR) }
        return WebResourceResponse(
            contentType?.let { "${it.type}/${it.subtype}" },
            contentType?.charset()?.name(),
            code,
            message.ifEmpty { DEFAULT_REASON },
            responseHeaders - HEADER_SET_COOKIE.lowercase(Locale.ROOT),
            body.byteStream()
        )
    }
}
