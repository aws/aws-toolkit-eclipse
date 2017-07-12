package com.amazonaws.eclipse.codedeploy.deploy.wizard.page.validator;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.amazonaws.eclipse.codedeploy.appspec.model.AppspecTemplateParameter;
import com.amazonaws.eclipse.codedeploy.appspec.model.AppspecTemplateParameter.ParameterConstraints;
import com.amazonaws.eclipse.codedeploy.appspec.model.AppspecTemplateParameter.ParameterType;

public class GenericTemplateParameterValidator implements IValidator {

    private final AppspecTemplateParameter parameterModel;

    public GenericTemplateParameterValidator(AppspecTemplateParameter parameterModel) {
        this.parameterModel = parameterModel;
    }

    public AppspecTemplateParameter getParameterModel() {
        return parameterModel;
    }

    @Override
    public IStatus validate(Object value) {
        String input = (String)value;
        if (input == null) {
            return error("value must not be null.");
        }

        ParameterConstraints constraints = parameterModel.getConstraints();

        if (parameterModel.getType() == ParameterType.STRING) {
            String stringRegex = constraints.getValidationRegex();
            if (stringRegex != null && !input.matches(stringRegex)) {
                return error("value doesn't match regex \"" + stringRegex + "\"");
            }

        } else if (parameterModel.getType() == ParameterType.INTEGER) {
            try {
                int intValue = Integer.parseInt(input.trim());
                if (constraints.getMinValue() != null && intValue < constraints.getMinValue()) {
                    return error("minimum is " + constraints.getMinValue());
                }
                if (constraints.getMaxValue() != null && intValue > constraints.getMaxValue()) {
                    return error("maximum is " + constraints.getMaxValue());
                }

            } catch (NumberFormatException e) {
                return error("a number is expected");
            }
        }

        return ValidationStatus.ok();
    }

    private IStatus error(String message) {
        return ValidationStatus.error(String.format("Invalid value for %s (%s)",
                parameterModel.getSubstitutionAnchorText(), message));
    }
}
