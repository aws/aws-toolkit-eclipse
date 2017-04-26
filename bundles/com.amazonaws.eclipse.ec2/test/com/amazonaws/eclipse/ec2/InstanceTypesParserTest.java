package com.amazonaws.eclipse.ec2;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

public class InstanceTypesParserTest {

    /**
     * Tests the InstanceType parser by loading the fallback instance type
     * descriptions and making sure the loaded instance types look correct.
     *
     * This test requires the etc directory to be on the build-path in order for
     * the fallback file to be loaded.
     */
    @Test
    public void testInstanceTypesParser() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("InstanceTypes.xml");
        assertNotNull(inputStream);

        InstanceTypesParser parser = new InstanceTypesParser(inputStream);

        assertEquals("m1.small", parser.parseDefaultInstanceTypeId());

        List<InstanceType> instanceTypes = parser.parseInstanceTypes();
        assertTrue(instanceTypes.size() > 5);
        for (InstanceType type : instanceTypes) {
            assertNotNull(type.id);
            assertNotNull(type.diskSpaceWithUnits);
            assertNotNull(type.memoryWithUnits);
            assertNotNull(type.architectureBits);
            assertNotNull(type.name);
            assertTrue(type.numberOfVirtualCores > 0);
        }
    }
}
