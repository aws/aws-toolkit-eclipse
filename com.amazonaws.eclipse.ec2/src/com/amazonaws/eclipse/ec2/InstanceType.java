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
 * Represents a type of Amazon EC2 instance, including information on memory,
 * disk space, architecture, etc.
 */
public class InstanceType {

    /** The presentation/display name of this instance type. */
    public final String name;

    /** The EC2 ID for this instance type. */
    public final String id;

    /** The RAM (with units) available on this instance type. */
    public final String memoryWithUnits;

    /** The disk space (with units) available on this instance type. */
    public final String diskSpaceWithUnits;

    /** The number of virtual cores available on this instance type. */
    public final int numberOfVirtualCores;

    /** The architecture type (32, 64, or 32/64) on this instance type. */
    public final String architectureBits;

    /** Whether this instance type supports 32-bit AMIs */
    public final boolean supports32Bit;

    /** Whether this instance type supports 64-bit AMIs */
    public final boolean supports64Bit;

    /** Whether this instance type requires an EBS-backed image */
    public final boolean requiresEbsVolume;

    /** Whether this instance type requires images using hardware virtualization */
    public final boolean requiresHvmImage;


    public InstanceType(String name, String id,
            String memoryWithUnits,
            String diskSpaceWithUnits,
            int virtualCores,
            String architecture,
            boolean requiresEbsVolume) {
        this(name, id, memoryWithUnits, diskSpaceWithUnits, virtualCores, architecture, requiresEbsVolume, false);
    }

    public InstanceType(String name, String id,
            String memoryWithUnits,
            String diskSpaceWithUnits,
            int virtualCores,
            String architecture,
            boolean requiresEbsVolume,
            boolean requiresHvmImage) {
        this.name = name;
        this.id = id;
        this.diskSpaceWithUnits = diskSpaceWithUnits;
        this.memoryWithUnits = memoryWithUnits;
        this.numberOfVirtualCores = virtualCores;
        this.architectureBits = architecture;
        this.requiresEbsVolume = requiresEbsVolume;
        this.requiresHvmImage = requiresHvmImage;
        this.supports32Bit = architecture.contains("32");
        this.supports64Bit = architecture.contains("64");
    }

    /**
     * Returns whether a new instance of this type can be launched with
     * a specified image.
     */
    public boolean canLaunch(Image image) {
        if ( image == null ) return false;

        int requiredArchitectureBits = 32;
        if ( image.getArchitecture().equalsIgnoreCase("x86_64") ) {
            requiredArchitectureBits = 64;
        }
        if ( (requiredArchitectureBits == 64 && !supports64Bit) ||
             (requiredArchitectureBits == 32 && !supports32Bit) )
            return false;

        if ( requiresEbsVolume && !image.getRootDeviceType().equalsIgnoreCase("ebs") )
            return false;
        if ( requiresHvmImage && !image.getVirtualizationType().equalsIgnoreCase("hvm") )
            return false;

        return true;
    }
}
