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

import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import java.util.*;

public class RestrictedApiCall extends OpcodeStackDetector {

    final static String ISSUE_TYPE = "NEXTCLOUD_RESTRICTED_API_CALL";
    private static final Set<String> RESTRICTED_API;

    static {
        RESTRICTED_API = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(
                "android.accounts.AccountManager",
                "android.content.SharedPreferences",
                "android.content.SharedPreferences$Editor"
        )));
    }

    private final BugReporter bugReporter;

    public RestrictedApiCall(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        switch (seen) {
            case Const.INVOKEINTERFACE:
            case Const.INVOKEVIRTUAL:
            case Const.INVOKESTATIC:
                invoke();
            default:
                break;
        }
    }

    private void invoke() {
        ClassDescriptor operand = getClassDescriptorOperand();
        boolean forbidden = RESTRICTED_API.contains(operand.getDottedClassName());

        // Check if we're calling self-calling this
        ClassContext context = getClassContext();
        String callerSignature = context.getClassDescriptor().getSignature();
        String operandSignature = operand.getSignature();
        if (Objects.equals(callerSignature, operandSignature)) {
            return;
        }

        if (forbidden) {
            BugInstance bug = new BugInstance(this, ISSUE_TYPE, HIGH_PRIORITY)
                    .addClassAndMethod(this)
                    .addSourceLine(this, getPC());
            bugReporter.reportBug(bug);
        }
    }
}
