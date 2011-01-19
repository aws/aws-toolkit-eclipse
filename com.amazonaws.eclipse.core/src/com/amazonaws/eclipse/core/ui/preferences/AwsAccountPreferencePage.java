/*
 * Copyright 2008-2011 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.core.ui.preferences;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.AwsUrls;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.ui.PreferenceLinkListener;
import com.amazonaws.eclipse.core.ui.WebLinkListener;

/**
 * Preference page for basic AWS account information.
 */
public class AwsAccountPreferencePage
	extends AwsToolkitPreferencePage
	implements IWorkbenchPreferencePage {

	private ObfuscatingStringFieldEditor userIdFieldEditor;
	private ObfuscatingStringFieldEditor accessKeyFieldEditor;
	private ObfuscatingStringFieldEditor secretKeyFieldEditor;
	private FileFieldEditor certificateFieldEditor;
	private FileFieldEditor certificatePrivateKeyFieldEditor;


	/** The checkbox controling how we display the secret key */
	private Button hideSecretKeyCheckbox;

	/** The Text control in the secret key field editor */
	private Text secretKeyText;

	/**
	 * Creates the preference page and connects it to the plugin's preference
	 * store.
	 */
	public AwsAccountPreferencePage() {
		super("AWS Toolkit Preferences");

		setPreferenceStore(AwsToolkitCore.getDefault().getPreferenceStore());
		setDescription("AWS Toolkit Preferences");
	}

	/*
	 * PreferencePage Interface
	 */

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(gridData);

		WebLinkListener webLinkListener = new WebLinkListener();
		createAwsAccountSection(composite, webLinkListener);
		createSpacer(composite);
		createOptionalSection(composite, webLinkListener);
		createSpacer(composite);

		Link networkConnectionLink = new Link(composite, SWT.NULL);
		networkConnectionLink.setText("See <a href=\"org.eclipse.ui.net.NetPreferences\">Network connections</a> to configure how the AWS Toolkit connects to the internet.");
		PreferenceLinkListener preferenceLinkListener = new PreferenceLinkListener();
		networkConnectionLink.addListener(SWT.Selection, preferenceLinkListener);

		String javaForumLinkText = "Get help or provide feedback on the " +
				"<a href=\"" + AwsUrls.JAVA_DEVELOPMENT_FORUM_URL + "\">AWS Java Development forum</a>. ";
        newLink(webLinkListener, javaForumLinkText, composite);

		return composite;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		userIdFieldEditor.loadDefault();
		accessKeyFieldEditor.loadDefault();
		secretKeyFieldEditor.loadDefault();

		certificateFieldEditor.loadDefault();
		certificatePrivateKeyFieldEditor.loadDefault();

		super.performDefaults();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		// Clean up the AWS User ID and store it
		String userId = userIdFieldEditor.getStringValue();
		userId = userId.replace("-", "");
		userId = userId.replace(" ", "");
		userIdFieldEditor.setStringValue(userId);
		userIdFieldEditor.store();

		// Clean up the AWS Access Key and store it
		String accessKey = accessKeyFieldEditor.getStringValue();
		accessKey = accessKey.trim();
		accessKeyFieldEditor.setStringValue(accessKey);
		accessKeyFieldEditor.store();

		// Clean up the AWS Secret Key and store it
		String secretKey = secretKeyFieldEditor.getStringValue();
		secretKey = secretKey.trim();
		secretKeyFieldEditor.setStringValue(secretKey);
		secretKeyFieldEditor.store();

		// Save the certificate and private key
		certificateFieldEditor.store();
		certificatePrivateKeyFieldEditor.store();

		return super.performOk();
	}

	/*
	 * Private Interface
	 */

	/**
	 * Creates the widgets for the AWS account information section on this
	 * preference page.
	 *
	 * @param parent
	 *            The parent preference page composite.
	 * @param webLinkListener
	 *            The listener to attach to links.
	 */
	private void createAwsAccountSection(Composite parent,
			WebLinkListener webLinkListener) {
		Group awsAccountGroup = newGroup("AWS Security Credentials:", parent);

        String linkText =
              "<a href=\"" + AwsUrls.SIGN_UP_URL + "\">Sign up for a new AWS account</a> or " +
              "<a href=\"" + AwsUrls.SECURITY_CREDENTIALS_URL + "\">find your existing AWS security credentials</a>.";
        newLink(webLinkListener, linkText, awsAccountGroup);
        createSpacer(awsAccountGroup);

		accessKeyFieldEditor = newStringFieldEditor(PreferenceConstants.P_ACCESS_KEY, "&Access Key ID:", awsAccountGroup);
		secretKeyFieldEditor = newStringFieldEditor(PreferenceConstants.P_SECRET_KEY, "&Secret Access Key:", awsAccountGroup);

		// create an empty label in the first column so that the hide secret key
		// checkbox lines up with the other text controls
		new Label(awsAccountGroup, SWT.NONE);

		hideSecretKeyCheckbox = new Button(awsAccountGroup, SWT.CHECK);
		hideSecretKeyCheckbox.setText("Show secret access key");
		hideSecretKeyCheckbox.setSelection(false);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.verticalIndent = -6;
		gridData.horizontalIndent = 3;
		hideSecretKeyCheckbox.setLayoutData(gridData);
		hideSecretKeyCheckbox.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				updateSecretKeyText();
			}
		});

		secretKeyText = secretKeyFieldEditor.getTextControl(awsAccountGroup);

		updateSecretKeyText();
		tweakLayout((GridLayout)awsAccountGroup.getLayout());
	}

	/**
	 * Creates a new label to serve as an example for a field, using the
	 * specified text. The label will be displayed with a subtle font. This
	 * method assumes that the grid layout for the specified composite contains
	 * three columns.
	 *
	 * @param composite
	 *            The parent component for this new widget.
	 * @param text
	 *            The example text to display in the new label.
	 */
	private void createFieldExampleLabel(Composite composite, String text) {
		Label l = new Label(composite, SWT.NONE);
		Font font = l.getFont();

		l = new Label(composite, SWT.NONE);
		l.setText(text);

		FontData[] fontData = font.getFontData();
		if (fontData.length > 0) {
			FontData fd = fontData[0];
			fd.setHeight(10);
			fd.setStyle(SWT.ITALIC);

			l.setFont(new Font(Display.getCurrent(), fd));
		}

		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gridData.verticalAlignment = SWT.TOP;
		gridData.horizontalIndent = 3;
		gridData.verticalIndent = -4;
		l.setLayoutData(gridData);
	}

	/**
	 * Updates the secret key text according to whether or not the
	 * "display secret key in plain text" checkbox is selected or not.
	 */
	private void updateSecretKeyText() {
		if (hideSecretKeyCheckbox == null) return;
		if (secretKeyText == null) return;

		if (hideSecretKeyCheckbox.getSelection()) {
			secretKeyText.setEchoChar('\0');
		} else {
			secretKeyText.setEchoChar('*');
		}
	}

    /**
     * Creates the widgets for the optional configuration section on this
     * preference page.
     *
     * @param parent
     *            The parent preference page composite.
     * @param webLinkListener
     *            The listener to attach to links.
     */
	private void createOptionalSection(Composite parent, WebLinkListener webLinkListener) {
		Group optionalConfigGroup = newGroup("Optional Configuration:", parent);

		String linkText = "Your AWS account number and X.509 certificate are only needed if you want to bundle EC2 instances from Eclipse.  "
		    + "<a href=\"" + AwsUrls.SECURITY_CREDENTIALS_URL + "\">Manage your AWS X.509 certificate</a>.";
		newLink(webLinkListener, linkText, optionalConfigGroup);

		createSpacer(optionalConfigGroup);

        userIdFieldEditor = newStringFieldEditor(PreferenceConstants.P_USER_ID, "Account &Number:", optionalConfigGroup);
        createFieldExampleLabel(optionalConfigGroup, "ex: 1111-2222-3333");

		certificateFieldEditor = newFileFieldEditor(PreferenceConstants.P_CERTIFICATE_FILE,
				"&Certificate File:", optionalConfigGroup);
		certificatePrivateKeyFieldEditor = newFileFieldEditor(PreferenceConstants.P_PRIVATE_KEY_FILE,
				"&Private Key File:", optionalConfigGroup);

		tweakLayout((GridLayout)optionalConfigGroup.getLayout());
	}

}
