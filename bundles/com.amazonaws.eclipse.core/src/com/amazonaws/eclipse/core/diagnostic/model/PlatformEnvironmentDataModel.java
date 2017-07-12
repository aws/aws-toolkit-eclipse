/*
 * Copyright 2008-2014 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.diagnostic.model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.Bundle;

/**
 * The data model containing all the platform runtime environment information.
 */
public class PlatformEnvironmentDataModel {

    /* Provided by JRE */
    private String osName;
    private String osVersion;
    private String osArch;
    private String javaVmName;
    private String javaVmVersion;
    private String javaVersion;

    /* Eclipse plugin bundles */
    private String eclipsePlatformVersion;
    private List<Bundle> installedBundles = new LinkedList<>();

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getOsArch() {
        return osArch;
    }

    public void setOsArch(String osArch) {
        this.osArch = osArch;
    }

    public String getJavaVmName() {
        return javaVmName;
    }

    public void setJavaVmName(String javaVmName) {
        this.javaVmName = javaVmName;
    }

    public String getJavaVmVersion() {
        return javaVmVersion;
    }

    public void setJavaVmVersion(String javaVmVersion) {
        this.javaVmVersion = javaVmVersion;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getEclipsePlatformVersion() {
        return eclipsePlatformVersion;
    }

    public void setEclipsePlatformVersion(String eclipsePlatformVersion) {
        this.eclipsePlatformVersion = eclipsePlatformVersion;
    }

    public List<Bundle> getInstalledBundles() {
        return new LinkedList<>(installedBundles);
    }

    public void addInstalledBundle(Bundle bundle) {
        this.installedBundles.add(bundle);
    }

    @Override
    public String toString() {
        StringWriter sb = new StringWriter();
        PrintWriter pw = new PrintWriter(sb);

        pw.println("============= Platform environment =============");
        pw.println();

        pw.print("Eclipse platform version : ");
        pw.println(getEclipsePlatformVersion());

        pw.print("OS name : ");
        pw.println(getOsName());
        pw.print("OS version : ");
        pw.println(getOsVersion());
        pw.print("OS architecture : ");
        pw.println(getOsArch());
        pw.print("JVM name : ");
        pw.println(getJavaVmName());
        pw.print("JVM version : ");
        pw.println(getJavaVmVersion());
        pw.print("Java lang version : ");
        pw.println(getJavaVersion());
        pw.println();

        pw.println("============= Installed Plug-ins =============");
        pw.println();

        for (Bundle bundle : getInstalledBundles()) {
            pw.println(bundle.toString());
        }

        return sb.toString();
    }
}
