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
package com.nextcloud.spotbugs.impl;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import javax.inject.Provider;
import java.util.Collection;
import java.util.Objects;

public class PrivateImplementationCall extends OpcodeStackDetector {

    final static String ISSUE_TYPE = "NEXTCLOUD_PRIVATE_IMPL_CALL";
    private final static String IMPLEMENTATION_SUFFIX = "Impl;";
    private final static String PROVIDER_SIGNATURE = Provider.class.getName();

    private final BugReporter bugReporter;

    public PrivateImplementationCall(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        switch (seen) {
            case Const.INVOKEVIRTUAL:
            case Const.INVOKESTATIC:
            case Const.INVOKESPECIAL:
                checkForPrivateImplCall();
                break;
            default:
                break;
        }
    }

    @Override
    public void visit(Method obj) {
        super.visit(obj);
        for(Type type : obj.getArgumentTypes()) {
            if(type.getSignature().endsWith(IMPLEMENTATION_SUFFIX)) {
                BugInstance bug = new BugInstance(this, ISSUE_TYPE, HIGH_PRIORITY)
                        .addClassAndMethod(this)
                        .addSourceLine(this, getPC());
                bugReporter.reportBug(bug);
            }
        }
    }

    private void checkForPrivateImplCall() {
        ClassDescriptor operand = getClassDescriptorOperand();
        String signature = operand.getSignature();

        // Check if we are dealing with Impl class
        if(!signature.endsWith(IMPLEMENTATION_SUFFIX)) {
            return;
        }

        ClassContext context = getClassContext();
        XClass callerClass = context.getXClass();

        // Check if we're calling self-calling this
        String callerSignature = context.getClassDescriptor().getSignature();
        String operandSignature = operand.getSignature();
        if (Objects.equals(callerSignature, operandSignature)) {
            return;
        }

        // Check if private constructor is called from dependency injection provider
        for( ClassDescriptor cd : callerClass.getInterfaceDescriptorList() ) {
            if(cd.getDottedClassName().equals(PROVIDER_SIGNATURE)) {
                return;
            }
        }

        // Check if the method is annotated with @Provides (Dagger)
        XMethod callerMethod = getXMethod();
        Collection<ClassDescriptor> annotations = callerMethod.getAnnotationDescriptors();
        for(ClassDescriptor annotation : annotations) {
            if (annotation.getDottedClassName().endsWith("Provides")) {
                return;
            }
        }

        BugInstance bug = new BugInstance(this, ISSUE_TYPE, HIGH_PRIORITY)
                .addClassAndMethod(this)
                .addSourceLine(this, getPC());
        bugReporter.reportBug(bug);
    }
}
