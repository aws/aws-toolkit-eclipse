package com.amazonaws.eclipse.opsworks.deploy.wizard.page;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.amazonaws.eclipse.opsworks.deploy.wizard.model.S3ApplicationSource;
import com.amazonaws.services.opsworks.model.App;
import com.amazonaws.services.opsworks.model.EnvironmentVariable;

public class ExistingAppConfigurationReviewComposite extends Composite {

    private final App app;
    private final S3ApplicationSource parsedS3ApplicationSource;

    ExistingAppConfigurationReviewComposite(Composite parent, App app) {
        super(parent, SWT.NONE);

        this.app = app;
        parsedS3ApplicationSource = S3ApplicationSource.parse(app.getAppSource());

        setLayout(new GridLayout(1, false));
        createControls(this);
    }

    S3ApplicationSource getParsedS3ApplicationSource() {
        return parsedS3ApplicationSource;
    }

    private void createControls(Composite parent) {
        createBasicSettingSection(parent);
        createApplicationSourceSection(parent);
        createEnvironmentVariablesSection(parent);
        createCustomDomainsSection(parent);
        createSslSettingsSection(parent);
    }

    private void createBasicSettingSection(Composite parent) {
        Group settingsGroup = newGroup(parent, "Settings");
        settingsGroup.setLayout(new GridLayout(2, true));

        newFillingLabel(settingsGroup, "Name").setFont(JFaceResources.getBannerFont());
        newFillingLabel(settingsGroup, app.getName());

        newFillingLabel(settingsGroup, "Short name").setFont(JFaceResources.getBannerFont());
        newFillingLabel(settingsGroup, app.getShortname());

        newFillingLabel(settingsGroup, "OpsWorks ID").setFont(JFaceResources.getBannerFont());
        newFillingLabel(settingsGroup, app.getAppId());

        newFillingLabel(settingsGroup, "Type").setFont(JFaceResources.getBannerFont());
        newFillingLabel(settingsGroup, toCamelCase(app.getType()));
    }

    private void createApplicationSourceSection(Composite parent) {
        Group applicationSourceGroup = newGroup(parent, "Application Source");
        applicationSourceGroup.setLayout(new GridLayout(2, true));

        S3ApplicationSource s3Source = S3ApplicationSource.parse(app.getAppSource());

        if (s3Source == null) {
            newFillingLabel(
                    applicationSourceGroup,
                    String.format("Unrecognized application source %s:[%s]",
                            app.getAppSource().getType(), app.getAppSource().getUrl()),
                    2);
            return;
        }

        newFillingLabel(applicationSourceGroup, "S3 bucket").setFont(JFaceResources.getBannerFont());
        newFillingLabel(applicationSourceGroup, s3Source.getBucketName());

        if (s3Source.getRegion() != null) {
            newFillingLabel(applicationSourceGroup, "Bucket region").setFont(JFaceResources.getBannerFont());
            newFillingLabel(applicationSourceGroup, s3Source.getRegion().getName());
        }

        newFillingLabel(applicationSourceGroup, "Key").setFont(JFaceResources.getBannerFont());
        newFillingLabel(applicationSourceGroup, s3Source.getKeyName());
    }

    private void createEnvironmentVariablesSection(Composite parent) {
        Group envVarGroup = newGroup(parent, "Environment Variables");
        envVarGroup.setLayout(new GridLayout(2, true));

        if (app.getEnvironment() == null || app.getEnvironment().isEmpty()) {
            newFillingLabel(envVarGroup, "None", 2);
        }

        for (EnvironmentVariable envVar : app.getEnvironment()) {
            newFillingLabel(envVarGroup, envVar.getKey()).setFont(JFaceResources.getBannerFont());
            newFillingLabel(envVarGroup, envVar.getValue());
        }
    }

    private void createCustomDomainsSection(Composite parent) {
        Group domainsGroup = newGroup(parent, "Custom Domains");
        domainsGroup.setLayout(new GridLayout(1, true));

        if (app.getDomains() == null || app.getDomains().isEmpty()) {
            newFillingLabel(domainsGroup, "None");
        }

        for (String domain : app.getDomains()) {
            newFillingLabel(domainsGroup, domain);
        }
    }

    private void createSslSettingsSection(Composite parent) {
        Group sslSettingsGroup = newGroup(parent, "SSL Settings");
        sslSettingsGroup.setLayout(new GridLayout(2, true));

        newFillingLabel(sslSettingsGroup, "SSL enabled").setFont(JFaceResources.getBannerFont());
        newFillingLabel(sslSettingsGroup, app.getEnableSsl() ? "Yes" : "No");

        if (app.getEnableSsl()) {
            newFillingLabel(sslSettingsGroup, "SSL certificate").setFont(JFaceResources.getBannerFont());
            newFillingLabel(sslSettingsGroup, app.getSslConfiguration().getCertificate());

            newFillingLabel(sslSettingsGroup, "SSL certificate key").setFont(JFaceResources.getBannerFont());
            newFillingLabel(sslSettingsGroup, app.getSslConfiguration().getPrivateKey());

            newFillingLabel(sslSettingsGroup, "SSL certificates of CA").setFont(JFaceResources.getBannerFont());
            newFillingLabel(sslSettingsGroup, app.getSslConfiguration().getChain());
        }
    }

    private static String toCamelCase(String string) {
        if (string.length() == 0) {
            return "";
        } else if (string.length() == 1) {
            return string.substring(0, 0).toUpperCase();
        } else {
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        }
    }
}
