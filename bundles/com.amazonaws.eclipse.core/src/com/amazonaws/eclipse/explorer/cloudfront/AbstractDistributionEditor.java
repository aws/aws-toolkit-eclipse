/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.cloudfront;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;

import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.explorer.AbstractAwsResourceEditorInput;
import com.amazonaws.services.cloudfront.AmazonCloudFront;

public abstract class AbstractDistributionEditor extends EditorPart implements IRefreshable {

    private static final String ACCESS_LOGGING_DOCUMENTATION_URL = "http://docs.amazonwebservices.com/AmazonCloudFront/latest/DeveloperGuide/AccessLogs.html";
    private static final String CNAME_DOCUMENTATION_URL          = "http://docs.amazonwebservices.com/AmazonCloudFront/latest/DeveloperGuide/CNAMEs.html";

    private AbstractAwsResourceEditorInput editorInput;

    protected Text domainNameText;
    protected Text distributionIdText;
    protected Text lastModifiedText;
    protected Text enabledText;
    protected Text statusText;
    protected Text commentText;

    protected Label defaultRootObjectLabel;

    protected Text originText;

    protected Text loggingEnabledText;
    protected Text loggingBucketText;
    protected Text loggingPrefixText;
    protected org.eclipse.swt.widgets.List cnamesList;
    private ScrolledForm form;


    protected boolean supportsDefaultRootObjects() {
        return true;
    }

    protected abstract void contributeActions(IToolBarManager iToolBarManager);

    protected abstract String getResourceTitle();


    @Override
    public void doSave(IProgressMonitor monitor) {}

    @Override
    public void doSaveAs() {}

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void setFocus() {}

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        setPartName(input.getName());

