/*
 * Copyright 2011-2013 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.explorer.sns;

import java.util.regex.Pattern;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.wizards.CompositeWizardPage;
import com.amazonaws.eclipse.core.ui.wizards.InputValidator;
import com.amazonaws.eclipse.core.ui.wizards.TextWizardPageInput;
import com.amazonaws.eclipse.core.ui.wizards.WizardPageInput;
import com.amazonaws.eclipse.explorer.ContentProviderRegistry;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SetTopicAttributesRequest;

/**
 * A wizard for creating an SNS topic.
 */
public class CreateTopicWizard extends Wizard {

    private final CompositeWizardPage page;

    /**
     * Constructor.
     */
    public CreateTopicWizard() {

        page = new CompositeWizardPage(
            "Create New SNS Topic",
            "Create New SNS Topic",
            AwsToolkitCore.getDefault()
                .getImageRegistry()
                .getDescriptor("aws-logo")
        );

        WizardPageInput topicName = new TextWizardPageInput(
            "Topic Name: ",
            null,       // no descriptive text.
            TopicNameValidator.INSTANCE,
            null        // no async validation possible for topic names.
        );

        WizardPageInput displayName = new TextWizardPageInput(
            "Display Name: ",
            "An optional display name for this topic.",
            DisplayNameValidator.INSTANCE,
            null        // no async validation here either.
        );

        page.addInput(TOPIC_NAME_INPUT, topicName);
        page.addInput(DISPLAY_NAME_INPUT, displayName);
    }

    @Override
    public void addPages() {
        super.addPage(page);
    }

    @Override
    public int getPageCount() {
        return 1;
    }

    @Override
    public boolean needsPreviousAndNextButtons() {
        return false;
    }

    @Override
    public boolean performFinish() {
        String topicName = (String) page.getInputValue(TOPIC_NAME_INPUT);
        String displayName = (String) page.getInputValue(DISPLAY_NAME_INPUT);

        AmazonSNS client = AwsToolkitCore.getClientFactory().getSNSClient();

        CreateTopicResult result =
            client.createTopic(new CreateTopicRequest(topicName));

        if (displayName != null && displayName.length() > 0) {
            try {

                client.setTopicAttributes(new SetTopicAttributesRequest()
                    .withTopicArn(result.getTopicArn())
                    .withAttributeName(DISPLAY_NAME_ATTRIBUTE)
                    .withAttributeValue(displayName)
                );

            } catch (AmazonClientException exception) {
                AwsToolkitCore.getDefault().logError(
                    "Error setting topic display name",
                    exception
                );

                MessageDialog dialog = new MessageDialog(
                    Display.getCurrent().getActiveShell(),
                    "Warning",
                    null,
                    ("The topic was successfully created, but the display "
                     + "name could not be set (" + exception.toString()
                     + ")"),
                    MessageDialog.WARNING,
                    new String[] { "OK" },
                    0
                );
                dialog.open();
            }
        }

        ContentProviderRegistry.refreshAllContentProviders();
        return true;
    }

    private static final String TOPIC_NAME_INPUT = "topicName";
    private static final String DISPLAY_NAME_INPUT = "displayName";

    private static final String DISPLAY_NAME_ATTRIBUTE = "DisplayName";

    /**
     * Synchronous validator for topic names.
     */
    private static class TopicNameValidator implements InputValidator {

        public static TopicNameValidator INSTANCE = new TopicNameValidator();

        /** {@inheritDoc} */
        @Override
        public IStatus validate(final Object value) {
            String topicName = (String) value;

            if (topicName == null || topicName.length() == 0) {
                return ValidationStatus.error("Please enter a topic name");
            }

            if (topicName.length() > MAX_TOPIC_NAME_LENGTH) {
                return ValidationStatus.error(String.format(
                    "Topic names may not exceed %d characters",
                    MAX_TOPIC_NAME_LENGTH
                ));
            }

            if (!TOPIC_NAME_PATTERN.matcher(topicName).matches()) {
                return ValidationStatus.error(
                    "Topic names may only contain letters, numbers, '-', "
                    + "or '_'"
                );
            }

            return ValidationStatus.ok();
        }

        /**
         * I'm stateless, use my singleton INSTANCE.
         */
        private TopicNameValidator() {
        }

        /**
         * Valid characters which can appear in a topic name, per
         * http://aws.amazon.com/sns/faqs/#10.
         */
        private static final Pattern TOPIC_NAME_PATTERN =
            Pattern.compile("[A-Za-z0-9-_]+");

        private static final int MAX_TOPIC_NAME_LENGTH = 256;
    }

    /**
     * Synchronous validator for display names.
     */
    private static class DisplayNameValidator implements InputValidator {

        public static DisplayNameValidator INSTANCE =
            new DisplayNameValidator();

        /** {@inheritDoc} */
        @Override
        public IStatus validate(final Object value) {
            String displayName = (String) value;

            if (displayName == null || displayName.length() == 0) {
                // Display name is optional.
                return ValidationStatus.ok();
            }

            if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
                return ValidationStatus.error(String.format(
                    "Display names may not exceed %d characters",
                    MAX_DISPLAY_NAME_LENGTH
                ));
            }

            return ValidationStatus.ok();
        }

        private static final int MAX_DISPLAY_NAME_LENGTH = 100;
    }
}
