package com.owncloud.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.features.FeatureItem;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class FeatureFragment extends Fragment {
    private FeatureItem item;

    static public FeatureFragment newInstance(FeatureItem item) {
        FeatureFragment f = new FeatureFragment();
        Bundle args = new Bundle();
        args.putParcelable("feature", item);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        item = getArguments() != null ? (FeatureItem) getArguments().getParcelable("feature") : null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.whats_new_element, container, false);
        int fontColor = getResources().getColor(R.color.login_text_color);

        ImageView whatsNewImage = view.findViewById(R.id.whatsNewImage);
        if (item.shouldShowImage()) {
            whatsNewImage.setImageDrawable(ThemeDrawableUtils.tintDrawable(item.getImage(), fontColor));
        }

        TextView whatsNewTitle = view.findViewById(R.id.whatsNewTitle);
        if (item.shouldShowTitleText()) {
            whatsNewTitle.setText(item.getTitleText());
            whatsNewTitle.setTextColor(fontColor);
            whatsNewTitle.setVisibility(View.VISIBLE);
        } else {
            whatsNewTitle.setVisibility(View.GONE);
        }

        LinearLayout linearLayout = view.findViewById(R.id.whatsNewTextLayout);
        if (item.shouldShowContentText()) {
            if (item.shouldShowBulletPointList()) {
                String[] texts = getText(item.getContentText()).toString().split("\n");

                for (String text : texts) {
                    TextView textView = generateTextView(text, getContext(),
                            item.shouldContentCentered(), fontColor, true);

                    linearLayout.addView(textView);
                }
            } else {
                TextView textView = generateTextView(getText(item.getContentText()).toString(),
                        getContext(), item.shouldContentCentered(), fontColor, false);

                linearLayout.addView(textView);
            }
        } else {
            linearLayout.setVisibility(View.GONE);
        }

        return view;
    }

    private TextView generateTextView(String text, Context context,
                                      boolean shouldContentCentered, int fontColor,
                                      boolean showBulletPoints) {
        int standardMargin = context.getResources().getDimensionPixelSize(R.dimen.standard_margin);
        int doubleMargin = context.getResources()
                .getDimensionPixelSize(R.dimen.standard_double_margin);
        int zeroMargin = context.getResources().getDimensionPixelSize(R.dimen.zero);

        TextView textView = new TextView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(doubleMargin, standardMargin, doubleMargin, zeroMargin);
        textView.setTextAppearance(context, R.style.NextcloudTextAppearanceMedium);
        textView.setLayoutParams(layoutParams);

        if (showBulletPoints) {
            BulletSpan bulletSpan = new BulletSpan(standardMargin, fontColor);
            SpannableString spannableString = new SpannableString(text);
            spannableString.setSpan(bulletSpan, 0, spannableString.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(spannableString);
        } else {
            textView.setText(text);
        }
        textView.setTextColor(fontColor);

        if (!shouldContentCentered) {
            textView.setGravity(Gravity.START);
        } else {
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
        }

        return textView;
    }
}
