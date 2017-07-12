package com.amazonaws.eclipse.codedeploy.appspec;

import static com.amazonaws.eclipse.codedeploy.appspec.PreferenceStoreConstants.LOCATION_SEPARATOR;
import static com.amazonaws.eclipse.codedeploy.appspec.PreferenceStoreConstants.P_CUSTOM_APPSPEC_TEMPLATE_LOCATIONS;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Bundle;

import com.amazonaws.eclipse.codedeploy.CodeDeployPlugin;
import com.amazonaws.eclipse.codedeploy.appspec.model.AppspecTemplateMetadataModel;
import com.amazonaws.util.StringUtils;

public class AppspecTemplateRegistry {

    /**
     * All the default template metadata files are located inside
     * {plugin-root}/template-metadata
     */
    private static final String DEFAULT_TEMPLATE_METADATA_BASEDIR = "template-metadata";

    private static final AppspecTemplateRegistry INSTANCE = new AppspecTemplateRegistry(
            CodeDeployPlugin.getDefault().getPreferenceStore());

    private final IPreferenceStore prefStore;

    public static AppspecTemplateRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Load all the template metadata files located inside
     * {plugin-root}/template-metadata
     */
    public List<AppspecTemplateMetadataModel> getDefaultTemplates() {

        List<AppspecTemplateMetadataModel> models = new LinkedList<>();

        try {
            Bundle bundle = CodeDeployPlugin.getDefault().getBundle();
            URL bundleBaseUrl = FileLocator.resolve(bundle.getEntry("/"));
            File defaultTemplateMetadataDir = new File(bundleBaseUrl.getFile(),
                    DEFAULT_TEMPLATE_METADATA_BASEDIR);

            for (File metadataFile : defaultTemplateMetadataDir.listFiles()) {
                try {
                    models.add(AppspecTemplateMetadataModel.loadFromFile(metadataFile, true));
                } catch (Exception e) {
                    // log exception and proceed to the next template
                    CodeDeployPlugin.getDefault().warn(String.format(
                            "Failed to load template metadata from %s.",
                            metadataFile.getAbsolutePath()),
                            e);
                }
            }

            return models;

        } catch (Exception e) {
            CodeDeployPlugin.getDefault().reportException(
                    "Failed to load default appspec templates.", e);
            return null;
        }

    }

    public List<AppspecTemplateMetadataModel> getCustomTemplates() {

        List<AppspecTemplateMetadataModel> models = new LinkedList<>();

        String prefValue = prefStore.getString(P_CUSTOM_APPSPEC_TEMPLATE_LOCATIONS);
        if (prefValue != null && !prefValue.isEmpty()) {

            String[] locations = prefValue.split(Pattern.quote(LOCATION_SEPARATOR));

            for (String location : locations) {
                File metadataFile = new File(location);
                try {
                    models.add(AppspecTemplateMetadataModel.loadFromFile(metadataFile, true));
                } catch (Exception e) {
                    // log exception and proceed to the next template
                    CodeDeployPlugin.getDefault().warn(String.format(
                            "Failed to load template metadata from %s.",
                            metadataFile.getAbsolutePath()),
                            e);
                }
            }
        }

        return models;
    }

    public Set<String> getAllTemplateNames() {
        Set<String> allNames = new HashSet<>();

        for (AppspecTemplateMetadataModel template : getDefaultTemplates()) {
            allNames.add(template.getTemplateName());
        }
        for (AppspecTemplateMetadataModel template : getCustomTemplates()) {
            allNames.add(template.getTemplateName());
        }

        return allNames;
    }

    public AppspecTemplateMetadataModel importCustomTemplateMetadata(File metadataFile) {
        AppspecTemplateMetadataModel newTemplate = AppspecTemplateMetadataModel
                .loadFromFile(metadataFile, true);

        // Check template name conflict
        if (getAllTemplateNames().contains(newTemplate.getTemplateName())) {
            throw new RuntimeException(String.format(
                    "The \"%s\" template already exists.", newTemplate.getTemplateName()));
        }

        String prefValue = prefStore.getString(P_CUSTOM_APPSPEC_TEMPLATE_LOCATIONS);
        if (prefValue != null && !prefValue.isEmpty()) {
            List<String> locations = new LinkedList<>(
                    Arrays.asList(prefValue.split(Pattern.quote(LOCATION_SEPARATOR))));
            locations.add(metadataFile.getAbsolutePath());

            prefStore.setValue(
                    P_CUSTOM_APPSPEC_TEMPLATE_LOCATIONS,
                    StringUtils.join(LOCATION_SEPARATOR, locations.toArray(new String[locations.size()])));

        } else {
            prefStore.setValue(
                    P_CUSTOM_APPSPEC_TEMPLATE_LOCATIONS,
                    metadataFile.getAbsolutePath());
        }

        return newTemplate;
    }

    private AppspecTemplateRegistry(IPreferenceStore prefStore) {
        this.prefStore = prefStore;
    };
}
