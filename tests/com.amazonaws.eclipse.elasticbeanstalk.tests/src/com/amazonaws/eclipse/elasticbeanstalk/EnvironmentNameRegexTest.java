package com.amazonaws.eclipse.elasticbeanstalk;

import org.eclipse.core.runtime.IStatus;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import com.amazonaws.eclipse.elasticbeanstalk.server.ui.databinding.EnvironmentNameValidator;

/**
 * Test cases for environment name field in the
 * "New Elastic Beanstalk application" wizard page.
 *
 */
public class EnvironmentNameRegexTest {

    @Test
    public void testEnvironmentNameValidation() {

        final EnvironmentNameValidator validator = new EnvironmentNameValidator();

        assertEquals(validator.validate("").getSeverity(), IStatus.ERROR);

        assertEquals(validator.validate("a").getSeverity(), IStatus.ERROR);

        assertEquals(validator.validate("-a").getSeverity(), IStatus.ERROR);

        assertEquals(validator.validate("-a-").getSeverity(), IStatus.ERROR);

        assertEquals(validator.validate("ab&&cd").getSeverity(), IStatus.ERROR);

        assertEquals(validator.validate("abcd!").getSeverity(), IStatus.ERROR);

        assertEquals(validator.validate("-abcd-").getSeverity(), IStatus.ERROR);

        assertEquals(validator.validate("abcd").getSeverity(), IStatus.OK);

        assertEquals(validator.validate("ab-cd").getSeverity(), IStatus.OK);

        assertEquals(validator.validate("ab-c123d").getSeverity(), IStatus.OK);
    }

}
