package com.testHelpers;

import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.lang.reflect.Field;

import androidx.core.content.ContextCompat;
import androidx.test.espresso.matcher.BoundedMatcher;

public class TestHelper {
    public static Matcher<View> withTextColor(final int expectedId) {
        return new BoundedMatcher<View, TextView>(TextView.class) {

            @Override
            protected boolean matchesSafely(TextView textView) {
                int colorId = ContextCompat.getColor(textView.getContext(), expectedId);
                return textView.getCurrentTextColor() == colorId;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with text color: ");
                description.appendValue(expectedId);
            }
        };
    }

    public static Matcher<View> withBackgroundColor(final int color) {
        return new BoundedMatcher<View, TextView>(TextView.class) {

            @Override
            protected boolean matchesSafely(TextView textView) {
                Drawable drawable = textView.getBackground();
                ColorDrawable colorDrawable = new ColorDrawable(ContextCompat.getColor(textView.getContext(), color));
                if (drawable instanceof ColorDrawable) {
                    ColorDrawable backgroundColor = (ColorDrawable) textView.getBackground();
                    return colorDrawable.getColor() == backgroundColor.getColor();
                } else if (drawable instanceof RippleDrawable) {
                    RippleDrawable rippleDrawable = (RippleDrawable) textView.getBackground();
                    Drawable.ConstantState state = rippleDrawable.getConstantState();
                    try {
                        Field colorField = state.getClass().getDeclaredField("mColor");
                        colorField.setAccessible(true);
                        ColorStateList colorStateList = (ColorStateList) colorField.get(state);
                        if (colorStateList != null) {
                            int rippleColor = colorStateList.getDefaultColor();
                            return colorDrawable.getColor() == rippleColor;
                        } else {
                            return false;
                        }
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("background color to be: ");
                description.appendValue(color);
            }
        };
    }
}
