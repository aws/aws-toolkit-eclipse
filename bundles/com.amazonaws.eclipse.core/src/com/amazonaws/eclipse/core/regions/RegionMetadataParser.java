/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.regions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parses the Eclipse toolkit region metadata file to pull out information about
 * the available regions, names, IDs, and what service endpoints are available
 * in each region.
 */
public class RegionMetadataParser {

    private static final String FLAG_ICON_TAG           = "flag-icon";
    private static final String REGION_DISPLAY_NAME_TAG = "displayname";
    private static final String REGION_SYSTEM_ID_TAG    = "systemname";
    private static final String REGION_TAG              = "region";
    private static final String SERVICE_TAG             = "service";
    private static final String SERVICE_ID_ATTRIBUTE    = "serviceId";
    private static final String SERVICE_NAME_ATTRIBUTE  = "name";
    private static final String SIGNER_ATTRIBUTE        = "signer";
    private static final String RESTRICTIONS            = "restrictions";

    /**
     * Parses the specified input stream and returns a list of the regions
     * declared in it.
     *
     * @param input
     *            The stream containing the region metadata to parse.
     *
     * @return The list of parsed regions.
     */
    public List<Region> parseRegionMetadata(InputStream input) {
        Document document;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            document = documentBuilder.parse(input);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse region metadata file: " + e.getMessage(), e);
        }

        NodeList regionNodes = document.getElementsByTagName(REGION_TAG);
        List<Region> regions = new ArrayList<>();
        for (int i = 0; i < regionNodes.getLength(); i++) {
            Node node = regionNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)node;
                regions.add(parseRegionElement(element));
            }
        }
        return regions;
    }

    private Region parseRegionElement(Element regionElement) {
        String name = getTagValue(REGION_DISPLAY_NAME_TAG, regionElement);
        String id = getTagValue(REGION_SYSTEM_ID_TAG, regionElement);
        String flagIcon = getTagValue(FLAG_ICON_TAG, regionElement);
        String restriction = getTagValue(RESTRICTIONS, regionElement);
        Region region = new RegionImpl(name, id, flagIcon, restriction);

        NodeList serviceNodes = regionElement.getElementsByTagName(SERVICE_TAG);
        for (int i = 0; i < serviceNodes.getLength(); i++) {
            Node node = serviceNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)node;
                String serviceName = getAttributeValue(element, SERVICE_NAME_ATTRIBUTE);
                String serviceId = getAttributeValue(element, SERVICE_ID_ATTRIBUTE);
                String signer = getAttributeValue(element, SIGNER_ATTRIBUTE);
                String endpoint = element.getTextContent();

                region.getServiceEndpoints().put(serviceName, endpoint);
                region.getServicesByName().put(serviceName,
                    new Service(serviceName, serviceId, endpoint, signer));
            }
        }

        return region;
    }

    private static String getAttributeValue(Element element, String attribute) {
        if (!element.hasAttribute(attribute)) {
            return null;
        }

        return element.getAttribute(attribute);
    }

    private static String getTagValue(String tagName, Element element){
        Node tagNode = element.getElementsByTagName(tagName).item(0);
        if ( tagNode == null ) {
            return null;
        }
        NodeList nodes= tagNode.getChildNodes();
        Node node = nodes.item(0);

        return node.getNodeValue();
    }
}
