/*
 *   ownCloud Android client application
 *
 *   Copyright (C) 2016 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.preview;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.StringUtils;
import com.owncloud.android.utils.ThemeUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.LinkResolver;
import io.noties.markwon.LinkResolverDef;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.core.spans.LinkSpan;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListDrawable;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.syntax.Prism4jTheme;
import io.noties.markwon.syntax.Prism4jThemeDefault;
import io.noties.markwon.syntax.SyntaxHighlightPlugin;
import io.noties.prism4j.Prism4j;
import io.noties.prism4j.annotations.PrismBundle;

@PrismBundle(
    include = {
        "c", "clike", "clojure", "cpp", "csharp", "css", "dart", "git", "go", "groovy", "java", "javascript", "json",
        "kotlin", "latex", "makefile", "markdown", "markup", "python", "scala", "sql", "swift", "yaml"
    },
    grammarLocatorClassName = ".MarkwonGrammarLocator"
)
public abstract class PreviewTextFragment extends FileFragment implements SearchView.OnQueryTextListener, Injectable {
    private static final String TAG = PreviewTextFragment.class.getSimpleName();


    protected SearchView mSearchView;
    protected String mSearchQuery = "";
    protected boolean mSearchOpen;
    protected TextView mTextPreview;
    protected Handler mHandler;
    protected RelativeLayout mMultiView;
    protected String mOriginalText;

    private TextView mMultiListMessage;
    private TextView mMultiListHeadline;
    private ImageView mMultiListIcon;
    private ProgressBar mMultiListProgress;

    @Inject UserAccountManager accountManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log_OC.e(TAG, "onCreateView");

        View ret = inflater.inflate(R.layout.text_file_preview, container, false);
        mTextPreview = ret.findViewById(R.id.text_preview);

        mMultiView = ret.findViewById(R.id.multi_view);

        setupMultiView(ret);
        setMultiListLoadingMessage();

        return ret;
    }

    private void setupMultiView(View view) {
        mMultiListMessage = view.findViewById(R.id.empty_list_view_text);
        mMultiListHeadline = view.findViewById(R.id.empty_list_view_headline);
        mMultiListIcon = view.findViewById(R.id.empty_list_icon);
        mMultiListProgress = view.findViewById(R.id.empty_list_progress);
    }

    private void setMultiListLoadingMessage() {
        if (mMultiView != null) {
            mMultiListHeadline.setText(R.string.file_list_loading);
            mMultiListMessage.setText("");

            mMultiListIcon.setVisibility(View.GONE);
            mMultiListProgress.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log_OC.e(TAG, "onStart");

        loadAndShowTextPreview();
    }

    abstract void loadAndShowTextPreview();

    @Override
    public boolean onQueryTextSubmit(String query) {
        performSearch(query, 0);
        return true;
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        performSearch(newText, 500);
        return true;
    }


    private void performSearch(final String query, int delay) {
        mHandler.removeCallbacksAndMessages(null);

        if (mOriginalText != null) {
            if (getActivity() instanceof FileDisplayActivity) {
                FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) getActivity();
                fileDisplayActivity.setSearchQuery(query);
            }
            mHandler.postDelayed(() -> {
                if (query != null && !query.isEmpty()) {
                    if (getContext() != null && getContext().getResources() != null) {
                        String coloredText = StringUtils.searchAndColor(mOriginalText, query,
                            getContext().getResources().getColor(R.color.primary));
                        mTextPreview.setText(Html.fromHtml(coloredText.replace("\n", "<br \\>")));
                    }
                } else {
                    setText(mTextPreview, mOriginalText, getContext());
                }
            }, delay);
        }

        if (delay == 0 && mSearchView != null) {
            mSearchView.clearFocus();
        }
    }

    protected static Spanned getRenderedMarkdownText(Context context, String markdown) {
        Prism4j prism4j = new Prism4j(new MarkwonGrammarLocator());
        Prism4jTheme prism4jTheme = Prism4jThemeDefault.create();
        TaskListDrawable drawable = new TaskListDrawable(Color.GRAY, Color.GRAY, Color.WHITE);
        drawable.setColorFilter(ThemeUtils.primaryColor(context, true), PorterDuff.Mode.SRC_ATOP);

        final Markwon markwon = Markwon.builder(context)
            .usePlugin(new AbstractMarkwonPlugin() {
                @Override
                public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
                    TextPaint textPaint = new TextPaint();
                    textPaint.setColorFilter(new PorterDuffColorFilter(ThemeUtils.primaryColor(context), PorterDuff.Mode.SRC_ATOP));
                    builder.linkColor(ThemeUtils.primaryColor(context, true));
                }

                @Override
                public void configureConfiguration(@NonNull MarkwonConfiguration.Builder builder) {
                    builder.linkResolver((view, link) -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                            DisplayUtils.startIntentIfAppAvailable(intent, getActivity(), R.string.no_browser_available);
                        } catch (Throwable throwable) {
                            Toast.makeText(context, R.string.error_opening_link, Toast.LENGTH_SHORT).show();
                        }

                    });
                }
            })
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(drawable))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
            .build();

        return markwon.toMarkdown(markdown);
    }

    /**
     * Finishes the preview
     */
    protected void finish() {
        getActivity().runOnUiThread(() -> getActivity().onBackPressed());
    }

    private void setText(TextView textView, String text, OCFile file) {
        if (MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN.equals(file.getMimeType())
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN
            && context != null) {
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setText(getRenderedMarkdownText(getContext(), text));
        } else {
            textView.setText(text);
        }
    }
}
