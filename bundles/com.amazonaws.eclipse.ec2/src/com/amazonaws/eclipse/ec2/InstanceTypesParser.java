/*
 * Copyright 2013 Amazon Technologies, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class InstanceTypesParser {

    // Tag names for the instance type description file
    private static final String ID                    = "id";
    private static final String REQUIRES_EBS_VOLUME   = "RequiresEBSVolume";
    private static final String REQUIRES_HVM_IMAGE    = "RequiresHvmImage";
    private static final String ARCHITECTURE_BITS     = "ArchitectureBits";
    private static final String VIRTUAL_CORES         = "VirtualCores";
    private static final String MEMORY                = "Memory";
    private static final String DISK_SPACE            = "DiskSpace";
    private static final String INSTANCE_TYPE         = "InstanceType";
    private static final String DISPLAY_NAME          = "DisplayName";
    private static final String DEFAULT_INSTANCE_TYPE = "DefaultInstanceType";

    private Document doc;


    /**
     * Creates a new parser for instance type metadata that reads from the
     * specified InputStream.
     *
     * @param inputStream
     *            The stream to read instance type metadata from.
     *
     * @throws IOException
     *             If there were any problems reading from the stream.
     */
    public InstanceTypesParser(InputStream inputStream) throws IOException {
        try {
            doc = parseDocument(inputStream);
        } catch (Exception e) {
            throw new IOException("Unable to parse instance type descriptions", e);
        }
    }

    /**
     * Returns a list of the InstanceType objects parsed from the provided
     * descriptions.
     */
    public List<InstanceType> parseInstanceTypes() {
        LinkedList<InstanceType> instanceTypes = new LinkedList<>();
        NodeList instanceTypeNodeList = doc.getElementsByTagName(INSTANCE_TYPE);
        for (int i = 0; i < instanceTypeNodeList.getLength(); i++) {
            Node instanceTypeNode = instanceTypeNodeList.item(i);
            instanceTypes.add(parseInstanceTypeElement(instanceTypeNode));
        }
        return instanceTypes;
    }

    /**
     * Returns the default instance type specified in the instance type
     * descriptions.
     */
    public String parseDefaultInstanceTypeId() {
        NodeList nodes = doc.getElementsByTagName(DEFAULT_INSTANCE_TYPE);
        return getAttribute(nodes.item(0), ID);
    }

    private Document parseDocument(InputStream inputStream)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();
        return doc;
    }

    /**
     * Parses an InstanceType element from the document:
     * <InstanceType id="t1.micro">
     *   <DisplayName>Micro</DisplayName>
     *   <Memory>613 MB</Memory>
     *   <DiskSpace>0 (EBS only)</DiskSpace>
     *   <VirtualCores>1</VirtualCores>
     *   <ArchitectureBits>32/64</ArchitectureBits>
     *   <RequiresEBSVolume>true</RequiresEBSVolume>
     *   <RequiresHvmImage>false</RequiresHvmImage>
     * </InstanceType>
     */
    private InstanceType parseInstanceTypeElement(Node instanceTypeNode) {
        String id = getAttribute(instanceTypeNode, ID);

        String displayName = null;
        String memory = null;
        String diskSpace = null;
        String architecture = null;
        int virtualCores = 0;
        boolean requiresEbsVolume = false;
        boolean requiresHvmImage = false;

        Node node = instanceTypeNode.getFirstChild();
        do {
            String nodeName = node.getNodeName();
            if (nodeName.equals(DISPLAY_NAME)) {
                displayName = getChildText(node);
            } else if (nodeName.equals(MEMORY)) {
                memory = getChildText(node);
            } else if (nodeName.equals(DISK_SPACE)) {
                diskSpace = getChildText(node);
            } else if (nodeName.equals(VIRTUAL_CORES)) {
                virtualCores = parseInt(node);
            } else if (nodeName.equals(ARCHITECTURE_BITS)) {
                architecture = getChildText(node);
            } else if (nodeName.equals(REQUIRES_EBS_VOLUME)) {
                requiresEbsVolume = Boolean.parseBoolean(getChildText(node));
            } else if (nodeName.equals(REQUIRES_HVM_IMAGE)) {
                requiresHvmImage = Boolean.parseBoolean(getChildText(node));
            }
        } while ((node = node.getNextSibling()) != null);

        return new InstanceType(displayName, id, memory, diskSpace, virtualCores, architecture, requiresEbsVolume, requiresHvmImage);
    }

    private int parseInt(Node node) {
        try {
            return Integer.parseInt(getChildText(node));
        } catch (NumberFormatException nfe) {
            Status status = new Status(IStatus.WARNING, Ec2Plugin.PLUGIN_ID, "Error parsing Amazon EC2 instance type", nfe);
            Ec2Plugin.getDefault().getLog().log(status);

            return -1;
        }
    }

    private String getAttribute(Node node, String attributeName) {
        return node.getAttributes().getNamedItem(attributeName).getTextContent();
    }

    private String getChildText(Node node) {
        return node.getFirstChild().getTextContent();
    }
}