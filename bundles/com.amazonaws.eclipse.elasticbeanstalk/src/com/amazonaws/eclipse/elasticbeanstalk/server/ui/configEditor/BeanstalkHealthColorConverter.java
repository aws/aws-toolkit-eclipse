/*
 * Copyright 2015 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import java.util.Map;

import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import com.amazonaws.util.ImmutableMapParameter;

/**
 * Helper class to convert from the String color name returned by the Beanstalk API to a
 * {@link Color} object usable in the UI
 */
public class BeanstalkHealthColorConverter {

    /**
     * Default color (Black) to return for any unknown colors returned by the service
     */
    private static final RGB UNKNOWN = new RGB(0, 0, 0);

    private final ResourceManager resourceManager;

    //@formatter:off
    private final Map<String, RGB> colorNameToRgb = new ImmutableMapParameter.Builder<String, RGB>()
            .put("Green", new RGB(0, 150, 0))
            .put("Yellow", new RGB(204, 204, 0))
            .put("Red", new RGB(255, 0, 0))
            .put("Grey", new RGB(96, 96, 96))
            .build();
    //@formatter:on

    public BeanstalkHealthColorConverter(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Convert the string representation of the color (as returned by the ElasticBeanstalk API) to a
     * SWF Color object
     * 
     * @param healthColorName
     *            Name of color returned by Beanstalk
     * @return Appropriate SWF Color or a default to handle new colors added by the service
     */
    public Color toColor(String healthColorName) {
        return resourceManager.createColor(stringColorNameToRgb(healthColorName));
    }

    /**
     * Convert the string representation of the color to an RGB object
     * 
     * @param healthColorName
     *            Name of color returned by Beanstalk
     * @return Appropriate RGB Color or a default to handle new colors added by the service
     */
    private RGB stringColorNameToRgb(String healthColorName) {
        if (colorNameToRgb.containsKey(healthColorName)) {
            return colorNameToRgb.get(healthColorName);
        }
        return UNKNOWN;
    }

}