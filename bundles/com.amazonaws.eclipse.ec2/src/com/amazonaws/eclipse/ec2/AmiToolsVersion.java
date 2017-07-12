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

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the version number of the AMI bundling tools.
 */
public class AmiToolsVersion {
    private final int majorVersion;
    private final int minorVersion;
    private final int patch;

    private static final String regex = "^(\\d+)\\.(\\d+)-(\\d+)\\s*.*";
    private static final Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);

    /**
     * Constructs a new version object directly from major, minor, and patch
     * version numbers.
     * 
     * @param majorVersion
     *            The major version for this new version object.
     * @param minorVersion
     *            The minor version for this new version object.
     * @param patch
     *            The patch version for this new version object.
     */
    public AmiToolsVersion(int majorVersion, int minorVersion, int patch) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patch = patch;
    }

    /**
     * Constructs a new version object by parsing a version string, which is
     * assumed to be of the form:
     * <major-version>.<minor-version>-<patch-version>
     * 
     * @param versionString
     *            The version string to parse for the major, minor and patch
     *            version numbers.
     * 
     * @throws ParseException
     *             If the version string parameter was not able to be parsed
     *             into the expected format.
     */
    public AmiToolsVersion(String versionString) throws ParseException {
        String parseExceptionMessage = "Version string '" + versionString 
            + "' does not start with version number in the form " 
            + "'<major-version>.<minor-version>-<patch-version>'";
    
        Matcher matcher = pattern.matcher(versionString);
        if (! matcher.matches()) {
            throw new ParseException(parseExceptionMessage, -1);
        }

        try {
            majorVersion = Integer.parseInt(matcher.group(1));
            minorVersion = Integer.parseInt(matcher.group(2));
            patch        = Integer.parseInt(matcher.group(3));
        } catch (Throwable t) {
            throw new ParseException(parseExceptionMessage, -1);
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return majorVersion + "." + minorVersion + "-" + patch;
    }

    /**
     * Returns true if this version is greater than the specified version.
     * 
     * @param other
     *            The other version to compare against.
     * 
     * @return True if this version is greater than the other specified
     *         version.
     */
    public boolean isGreaterThan(AmiToolsVersion other) {
        if (this.majorVersion > other.majorVersion) {
            return true;
        } else if (this.majorVersion < other.majorVersion) {
            return false;
        }
        
        if (this.minorVersion > other.minorVersion) {
            return true;
        } else if (this.minorVersion < other.minorVersion) {
            return false;
        }
        
        if (this.patch > other.patch) {
            return true;
        } else if (this.patch < other.patch) {
            return false;
        }
        
        return false;
    }

    /**
     * Returns the major version component of this version number.
     * 
     * @return The major version component of this version number.
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Returns the minor version component of this version number.
     * 
     * @return The minor version component of this version number.
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Returns the patch component of this version number.
     * 
     * @return The patch component of this version number.
     */
    public int getPatch() {
        return patch;
    }
    
}
