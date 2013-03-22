/*
 * Copyright 2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.android.sdk.newproject;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.amazonaws.eclipse.android.sdk.AndroidSDKPlugin;

public class AndroidManifestFile {
    private File manifestFile;
    private Transformer transformer;

    public AndroidManifestFile(IProject project) {
        IPath projectLocation = project.getLocation();
        IPath androidManifestFile = projectLocation.append("AndroidManifest.xml");
        manifestFile = androidManifestFile.toFile();

        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException("Unable to configure output transformer for Android manifest files", e);
        } catch (TransformerFactoryConfigurationError e) {
            throw new RuntimeException("Unable to configure output transformer for Android manifest files", e);
        }
    }

    /**
     * Adds an activity to the default application in the manifest:
     *
     * <activity android:name="S3UploaderActivity"
     *           android:label="@string/app_name">
     *    <intent-filter>
     *       <action android:name="android.intent.action.MAIN" />
     *       <category android:name="android.intent.category.LAUNCHER" />
     *    </intent-filter>
     * </activity>
     */
    public void addSampleActivity() {
        try {
            Document doc = parse();

            Node manifestElement = firstChildElement(doc, "manifest");
            if (manifestElement == null) {
                throw new RuntimeException("No manifest element in manifest.xml");
            }

            Node applicationElement = firstChildElement(manifestElement, "application");
            if (applicationElement == null) {
                throw new RuntimeException("No application element in manifest.xml");
            }

            applicationElement.appendChild(createActivityElement(doc));

            StreamResult result = new StreamResult(manifestFile);
            transformer.transform(new DOMSource(doc), result);
        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, AndroidSDKPlugin.PLUGIN_ID,
                "Unable to update Android project manifest with settings for the AWS SDK for Android", e);
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }
    }

    /**
     * Updates the Android manifest file for the specified Android project with
     * customizations for building Android projects that work with AWS.
     *
     * Specifically, this method adds the following configuration:
     *   uses-sdk android:minSdkVersion="8"
     *   uses-permission android:name="android.permission.INTERNET"
     */
    public void initialize() {
        try {
            Document doc = parse();
            Node manifestElement = firstChildElement(doc, "manifest");
            Node applicationElement = firstChildElement(manifestElement, "application");
            manifestElement.removeChild(applicationElement);

            Element usesSdkElement = doc.createElement("uses-sdk");
            usesSdkElement.setAttribute("android:minSdkVersion", "8");
            manifestElement.appendChild(usesSdkElement);

            Element usesPermissionElement = doc.createElement("uses-permission");
            usesPermissionElement.setAttribute("android:name", "android.permission.INTERNET");
            manifestElement.appendChild(usesPermissionElement);

            // Add the application element back, so that it
            // comes after the uses-permission elements
            manifestElement.appendChild(applicationElement);

            StreamResult result = new StreamResult(manifestFile);
            transformer.transform(new DOMSource(doc), result);
        } catch (Exception e) {
            IStatus status = new Status(IStatus.ERROR, AndroidSDKPlugin.PLUGIN_ID,
                "Unable to update Android project manifest with settings for the AWS SDK for Android", e);
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }
    }

    private Document parse() {
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return docBuilder.parse(manifestFile);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse Android manifest file", e);
        }
    }

    private Node createActivityElement(Document doc) {
        Element activityElement = doc.createElement("activity");
        activityElement.setAttribute("android:name", "S3UploaderActivity");
        activityElement.setAttribute("android:label", "@string/app_name");

        Element intentFilterElement = doc.createElement("intent-filter");
        Element actionElement = doc.createElement("action");
        actionElement.setAttribute("android:name", "android.intent.action.MAIN");
        Element categoryElement = doc.createElement("category");
        categoryElement.setAttribute("android:name", "android.intent.category.LAUNCHER");
        intentFilterElement.appendChild(actionElement);
        intentFilterElement.appendChild(categoryElement);
        activityElement.appendChild(intentFilterElement);

        return activityElement;
    }

    private Node firstChildElement(Node parent, String elementName) {
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            Node node = parent.getChildNodes().item(i);
            if (node.getNodeName().equals(elementName)) return node;
        }
        return null;
    }
}