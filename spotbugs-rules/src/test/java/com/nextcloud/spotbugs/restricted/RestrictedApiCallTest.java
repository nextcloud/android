/*
 * Nextcloud Android SpotBugs Plugin
 *
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2019 Chris Narkiewicz
 * Copyright (C) 2019 Nextcloud GmbH
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
package com.nextcloud.spotbugs.restricted;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;

import com.nextcloud.spotbugs.Utils;
import com.nextcloud.spotbugs.restricted.examples.BadAccountManagerUsed;
import com.nextcloud.spotbugs.restricted.examples.BadSharedPreferencesUse;
import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class RestrictedApiCallTest {

    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void testSharedPreferences() {
        Path path = Utils.get(BadSharedPreferencesUse.class);
        BugCollection bugCollection = spotbugs.performAnalysis(path);

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(RestrictedApiCall.ISSUE_TYPE).build();
        assertThat(bugCollection, containsExactly(6, bugTypeMatcher));
    }

    @Test
    public void testAccountManager() {
        Path path = Utils.get(BadAccountManagerUsed.class);
        BugCollection bugCollection = spotbugs.performAnalysis(path);

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(RestrictedApiCall.ISSUE_TYPE).build();
        assertThat(bugCollection, containsExactly(4, bugTypeMatcher));
    }

}
