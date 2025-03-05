/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017-2018 Mario Danic <mario@lovelyhq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.utils.text;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import third_parties.fresco.BetterImageSpan;

public class Spans {

    public static class MentionChipSpan extends BetterImageSpan {
        public String id;
        public CharSequence label;

        public MentionChipSpan(@NonNull Drawable drawable, int verticalAlignment, String id, CharSequence label) {
            super(drawable, verticalAlignment);
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return this.id;
        }

        public CharSequence getLabel() {
            return this.label;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setLabel(CharSequence label) {
            this.label = label;
        }

        public boolean equals(final Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof MentionChipSpan)) {
                return false;
            }
            final MentionChipSpan other = (MentionChipSpan) o;
            if (!other.canEqual((Object) this)) {
                return false;
            }
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
                return false;
            }
            final Object this$label = this.getLabel();
            final Object other$label = other.getLabel();

            return this$label == null ? other$label == null : this$label.equals(other$label);
        }

        protected boolean canEqual(final Object other) {
            return other instanceof MentionChipSpan;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            final Object $label = this.getLabel();
            return result * PRIME + ($label == null ? 43 : $label.hashCode());
        }

        public String toString() {
            return "Spans.MentionChipSpan(id=" + this.getId() + ", label=" + this.getLabel() + ")";
        }
    }
}
