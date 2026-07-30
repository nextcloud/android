/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activities.adapter

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.GlideHelper
import com.nextcloud.utils.text.RichSubjectFormatter
import com.nextcloud.utils.text.RichSubjectParam
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityListItemBinding
import com.owncloud.android.databinding.ActivityListItemHeaderBinding
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.activities.model.Activity
import com.owncloud.android.lib.resources.activities.model.RichElement
import com.owncloud.android.lib.resources.activities.model.RichObject
import com.owncloud.android.lib.resources.activities.models.PreviewObject
import com.owncloud.android.ui.activities.StickyHeaderAdapter
import com.owncloud.android.ui.interfaces.ActivityListInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log
import kotlin.math.pow

@Suppress("MagicNumber", "TooManyFunctions")
open class ActivityListAdapter(
    protected val context: FragmentActivity,
    private val currentAccountProvider: CurrentAccountProvider,
    private val activityListInterface: ActivityListInterface,
    private val isDetailView: Boolean,
    private val viewThemeUtils: ViewThemeUtils
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    StickyHeaderAdapter {

    protected var client: NextcloudClient? = null
    val values: MutableList<Any> = mutableListOf()
    private val px = getThumbnailDimension()
    private var cachedNextcloudClient: NextcloudClient? = null
    private val richSubjectFormatter by lazy { RichSubjectFormatter(context, currentAccountProvider) }

    // region Public Methods
    @Suppress("NotifyDataSetChanged")
    fun setActivityItems(activityItems: List<Any>, client: NextcloudClient, clear: Boolean) {
        this.client = client
        if (clear) values.clear()
        appendGroupedByHeader(activityItems) { (it as Activity).datetime.time }
        notifyDataSetChanged()
    }

    fun isEmpty() = values.isEmpty()

    fun getHeaderDateString(context: Context, modificationTimestamp: Long): CharSequence =
        if ((System.currentTimeMillis() - modificationTimestamp) < DateUtils.WEEK_IN_MILLIS) {
            DisplayUtils.getRelativeDateTimeString(
                context,
                modificationTimestamp,
                DateUtils.DAY_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0
            )
        } else {
            DateFormat.format(
                DateFormat.getBestDateTimePattern(Locale.getDefault(), HEADER_DATE_SKELETON),
                modificationTimestamp
            )
        }
    // endregion

    // region Overridden Methods
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == ACTIVITY_TYPE) {
            ActivityViewHolder(ActivityListItemBinding.inflate(inflater, parent, false))
        } else {
            ActivityViewHeaderHolder(ActivityListItemHeaderBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ActivityViewHolder -> bindActivityViewHolder(holder, position)
            is ActivityViewHeaderHolder -> holder.binding.header.text = values[position] as String
        }
    }

    override fun getItemViewType(position: Int) = if (values[position] is Activity) ACTIVITY_TYPE else HEADER_TYPE

    override fun getItemCount() = values.size

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        var pos = itemPosition
        while (pos >= 0 && !isHeader(pos)) pos--
        return pos
    }

    override fun getHeaderLayout(headerPosition: Int) = R.layout.activity_list_item_header

    override fun bindHeaderData(header: View?, headerPosition: Int) {
        header?.findViewById<TextView>(R.id.header)?.text = values[headerPosition] as String
    }

    override fun isHeader(itemPosition: Int) =
        itemPosition in values.indices && getItemViewType(itemPosition) == HEADER_TYPE
    // endregion

    // region Private Methods
    private fun appendGroupedByHeader(items: List<Any>, timestampOf: (Any) -> Long?) {
        var currentHeader: String? = null
        for (item in items) {
            val header = getHeaderDateString(context, timestampOf(item) ?: continue).toString()
            if (!header.equals(currentHeader, ignoreCase = true)) {
                currentHeader = header
                values.add(header)
            }
            values.add(item)
        }
    }

    private fun bindActivityViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = values[position] as Activity
        holder.bindDateTime(activity)
        holder.bindSubject(activity)
        holder.bindMessage(activity)
        holder.bindIcon(activity)
        holder.bindPreviews(activity)
    }

    private fun ActivityViewHolder.bindDateTime(activity: Activity) {
        binding.datetime.apply {
            visibility = View.VISIBLE
            text = DateFormat.format(TIME_PATTERN, activity.datetime.time)
        }
    }

    private fun ActivityViewHolder.bindSubject(activity: Activity) {
        when {
            activity.richSubjectElement.richSubject.isNotEmpty() ->
                binding.subject.text = addClickablePart(activity.richSubjectElement)

            activity.subject.isNotEmpty() -> binding.subject.apply {
                visibility = View.VISIBLE
                text = activity.subject
            }

            else -> binding.subject.visibility = View.GONE
        }
    }

    private fun ActivityViewHolder.bindMessage(activity: Activity) {
        binding.message.apply {
            text = activity.message
            visibility = if (activity.message.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun ActivityViewHolder.bindIcon(activity: Activity) {
        if (activity.icon.isNotEmpty()) {
            GlideHelper.loadTintableIconIntoImageView(
                context,
                client,
                activity.icon,
                binding.icon,
                R.drawable.ic_activity,
                context.resources.getDimensionPixelSize(R.dimen.activity_icon_width)
            )
        }

        if (activity.icon.endsWith(COLORED_ICON_SUFFIX, ignoreCase = true)) {
            binding.icon.imageTintList = null
        } else {
            viewThemeUtils.platform.colorImageView(binding.icon, ColorRole.ON_SURFACE_VARIANT)
        }
    }

    private fun ActivityViewHolder.bindPreviews(activity: Activity) {
        val richObjectList = activity.richSubjectElement.richObjectList

        if (richObjectList.isEmpty()) {
            binding.list.apply {
                removeAllViews()
                visibility = View.GONE
            }
            return
        }

        binding.list.apply {
            visibility = View.VISIBLE
            removeAllViews()
            post { adjustColumnCount() }
            activity.previews
                .filter { shouldShowPreview(it) }
                .forEach { addView(createThumbnail(it, richObjectList)) }
        }
    }

    private fun shouldShowPreview(preview: PreviewObject): Boolean =
        !isDetailView || MimeTypeUtil.isImageOrVideo(preview.mimeType) || MimeTypeUtil.isVideo(preview.mimeType)

    private fun GridLayout.adjustColumnCount() {
        val columns = measuredWidth / (px + PREVIEW_COLUMN_SPACING)
        try {
            columnCount = columns
        } catch (_: IllegalArgumentException) {
            Log_OC.e(TAG, "error setting column count to $columns")
        }
    }

    private suspend fun nextcloudClient(): NextcloudClient = cachedNextcloudClient ?: withContext(Dispatchers.IO) {
        OwnCloudClientManagerFactory.getDefaultSingleton()
            .getNextcloudClientFor(currentAccountProvider.user.toOwnCloudAccount(), context)
    }.also { cachedNextcloudClient = it }

    private fun loadImageAsync(url: String, imageView: ImageView, @DrawableRes placeholder: Int) {
        context.lifecycleScope.launch {
            runCatching {
                GlideHelper.loadIntoImageView(context, nextcloudClient(), url, imageView, placeholder, false)
            }.onFailure {
                Log_OC.e(TAG, "Exception loading image: $it")
            }
        }
    }

    private fun createThumbnail(previewObject: PreviewObject, richObjectList: List<RichObject>): ImageView {
        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(px, px).apply {
                setMargins(PREVIEW_CELL_MARGIN, PREVIEW_CELL_MARGIN, PREVIEW_CELL_MARGIN, PREVIEW_CELL_MARGIN)
            }
        }

        richObjectList.firstOrNull { it.id?.toIntOrNull() == previewObject.fileId }?.let { richObject ->
            imageView.setOnClickListener { activityListInterface.onActivityClicked(richObject) }
        }

        when {
            MimeTypeUtil.isImageOrVideo(previewObject.mimeType) -> {
                val placeholder =
                    if (MimeTypeUtil.isImage(previewObject.mimeType)) R.drawable.file_image else R.drawable.file_movie
                previewObject.source?.let { loadImageAsync(it, imageView, placeholder) }
            }

            MimeTypeUtil.isFolder(previewObject.mimeType) ->
                imageView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(context, viewThemeUtils))

            else ->
                imageView.setImageDrawable(
                    MimeTypeUtil.getFileTypeIcon(previewObject.mimeType, "", context, viewThemeUtils)
                )
        }

        return imageView
    }

    private fun addClickablePart(richElement: RichElement): SpannableStringBuilder =
        richSubjectFormatter.format(richElement.richSubject) { tag ->
            richElement.richObjectList
                .firstOrNull { it.tag.equals(tag, ignoreCase = true) }
                ?.toRichSubjectParam()
        }

    private fun RichObject.toRichSubjectParam(): RichSubjectParam {
        val richObject = this
        return RichSubjectParam(type, id, name) { activityListInterface.onActivityClicked(richObject) }
    }

    private fun getThumbnailDimension(): Int {
        val dimension = MainApp.getAppContext().resources.getDimension(R.dimen.file_icon_size_grid)
        return (2.0.pow(floor(log(dimension.toDouble(), 2.0))) / 2).toInt()
    }
    // endregion

    protected class ActivityViewHolder(val binding: ActivityListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    protected class ActivityViewHeaderHolder(val binding: ActivityListItemHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        const val HEADER_TYPE = 100
        const val ACTIVITY_TYPE = 101
        private const val COLORED_ICON_SUFFIX = "-color.svg"
        private const val TIME_PATTERN = "HH:mm"
        private const val HEADER_DATE_SKELETON = "EEEE, MMMM d"
        private const val PREVIEW_COLUMN_SPACING = 20
        private const val PREVIEW_CELL_MARGIN = 10
        private val TAG: String = ActivityListAdapter::class.java.simpleName
    }
}
