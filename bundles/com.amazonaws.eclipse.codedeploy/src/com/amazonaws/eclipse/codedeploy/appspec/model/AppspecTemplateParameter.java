package com.amazonaws.eclipse.codedeploy.appspec.model;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AppspecTemplateParameter {

    private final String name;
    private final ParameterType type;
    private final String defaultValueAsString;
    private final String substitutionAnchorText;
    private final ParameterConstraints constraints;

    @JsonCreator
    public AppspecTemplateParameter(
            @JsonProperty("name") String name,
            @JsonProperty("type") ParameterType type,
            @JsonProperty("defaultValueAsString") String defaultValueAsString,
            @JsonProperty("constraints") ParameterConstraints constraints,
            @JsonProperty("substitutionAnchorText") String substitutionAnchorText) {
        this.name = name;
        this.type = type;
        this.defaultValueAsString = defaultValueAsString;
        this.constraints = constraints;
        this.substitutionAnchorText = substitutionAnchorText;
    }

    public String getName() {
        return name;
    }

    public ParameterType getType() {
        return type;
    }

    public String getDefaultValueAsString() {
        return defaultValueAsString;
    }

    public ParameterConstraints getConstraints() {
        return constraints;
    }

    public String getSubstitutionAnchorText() {
        return substitutionAnchorText;
    }

    /**
     * @throw IllegalArgumentException if the parameter model is not valid
     */
    @JsonIgnore
    public void validate() {
        checkRequiredFields();
        checkConstraints();
    }

    private void checkRequiredFields() {
        if (substitutionAnchorText == null)
            throw new IllegalArgumentException("substitutionAnchorText is not specified for the template parameter.");
        if (name == null)
            throw new IllegalArgumentException("name is not specified for parameter " + substitutionAnchorText);
        if (type == null)
            throw new IllegalArgumentException("type is not specified for parameter " + substitutionAnchorText);
        if (defaultValueAsString == null)
            throw new IllegalArgumentException("defaultValueAsString is not specified for parameter " + substitutionAnchorText);
    }

    private void checkConstraints() {
        if (type == ParameterType.STRING) {
            if (constraints.getMaxValue() != null ||
                    constraints.getMinValue() != null) {
                throw new IllegalArgumentException(
                        "Invalid constraints for parameter " + substitutionAnchorText
                        + ". minValue or maxValue cannot be set for STRING-type parameter.");
            }

            if (constraints.getValidationRegex() != null) {
                try {
                    Pattern.compile(constraints.getValidationRegex());
                } catch (PatternSyntaxException e) {
                    throw new IllegalArgumentException(
                            "Invalid regex constraint for parameter " + substitutionAnchorText, e);
                }
            }
        }

        else if (type == ParameterType.INTEGER) {
            if (constraints.getValidationRegex() != null) {
                throw new IllegalArgumentException(
                        "Invalid constraints for parameter " + substitutionAnchorText
                        + ". validationRegex cannot be set for INTEGER-type parameter.");
            }

            if (constraints.getMaxValue() != null &&
                    constraints.getMinValue() != null &&
                    constraints.getMaxValue() < constraints.getMinValue()) {
                throw new IllegalArgumentException(
                        "Invalid constraints for parameter " + substitutionAnchorText
                        + ". maxValue cannot be smaller than minValue.");
            }
        }
    }

    public static class ParameterConstraints {

        /** For STRING type */
        private final String validationRegex;

        /** For INTEGER type */
        private final Integer minValue;
        private final Integer maxValue;

        @JsonCreator
        public ParameterConstraints(
                @JsonProperty("validationRegex") String validationRegex,
                @JsonProperty("minValue") Integer minValue,
                @JsonProperty("maxValue") Integer maxValue) {
            this.validationRegex = validationRegex;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        public String getValidationRegex() {
            return validationRegex;
        }

        public Integer getMinValue() {
            return minValue;
        }

        public Integer getMaxValue() {
            return maxValue;
        }

    }

    public enum ParameterType {
        STRING,
        INTEGER
    }
}