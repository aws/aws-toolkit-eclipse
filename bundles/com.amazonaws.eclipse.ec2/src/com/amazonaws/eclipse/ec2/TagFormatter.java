/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.amazonaws.services.ec2.model.Tag;


/**
 * Class that knows how to return a list of tags as text.
 */
public class TagFormatter {

    /**
     * Returns a formatted string representing the tags given. They will be
     * sorted by their keys.
     */
    public static String formatTags(List<Tag> tags) {

        if (tags == null || tags.isEmpty()) return "";
        Collections.sort(tags, new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        
        StringBuilder allTags = new StringBuilder();
        boolean first = true;
        for (Tag tag : tags) {
            if (first) {
                first = false;
            } else {
                allTags.append("; ");
            }
            allTags.append(tag.getKey()).append("=").append(tag.getValue());
        }
            
        return allTags.toString();
    }
    
}
