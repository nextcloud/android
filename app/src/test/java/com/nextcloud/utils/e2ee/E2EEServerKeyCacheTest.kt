/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import com.nextcloud.client.core.Clock
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class E2EEServerKeyCacheTest {
    private val clock: Clock = mock()
    private val cache = E2EEServerKeyCache(clock)
    private val success = RemoteOperationResult<String>(ResultCode.OK).apply { resultData = "server-key" }

    @Test
    fun repeatedFolderEntryReusesServerKey() {
        var requests = 0
        repeat(REPEATED_ENTRIES) {
            assertSame(
                success,
                cache.get(SERVER, CERTIFICATE) {
                    requests++
                    success
                }
            )
        }
        assertEquals(1, requests)
    }

    @Test
    fun expirationFetchesServerKeyAgain() {
        var requests = 0
        cache.get(SERVER, CERTIFICATE) {
            requests++
            success
        }
        whenever(clock.millisSinceBoot).thenReturn(TimeUnit.MINUTES.toMillis(10))
        cache.get(SERVER, CERTIFICATE) {
            requests++
            success
        }
        assertEquals(2, requests)
    }

    @Test
    fun changedCertificateFetchesServerKeyAgain() {
        var requests = 0
        cache.get(SERVER, CERTIFICATE) {
            requests++
            success
        }
        cache.get(SERVER, "renewed-certificate") {
            requests++
            success
        }
        assertEquals(2, requests)
    }

    @Test
    fun differentServersDoNotShareCachedKey() {
        var requests = 0
        cache.get(SERVER, CERTIFICATE) {
            requests++
            success
        }
        cache.get("https://cloud.example:8443", CERTIFICATE) {
            requests++
            success
        }
        cache.get("https://other.example", CERTIFICATE) {
            requests++
            success
        }
        assertEquals(3, requests)
    }

    @Test
    fun failuresAndEmptyResponsesAreNotCached() {
        val failure = RemoteOperationResult<String>(ResultCode.FORBIDDEN)
        val empty = RemoteOperationResult<String>(ResultCode.OK).apply { resultData = "" }
        assertSame(failure, cache.get(SERVER, CERTIFICATE) { failure })
        assertSame(empty, cache.get(SERVER, CERTIFICATE) { empty })
        assertSame(success, cache.get(SERVER, CERTIFICATE) { success })
    }

    @Test
    fun concurrentFolderEntriesShareOneRequest() {
        val executor = Executors.newFixedThreadPool(REPEATED_ENTRIES)
        val started = CountDownLatch(1)
        val response = CountDownLatch(1)
        val requests = AtomicInteger()
        try {
            val results = List(REPEATED_ENTRIES) {
                executor.submit<RemoteOperationResult<String>> {
                    cache.get(SERVER, CERTIFICATE) {
                        requests.incrementAndGet()
                        started.countDown()
                        check(response.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                        success
                    }
                }
            }
            assertTrue(started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            assertEquals(1, requests.get())
            response.countDown()
            results.forEach { assertSame(success, it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) }
            assertEquals(1, requests.get())
        } finally {
            response.countDown()
            executor.shutdownNow()
        }
    }

    companion object {
        private const val SERVER = "https://cloud.example"
        private const val CERTIFICATE = "certificate"
        private const val REPEATED_ENTRIES = 10
        private const val TIMEOUT_SECONDS = 5L
    }
}
