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
package com.amazonaws.eclipse.lambda.launching;

import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.swt.graphics.Color;

import com.amazonaws.eclipse.lambda.ui.LambdaPluginColors;

/**
 * Color for SAM Local terminal output. The default color is red as SAM Local's output
 * goes to stderr even they are not actually error messages. We override it to be black.
 */
public class SamLocalConsoleColorProvider extends ConsoleColorProvider {

    @Override
    public Color getColor(String streamIdentifer) {
        // Make the output black.
        return LambdaPluginColors.BLACK;
    }
}
