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
package com.amazonaws.eclipse.explorer.s3.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.KeyValueSetDataModel;
import com.amazonaws.eclipse.core.model.KeyValueSetDataModel.Pair;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.ui.TagsEditingDialog;
import com.amazonaws.eclipse.core.ui.TagsEditingDialog.TagsEditingDialogBuilder;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.eclipse.explorer.s3.S3Constants;
import com.amazonaws.eclipse.explorer.s3.S3ObjectSummaryTable;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.Tag;

public class EditObjectTagsAction extends AwsAction {

    private final S3ObjectSummaryTable table;

    public EditObjectTagsAction(S3ObjectSummaryTable s3ObjectSummaryTable) {
        super(AwsToolkitMetricType.EXPLORER_S3_EDIT_OBJECT_TAGS);
        this.table = s3ObjectSummaryTable;
        setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_WRENCH));
        setText("Edit Tags");
    }

    @Override
    public boolean isEnabled() {
        return table.getSelectedObjects().size() == 1;
    }

    @Override
    protected void doRun() {
        final AmazonS3 s3 = table.getS3Client();
        final S3ObjectSummary selectedObject = table.getSelectedObjects().iterator().next();
        List<Tag> tags = s3.getObjectTagging(new GetObjectTaggingRequest(
                selectedObject.getBucketName(),
                selectedObject.getKey()))
            .getTagSet();

        final KeyValueSetDataModel dataModel = convertToDataModel(tags);

        final TagsEditingDialog dialog = new TagsEditingDialogBuilder()
                .maxKeyLength(S3Constants.MAX_S3_OBJECT_TAG_KEY_LENGTH)
                .maxValueLength(S3Constants.MAX_S3_OBJECT_TAG_VALUE_LENGTH)
                .saveListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        saveTags(s3, selectedObject.getBucketName(), selectedObject.getKey(), dataModel);
                    }
                })
                .build(Display.getDefault().getActiveShell(), dataModel);

        if (Window.OK == dialog.open()) {
            try {
                saveTags(s3, selectedObject.getBucketName(), selectedObject.getKey(), dataModel);
                actionSucceeded();
            } catch (Exception e) {
                actionFailed();
                AwsToolkitCore.getDefault().reportException(e.getMessage(), e);
            } finally {
                actionFinished();
            }
        } else {
            actionCanceled();
            actionFinished();
        }
    }

    private void saveTags(AmazonS3 s3, String bucket, String key, KeyValueSetDataModel dataModel) {
        List<Tag> tags = convertFromDataModel(dataModel);
        s3.setObjectTagging(new SetObjectTaggingRequest(
                bucket,
                key,
                new ObjectTagging(tags)));
    }

    private KeyValueSetDataModel convertToDataModel(List<Tag> tags) {
        List<Pair> pairList = new ArrayList<>(tags.size());
        for (Tag tag : tags) {
            pairList.add(new Pair(tag.getKey(), tag.getValue()));
        }
        return new KeyValueSetDataModel(S3Constants.MAX_S3_OBJECT_TAGS, pairList);
    }

    private List<Tag> convertFromDataModel(KeyValueSetDataModel dataModel) {
        List<Tag> tags = new ArrayList<>(dataModel.getPairSet().size());
        for (Pair pair : dataModel.getPairSet()) {
            tags.add(new Tag(pair.getKey(), pair.getValue()));
        }
        return tags;
    }
}
