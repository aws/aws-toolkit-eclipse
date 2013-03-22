/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.ec2;

import com.amazonaws.services.ec2.model.Image;

/**
 * Represents the different types of instances supported by EC2 and their
 * various attributes, such as memory, disk space, architecture, etc.
 */
public enum InstanceType {
    // Standard Instance Types
    MICRO("Micro",        "t1.micro",   "613 MB", "0 (EBS only)", 1, "32/64", true,  true, true),
    SMALL("Small",        "m1.small",   "1.7 GB",  "160 GB",      1, "32/64", true,  true, false),
    MEDIUM("Medium",      "m1.medium", "3.75 GB",  "410 GB",      2, "32/64", true,  true, false),
    LARGE("Large",        "m1.large",   "7.5 GB",  "850 GB",      4,    "64", false, true, false),
    XLARGE("Extra Large", "m1.xlarge",   "15 GB", "1690 GB",      8,    "64", false, true, false),

    // High Memory Instance Types
    HIGH_MEM_EXTRA_LARGE("High-Mem Extra Large",                "m2.xlarge",  "17.1 GB",  "420 GB", 2, "64", false, true, false),
    HIGH_MEM_DOUBLE_EXTRA_LARGE("High-Mem Double Extra Large",  "m2.2xlarge", "34.2 GB",  "850 GB", 4, "64", false, true, false),
    HIGH_MEM_QUAD_EXTRA_LARGE("High-Mem Quadruple Extra Large", "m2.4xlarge", "68.4 GB", "1690 GB", 8, "64", false, true, false),

    // High CPU Instance Types
    MEDIUM_HCPU("High-CPU Medium",      "c1.medium", "1.7 GB",  "350 GB",  5, "32/64",  true, true, false),
    XLARGE_HCPU("High-CPU Extra Large", "c1.xlarge",   "7 GB", "1690 GB", 20,    "64", false, true, false),

    // Cluster Compute Instance Types
    CLUSTER_QUAD_EXTRA_LARGE("Cluster Compute Quadruple Extra Large", "cc1.4xlarge",   "23 GB", "1690 GB", 33, "64", false, true, false, true),
    CLUSTER_EIGHT_EXTRA_LARGE("Cluster Compute Eight Extra Large",    "cc1.8xlarge", "60.5 GB", "3370 GB", 88, "64", false, true, false, true),
    CLUSTER_GPU_QUAD_EXTRA_LARGE("Cluster GPU Quadruple Extra Large", "cg1.4xlarge",   "22 GB", "1690 GB", 33, "64", false, true, false, true),
    ;

    /** The default instance type is a small instance */
    public static InstanceType DEFAULT = SMALL;

    /** The presentation/display name of this instance type. */
    public final String name;

    /** The EC2 ID for this instance type. */
    public final String id;

    /** The RAM (measured in Gigabytes) available on this instance type. */
    public final String memoryWithUnits;

    /** The disk space available on this instance type. */
    public final String diskSpaceWithUnits;

    /** The number of virtual cores available on this instance type. */
    public final int numberOfVirtualCores;

    /** The architecture bits (32bit or 64bit) on this instance type. */
    public final String architectureBits;

    /** Whether this instance type supports 32-bit amis */
    public final boolean supports32Bit;

    /** Whether this instance type supports 64-bit amis */
    public final boolean supports64Bit;

    /** Whether this instance type requires an EBS-backed image */
    public final boolean requiresEbsVolume;

    /** Whether this instance type requires images using hardware virtualization */
    public final boolean requiresHvmImage;


    private InstanceType(String name, String id, String memoryInGigabytes,
            String diskSpaceInGigabytes, int numberOfVirtualCores, String architectureBits,
            boolean supports32Bit, boolean supports64Bit, boolean requiresEbsVolume) {
        this(name, id, memoryInGigabytes, diskSpaceInGigabytes, numberOfVirtualCores, architectureBits, supports32Bit, supports64Bit, requiresEbsVolume, false);
    }

    private InstanceType(String name, String id, String memoryInGigabytes,
            String diskSpaceInGigabytes, int numberOfVirtualCores, String architectureBits,
            boolean supports32Bit, boolean supports64Bit, boolean requiresEbsVolume, boolean requiresHvmImage) {
        this.name = name;
        this.id = id;
        this.diskSpaceWithUnits = diskSpaceInGigabytes;
        this.memoryWithUnits = memoryInGigabytes;
        this.numberOfVirtualCores = numberOfVirtualCores;
        this.architectureBits = architectureBits;
        this.supports32Bit = supports32Bit;
        this.supports64Bit = supports64Bit;
        this.requiresEbsVolume = requiresEbsVolume;
        this.requiresHvmImage = requiresHvmImage;
    }

    /**
     * Returns whether a new instance of this type can legally be launched with
     * the image given.
     */
    public boolean canLaunch(Image image) {
        if ( image == null ) return false;
        int requiredArchitectureBits = 32;
        if ( image.getArchitecture().equalsIgnoreCase("x86_64") ) {
            requiredArchitectureBits = 64;
        }
        if ( (requiredArchitectureBits == 64 && !supports64Bit) || (requiredArchitectureBits == 32 && !supports32Bit) )
            return false;
        if ( requiresEbsVolume && !image.getRootDeviceType().equalsIgnoreCase("ebs") )
            return false;
        if ( requiresHvmImage && !image.getVirtualizationType().equalsIgnoreCase("hvm") )
            return false;

        return true;
    }
}
