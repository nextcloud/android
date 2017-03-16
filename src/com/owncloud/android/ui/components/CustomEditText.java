/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;

/**
 * Custom edit text to support fixed suffix or prefix
 */

public class CustomEditText extends android.support.v7.widget.AppCompatEditText {
    private Rect mFixedRect = new Rect();
    private String fixedText = "";
    private boolean isPrefixFixed;

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        String serverInputType = getResources().getString(R.string.server_input_type);

        if (serverInputType.equals(AuthenticatorActivity.DIRECTORY_SERVER_INPUT_TYPE)) {
            isPrefixFixed = true;
            fixedText = getResources().getString(R.string.server_url) + "/";
        } else if (serverInputType.equals(AuthenticatorActivity.SUBDOMAIN_SERVER_INPUT_TYPE)) {
            isPrefixFixed = false;
            fixedText = "." + getResources().getString(R.string.server_url);
        }

    }

    public String getFullServerUrl() {
        if (TextUtils.isEmpty(fixedText)) {
            return getText().toString();
        } else if (isPrefixFixed) {
            return (getResources().getString(R.string.server_url) + "/" + getText().toString());
        } else {
            return (getText().toString() + "." + getResources().getString(R.string.server_url));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!TextUtils.isEmpty(fixedText)) {
            getPaint().getTextBounds(fixedText, 0, fixedText.length(), mFixedRect);

            if (isPrefixFixed) {
                mFixedRect.right += getPaint().measureText(fixedText);
            } else {
                mFixedRect.right += getPaint().measureText(fixedText);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!TextUtils.isEmpty(fixedText)) {
            if (isPrefixFixed) {
                canvas.drawText(fixedText, super.getCompoundPaddingLeft() + getPaint().measureText(fixedText),
                        getBaseline(), getPaint());
            } else {
                canvas.drawText(fixedText, super.getCompoundPaddingLeft() + getPaint().measureText(getText().toString()),
                        getBaseline(), getPaint());
            }
        }
    }
}
