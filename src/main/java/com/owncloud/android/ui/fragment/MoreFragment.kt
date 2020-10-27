package com.owncloud.android.ui.fragment

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.ExternalLinksProvider
import com.owncloud.android.lib.common.ExternalLink
import com.owncloud.android.lib.common.ExternalLinkType
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import com.owncloud.android.ui.activity.ExternalSiteWebView
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ThemeUtils
import kotlinx.android.synthetic.main.fragment_more.*
import org.parceler.Parcels
import kotlin.math.ceil
import kotlin.math.roundToInt

class MoreFragment : Fragment() {

    private val accountManager: UserAccountManager by lazy { (activity as FileDisplayActivity).userAccountManager }

    private val preferences: AppPreferences by lazy { (activity as FileDisplayActivity).appPreferences }

    private val clientFactory: ClientFactory by lazy { (activity as FileDisplayActivity).clientFactory }

    private val externalLinksProvider by lazy { ExternalLinksProvider(requireActivity().contentResolver) }

    private var isDevice = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_shared -> {
                    MainApp.showOnlyFilesOnDevice(false)
                    showFiles(SearchEvent("", SearchRemoteOperation.SearchType.SHARED_FILTER))
                }
                R.id.nav_on_device -> {
                    MainApp.showOnlyFilesOnDevice(true)
                    isDevice = true
                    showFiles(SearchEvent("", SearchRemoteOperation.SearchType.SHARED_FILTER))
                }
                else -> {
                    (activity as FileDisplayActivity).onNavigationItemClicked(menuItem)
                }
            }
            true
        }
        (activity as? FileDisplayActivity)?.setupToolbar()
        ThemeUtils.setColoredTitle(
            (activity as FileDisplayActivity?)?.supportActionBar,
            getString(R.string.more), context
        )
        ThemeUtils.colorProgressBar(drawerQuotaProgressBar, ThemeUtils.primaryColor(activity))
        getAndDisplayUserQuota()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            MainApp.showOnlyFilesOnDevice(isDevice)
            (activity as? FileDisplayActivity)?.setupToolbar()
            ThemeUtils.setColoredTitle(
                (activity as FileDisplayActivity?)?.supportActionBar,
                getString(R.string.more), context
            )
            updateQuotaLink()
        }
    }

    private fun showFiles(searchEvent: SearchEvent) {
        val bundle = Bundle()
        bundle.putParcelable(OCFileListFragment.SEARCH_EVENT, Parcels.wrap(searchEvent))
        val fragment = OCFileListFragment()
        fragment.arguments = bundle
        childFragmentManager.beginTransaction()
            .replace(R.id.container, fragment, FileDisplayActivity.TAG_LIST_OF_FILES)
            .commit()
        navView.visibility = View.GONE
    }

    private fun getAndDisplayUserQuota() {
        // set user space information
        val t = Thread(Runnable {
            val user: User = accountManager.user
            if (user.isAnonymous) {
                return@Runnable
            }
            val context = MainApp.getAppContext()
            val result = GetUserInfoRemoteOperation().execute(user.toPlatformAccount(), context)
            if (result.isSuccess && result.data != null) {
                val userInfo = result.data[0] as UserInfo
                val quota = userInfo.getQuota()
                if (quota != null) {
                    val used = quota.getUsed()
                    val total = quota.getTotal()
                    val relative = ceil(quota.getRelative()).toInt()
                    val quotaValue = quota.getQuota()
                    activity?.runOnUiThread {
                        if (quotaValue > 0 || quotaValue == GetUserInfoRemoteOperation.SPACE_UNLIMITED || quotaValue == GetUserInfoRemoteOperation.QUOTA_LIMIT_INFO_NOT_AVAILABLE
                        ) {
                            /*
                       * show quota in case
                       * it is available and calculated (> 0) or
                       * in case of legacy servers (==QUOTA_LIMIT_INFO_NOT_AVAILABLE)
                       */
                            setQuotaInformation(used, total, relative, quotaValue)
                        } else {
                            /*
                       * quotaValue < 0 means special cases like
                       * {@link RemoteGetUserQuotaOperation.SPACE_NOT_COMPUTED},
                       * {@link RemoteGetUserQuotaOperation.SPACE_UNKNOWN} or
                       * {@link RemoteGetUserQuotaOperation.SPACE_UNLIMITED}
                       * thus don't display any quota information.
                       */
                            showQuota(false)
                        }
                    }
                }
            }
        })
        t.start()
    }

    /**
     * configured the quota to be displayed.
     *
     * @param usedSpace  the used space
     * @param totalSpace the total space
     * @param relative   the percentage of space already used
     * @param quotaValue [GetUserInfoRemoteOperation.SPACE_UNLIMITED] or other to determinate state
     */
    private fun setQuotaInformation(usedSpace: Long, totalSpace: Long, relative: Int, quotaValue: Long) {
        if (GetUserInfoRemoteOperation.SPACE_UNLIMITED == quotaValue) {
            drawerQuotaPercentage.text = String.format(
                getString(R.string.drawer_quota_unlimited),
                DisplayUtils.bytesToHumanReadable(usedSpace)
            )
        } else {
            drawerQuotaPercentage.text = String.format(
                getString(R.string.drawer_quota),
                DisplayUtils.bytesToHumanReadable(usedSpace),
                DisplayUtils.bytesToHumanReadable(totalSpace)
            )
        }
        drawerQuotaProgressBar.progress = relative
        ThemeUtils.colorProgressBar(drawerQuotaProgressBar, DisplayUtils.getRelativeInfoColor(activity, relative))
        updateQuotaLink()
        showQuota(true)
    }

    private fun updateQuotaLink() {
        if (activity?.baseContext?.resources?.getBoolean(R.bool.show_external_links) == true) {
            val quotas: List<ExternalLink> = externalLinksProvider.getExternalLink(ExternalLinkType.QUOTA)
            val density = resources.displayMetrics.density
            val size = (24 * density).roundToInt()
            if (quotas.isNotEmpty()) {
                val firstQuota = quotas[0]
                drawerQuotaLink.text = firstQuota.name
                drawerQuotaLink.isClickable = true
                drawerQuotaLink.visibility = View.VISIBLE
                drawerQuotaLink.setOnClickListener(View.OnClickListener { v: View? ->
                    val externalWebViewIntent = Intent(
                        requireActivity().applicationContext,
                        ExternalSiteWebView::class.java
                    )
                    externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, firstQuota.name)
                    externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, firstQuota.url)
                    externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, true)
                    externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_MENU_ITEM_ID, -1)
                    startActivity(externalWebViewIntent)
                })
                val target: SimpleTarget<*> = object : SimpleTarget<Drawable?>() {

                    override fun onResourceReady(resource: Drawable?, glideAnimation: GlideAnimation<in Drawable?>?) {
                        val test = resource?.current
                        test?.setBounds(0, 0, size, size)
                        drawerQuotaLink.setCompoundDrawablesWithIntrinsicBounds(test, null, null, null)
                    }

                    override fun onLoadFailed(e: Exception, errorDrawable: Drawable) {
                        super.onLoadFailed(e, errorDrawable)
                        val test = errorDrawable.current
                        test.setBounds(0, 0, size, size)
                        drawerQuotaLink.setCompoundDrawablesWithIntrinsicBounds(test, null, null, null)
                    }
                }
                DisplayUtils.downloadIcon(
                    accountManager,
                    clientFactory,
                    activity,
                    firstQuota.iconUrl,
                    target,
                    R.drawable.ic_link,
                    size,
                    size
                )
            } else {
                drawerQuotaLink.visibility = View.GONE
            }
        } else {
            drawerQuotaLink.visibility = View.GONE
        }
    }

    private fun showQuota(showQuota: Boolean) {
        if (showQuota) {
            drawerQuota.visibility = View.VISIBLE
        } else {
            drawerQuota.visibility = View.GONE
        }
    }
}