        this.editorInput = (AbstractAwsResourceEditorInput)input;
    }

    @Override
    public void createPartControl(Composite parent) {
        FormToolkit toolkit = new FormToolkit(Display.getDefault());
        form = toolkit.createScrolledForm(parent);
        form.setFont(JFaceResources.getHeaderFont());

        form.setText(getResourceTitle() + " " + editorInput.getName());
        toolkit.decorateFormHeading(form.getForm());
        form.setImage(getTitleImage());
        form.getBody().setLayout(new GridLayout());

        createDistributionSummaryComposite(toolkit, form.getBody());
        form.reflow(true);
        refreshData();

        contributeActions(form.getToolBarManager());
        form.getToolBarManager().add(new Separator());
        form.getToolBarManager().add(new RefreshAction());
        form.getToolBarManager().update(true);
    }

    protected void createDistributionSummaryComposite(FormToolkit toolkit, Composite parent) {
        GridDataFactory gdf = GridDataFactory.swtDefaults()
            .align(SWT.FILL, SWT.TOP)
            .grab(true, false);

        GridDataFactory sectionGDF = GridDataFactory.swtDefaults()
            .span(2, 1)
            .grab(true, false)
            .align(SWT.FILL, SWT.TOP)
            .indent(0, 10);

        Composite summaryComposite = toolkit.createComposite(parent);
        summaryComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        summaryComposite.setLayout(new GridLayout(2, false));

        toolkit.createLabel(summaryComposite, "Domain Name:");

        domainNameText = createText(summaryComposite);
        gdf.applyTo(domainNameText);

        toolkit.createLabel(summaryComposite, "Distribution ID:");
        distributionIdText = createText(summaryComposite);
        gdf.applyTo(distributionIdText);

        toolkit.createLabel(summaryComposite, "Origin:");
        originText = createText(summaryComposite);
        gdf.applyTo(originText);

        toolkit.createLabel(summaryComposite, "Enabled:");
        enabledText = createText(summaryComposite);
        gdf.applyTo(enabledText);

        toolkit.createLabel(summaryComposite, "Status:");
        statusText = createText(summaryComposite);
        gdf.applyTo(statusText);

        toolkit.createLabel(summaryComposite, "Last Modified:");
        lastModifiedText = createText(summaryComposite);
        gdf.applyTo(lastModifiedText);

        toolkit.createLabel(summaryComposite, "Comment:");
        commentText = createText(summaryComposite);
        gdf.applyTo(commentText);

        if (supportsDefaultRootObjects()) {
            toolkit.createLabel(summaryComposite, "Default Root Object:");
            defaultRootObjectLabel = toolkit.createLabel(summaryComposite, "");
            gdf.applyTo(defaultRootObjectLabel);
        }


        // Logging
        Section loggingSection = toolkit.createSection(summaryComposite, Section.EXPANDED | Section.TITLE_BAR);
        loggingSection.setText("Access Logging:");
        Composite loggingComposite = toolkit.createComposite(loggingSection);
        loggingSection.setClient(loggingComposite);
        loggingComposite.setLayout(new GridLayout(2, false));
        sectionGDF.applyTo(loggingSection);

        toolkit.createLabel(loggingComposite, "Logging Enabled:");
        loggingEnabledText = createText(loggingComposite);

        toolkit.createLabel(loggingComposite, "Destination Bucket:");
        loggingBucketText = createText(loggingComposite);
        gdf.applyTo(loggingBucketText);

        toolkit.createLabel(loggingComposite, "Log File Prefix:");
        loggingPrefixText = createText(loggingComposite);
        gdf.applyTo(loggingPrefixText);

        WebLinkListener webLinkListener = new WebLinkListener();
        createVerticalSpacer(loggingComposite);
        createLink(loggingComposite, webLinkListener, "Amazon CloudFront provides optional log files with information about end user access to your objects.");
        createLink(loggingComposite, webLinkListener, "For more information, see the <A HREF=\"" + ACCESS_LOGGING_DOCUMENTATION_URL + "\">Access Logs for Distributions</A> section in the Amazon CloudFront documentation.");


        // CNAMEs
        Section cnamesSection = toolkit.createSection(summaryComposite, Section.EXPANDED | Section.TITLE_BAR);
        Composite cnamesComposite = toolkit.createComposite(cnamesSection);
        cnamesSection.setClient(cnamesComposite);
        sectionGDF.applyTo(cnamesSection);
        cnamesSection.setText("CNAME Aliases:");
        cnamesComposite.setLayout(new GridLayout(2, false));

        cnamesList = new org.eclipse.swt.widgets.List(cnamesComposite, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL);
        gdf.applyTo(cnamesList);
        ((GridData)cnamesList.getLayoutData()).horizontalSpan = 2;

        createVerticalSpacer(cnamesComposite);

        createLink(cnamesComposite, webLinkListener, "A CNAME record lets you specify an alternate domain name for the domain name CloudFront provides for your distribution.");
        createLink(cnamesComposite, webLinkListener, "For more information, see the <A HREF=\"" + CNAME_DOCUMENTATION_URL + "\">Using CNAMEs with Distributions</A> section in the Amazon CloudFront documentation.");
    }


    /*
     * Utils
     */

    protected AmazonCloudFront getClient() {
        AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory(editorInput.getAccountId());
        return clientFactory.getCloudFrontClientByEndpoint(editorInput.getRegionEndpoint());
    }

    protected Text createText(Composite parent) {
        Text text = new Text(parent, SWT.READ_ONLY);
        text.setBackground(parent.getBackground());
        text.setText("");
        return text;
    }

    protected Link createLink(Composite parent, Listener linkListener, String linkText) {
        Link link = new Link(parent, SWT.WRAP);
        link.setText(linkText);
        link.addListener(SWT.Selection, linkListener);
        GridData data = new GridData(SWT.FILL, SWT.TOP, false, false);
        data.horizontalSpan = 2;
        data.widthHint = 100;
        data.heightHint = 15;
        link.setLayoutData(data);

        return link;
    }

    protected Composite createVerticalSpacer(Composite parent) {
        Composite spacer = new Composite(parent, SWT.NONE);

        GridData data = new GridData(SWT.FILL, SWT.TOP, false, false);
        spacer.setSize(SWT.DEFAULT, 5);
        data.horizontalSpan = 2;
        data.widthHint = 5;
        data.heightHint = 5;
        spacer.setLayoutData(data);

        return spacer;
    }

    protected void updateToolbar() {
        form.getToolBarManager().update(true);
    }

    protected final class RefreshAction extends Action {
        public RefreshAction() {
            this.setText("Refresh");
            this.setToolTipText("Refresh distribution information");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REFRESH));
        }

        @Override
        public void run() {
            refreshData();
        }
    }
}
