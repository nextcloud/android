/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activities.adapter

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.GlideHelper
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

@Suppress("MagicNumber")
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

    @Suppress("NotifyDataSetChanged")
    fun setActivityItems(activityItems: List<Any>, client: NextcloudClient, clear: Boolean) {
        this.client = client
        if (clear) values.clear()

        var sTime = ""
        for (o in activityItems) {
            val activity = o as Activity
            val time = getHeaderDateString(context, activity.datetime.time).toString()
            if (!sTime.equals(time, ignoreCase = true)) {
                sTime = time
                values.add(sTime)
            }
            values.add(activity)
        }
        notifyDataSetChanged()
    }

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

    @Suppress("LongMethod")
    private fun bindActivityViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = values[position] as Activity

        holder.binding.datetime.apply {
            visibility = View.VISIBLE
            text = DateFormat.format("HH:mm", activity.datetime.time)
        }

        when {
            activity.richSubjectElement.richSubject.isNotEmpty() -> holder.binding.subject.apply {
                visibility = View.VISIBLE
                movementMethod = LinkMovementMethod.getInstance()
                setText(addClickablePart(activity.richSubjectElement), TextView.BufferType.SPANNABLE)
            }

            activity.subject.isNotEmpty() -> holder.binding.subject.apply {
                visibility = View.VISIBLE
                text = activity.subject
            }

            else -> holder.binding.subject.visibility = View.GONE
        }

        holder.binding.message.apply {
            text = activity.message
            visibility = if (activity.message.isNotEmpty()) View.VISIBLE else View.GONE
        }

        if (activity.icon.isNotEmpty()) {
            loadImageAsync(activity.icon, holder.binding.icon, R.drawable.ic_activity)
        }

        val isNightMode =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val isFileCreatedOrDeleted = activity.type.equals("file_created", ignoreCase = true) ||
            activity.type.equals("file_deleted", ignoreCase = true)

        if (!isFileCreatedOrDeleted) {
            holder.binding.icon.setColorFilter(
                if (isNightMode) Color.WHITE else Color.BLACK,
                PorterDuff.Mode.SRC_IN
            )
        }

        val richObjectList = activity.richSubjectElement.richObjectList
        if (richObjectList.isNotEmpty()) {
            holder.binding.list.apply {
                visibility = View.VISIBLE
                removeAllViews()
                post {
                    val totalColumnCount = measuredWidth / (px + 20)
                    try {
                        columnCount = totalColumnCount
                    } catch (e: IllegalArgumentException) {
                        Log_OC.e(TAG, "error setting column count to $totalColumnCount")
                    }
                }
                activity.previews
                    .filter {
                        !isDetailView || MimeTypeUtil.isImageOrVideo(it.mimeType) ||
                            MimeTypeUtil.isVideo(it.mimeType)
                    }
                    .forEach { addView(createThumbnail(it, richObjectList)) }
            }
        } else {
            holder.binding.list.apply {
                removeAllViews()
                visibility = View.GONE
            }
        }
    }

    private fun loadImageAsync(url: String, imageView: ImageView, @DrawableRes placeholder: Int) {
        context.lifecycleScope.launch {
            runCatching {
                val client = withContext(Dispatchers.IO) {
                    OwnCloudClientManagerFactory.getDefaultSingleton()
                        .getNextcloudClientFor(currentAccountProvider.user.toOwnCloudAccount(), context)
                }
                GlideHelper.loadIntoImageView(context, client, url, imageView, placeholder, false)
            }.onFailure {
                Log_OC.e(TAG, "Exception loading image: $it")
            }
        }
    }

    private fun createThumbnail(previewObject: PreviewObject, richObjectList: List<RichObject>): ImageView {
        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(px, px).apply { setMargins(10, 10, 10, 10) }
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
                    MimeTypeUtil.getFileTypeIcon(
                        previewObject.mimeType,
                        "",
                        context,
                        viewThemeUtils
                    )
                )
        }

        return imageView
    }

    private fun addClickablePart(richElement: RichElement): SpannableStringBuilder {
        var text = richElement.richSubject
        val ssb = SpannableStringBuilder(text)

        var idx1 = text.indexOf('{')
        while (idx1 != -1) {
            var idx2 = text.indexOf('}', idx1) + 1
            val richObject = richElement.richObjectList.firstOrNull {
                it.tag.equals(text.substring(idx1 + 1, idx2 - 1), ignoreCase = true)
            }

            if (richObject != null) {
                val name = richObject.name.orEmpty()
                ssb.replace(idx1, idx2, name)
                text = ssb.toString()
                idx2 = idx1 + name.length

                ssb.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) = activityListInterface.onActivityClicked(richObject)
                        override fun updateDrawState(ds: TextPaint) {
                            ds.isUnderlineText = false
                        }
                    },
                    idx1,
                    idx2,
                    0
                )
                ssb.setSpan(StyleSpan(Typeface.BOLD), idx1, idx2, 0)
                ssb.setSpan(
                    ForegroundColorSpan(context.resources.getColor(R.color.text_color)),
                    idx1,
                    idx2,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            idx1 = text.indexOf('{', idx2)
        }

        return ssb
    }

    override fun getItemViewType(position: Int) = if (values[position] is Activity) ACTIVITY_TYPE else HEADER_TYPE

    override fun getItemCount() = values.size

    fun isEmpty() = values.isEmpty()

    private fun getThumbnailDimension(): Int {
        val dimension = MainApp.getAppContext().resources.getDimension(R.dimen.file_icon_size_grid)
        return (2.0.pow(floor(log(dimension.toDouble(), 2.0))) / 2).toInt()
    }

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
                DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEE, MMMM d"),
                modificationTimestamp
            )
        }

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

    protected class ActivityViewHolder(val binding: ActivityListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    protected class ActivityViewHeaderHolder(val binding: ActivityListItemHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        const val HEADER_TYPE = 100
        const val ACTIVITY_TYPE = 101
        private val TAG: String = ActivityListAdapter::class.java.simpleName
    }
}
