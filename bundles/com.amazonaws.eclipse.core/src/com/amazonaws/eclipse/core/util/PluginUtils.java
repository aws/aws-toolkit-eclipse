/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

/**
 * Utilities for invoking other plugin features.
 */
public class PluginUtils {

    /**
     * Get or create a new MessageConsole given the console name.
     */
    public static MessageConsole getOrCreateMessageConsole(String consoleName) {
        IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();

        // Search existing consoles
        IConsole[] consoles = consoleManager.getConsoles();
        if (consoles != null) {
            for (IConsole console : consoles) {
                if (consoleName.equals(console.getName())
                        && (console instanceof MessageConsole)) {
                    return (MessageConsole)console;
                }
            }
        }

        // If not found, create a new console
        MessageConsole newConsole = new MessageConsole(consoleName, null);
        ConsolePlugin.getDefault().getConsoleManager()
                .addConsoles(new IConsole[] { newConsole });
        return newConsole;
    }

    /**
     * Replace the placeholder variables in the original string with the real ones.
     * E.g. ${workspace_loc:/Project/file.txt} will be replaced to the real workspace location.
     *
     * @throws CoreException - If unable to resolve the value of one or more variables
     */
    public static String variablePluginReplace(String originalValue) throws CoreException {
        if (originalValue == null || originalValue.isEmpty()) {
            return originalValue;
        }
        return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(originalValue);
    }

    private static final String WORKSPACE_LOC = "workspace_loc";
    /**
     * Generate a String with workspace location variable, and the relative path argument.
     */
    public static String variablePluginGenerateWorkspacePath(String argument) {
        return VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression(WORKSPACE_LOC, argument);
    }

    public static String variablePluginGenerateWorkspacePath(IPath relativePath) {
        return variablePluginGenerateWorkspacePath(relativePath.toString());
    }
}
