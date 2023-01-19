/*
 * Nextcloud Android client application
 *
 * @author Piotr Bator
 * Copyright (C) 2022 Piotr Bator
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class TimeUtils {

    private TimeUtils() {
        // utility class -> private constructor
    }

    public static DurationParts getDurationParts(long duration) {
        int days = (int) MILLISECONDS.toDays(duration);
        int hours = (int) MILLISECONDS.toHours(duration) - (days * 24);
        int minutes = (int) (MILLISECONDS.toMinutes(duration) - (MILLISECONDS.toHours(duration) * 60));
        return new DurationParts(days, hours, minutes);
    }

    public static class DurationParts {
        private int days;
        private int hours;
        private int minutes;

        public DurationParts(int days, int hours, int minutes) {
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
        }

        public int getDays() {
            return days;
        }

        public int getHours() {
            return hours;
        }

        public int getMinutes() {
            return minutes;
        }
    }

}
