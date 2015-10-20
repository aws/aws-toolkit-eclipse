/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.lambda.project.listener;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

/**
 * An acceptor that invokes the given visitor by traversing the incoming
 * IJavaElementDelta and all its affected children
 */
class JavaElementDeltaAcceptor {

    public static void accept(IJavaElementDelta delta, Visitor visitor) {
        if (visitor.visit(delta.getElement())) {
            accept(delta.getAffectedChildren(), visitor);
        }
    }

    private static void accept(IJavaElementDelta[] deltas, Visitor visitor) {
        for (IJavaElementDelta delta : deltas) {
            accept(delta, visitor);
        }
    }

    public static abstract class Visitor {

        private boolean visit(IJavaElement element) {
            switch (element.getElementType()) {
            case IJavaElement.JAVA_MODEL: {
                return visit((IJavaModel) element);
            }
            case IJavaElement.JAVA_PROJECT: {
                return visit((IJavaProject) element);
            }
            case IJavaElement.PACKAGE_FRAGMENT: {
                return visit((IPackageFragment) element);
            }
            case IJavaElement.PACKAGE_FRAGMENT_ROOT: {
                return visit((IPackageFragmentRoot) element);
            }
            case IJavaElement.COMPILATION_UNIT: {
                return visit((ICompilationUnit) element);
            }
            case IJavaElement.CLASS_FILE: {
                return visit((IClassFile) element);
            }
            default:
                return true;
            }
        }

        protected boolean visit(IJavaModel model) {
            return true;
        }

        protected boolean visit(IJavaProject project) {
            return true;
        }

        protected boolean visit(IPackageFragment fragment) {
            return true;
        }

        protected boolean visit(IPackageFragmentRoot fragmentRoot) {
            return true;
        }

        protected boolean visit(ICompilationUnit unit) {
            return true;
        }

        protected boolean visit(IClassFile clazz) {
            return true;
        }
    }
}
