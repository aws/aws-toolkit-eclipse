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
package com.amazonaws.eclipse.explorer.lambda;

import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.explorer.ExplorerNodeLabelProvider;
import com.amazonaws.eclipse.explorer.lambda.LambdaContentProvider.LambdaRootElement;
import com.amazonaws.eclipse.lambda.LambdaPlugin;

public class LambdaLabelProvider extends ExplorerNodeLabelProvider {
    @Override
    public String getText(Object element) {
        if (element instanceof LambdaRootElement) return "AWS Lambda";

        return getExplorerNodeText(element);
    }

    @Override
    public Image getDefaultImage(Object element) {
        if (element instanceof LambdaRootElement) {
            return LambdaPlugin.getDefault().getImageRegistry().get(LambdaPlugin.IMAGE_LAMBDA);
        } else {
            return LambdaPlugin.getDefault().getImageRegistry().get(LambdaPlugin.IMAGE_FUNCTION);
        }
    }
}
