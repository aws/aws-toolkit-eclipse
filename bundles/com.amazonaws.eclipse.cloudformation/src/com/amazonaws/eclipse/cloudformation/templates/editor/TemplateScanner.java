/*
 * Copyright 2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.cloudformation.templates.editor;

import static com.amazonaws.eclipse.cloudformation.preferences.EditorPreferences.TokenPreference.buildTemplateToken;
import static com.amazonaws.eclipse.cloudformation.preferences.TemplateTokenPreferenceNames.INTRINSIC_FUNCTION;
import static com.amazonaws.eclipse.cloudformation.preferences.TemplateTokenPreferenceNames.KEY;
import static com.amazonaws.eclipse.cloudformation.preferences.TemplateTokenPreferenceNames.PSEUDO_PARAMETER;
import static com.amazonaws.eclipse.cloudformation.preferences.TemplateTokenPreferenceNames.RESOURCE_TYPE;
import static com.amazonaws.eclipse.cloudformation.preferences.TemplateTokenPreferenceNames.VALUE;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;

import com.amazonaws.eclipse.cloudformation.templates.schema.IntrinsicFunction;
import com.amazonaws.eclipse.cloudformation.templates.schema.PseudoParameter;
import com.amazonaws.eclipse.cloudformation.templates.schema.TemplateSchemaRules;

public class TemplateScanner extends RuleBasedScanner {

    private final IPreferenceStore preferenceStore;

    private final Token INTRINSIC_FUNCTION_TOKEN;
    private final Token PSEUDO_PARAMETER_TOKEN;
    private final Token RESOURCE_TYPE_TOKEN;

    private final Token KEY_TOKEN;
    private final Token VALUE_TOKEN;

    private static final class TemplateWordDetector implements IWordDetector {

        private static final Set<Character> SYMBOLS = new HashSet<>();

        static {
            SYMBOLS.add('[');
            SYMBOLS.add(']');
            SYMBOLS.add('{');
            SYMBOLS.add('}');
            SYMBOLS.add(',');
            SYMBOLS.add(':');
            SYMBOLS.add('"');
            SYMBOLS.add('\'');
        }

        @Override
        public boolean isWordStart(char c) {
            if (Character.isWhitespace(c)) return false;

            if (SYMBOLS.contains(c)) return false;

            return true;
        }

        @Override
        public boolean isWordPart(char c) {
            if (Character.isWhitespace(c)) return false;

            // TODO: This one symbol isn't valid for a word start,
            //       but is valid for a word part
            if (c == ':') return true;

            if (SYMBOLS.contains(c)) return false;

            return true;
        }
    }

    private static class TemplateWhitespaceDetector implements IWhitespaceDetector {
        @Override
        public boolean isWhitespace(char c) {
            return Character.isWhitespace(c);
        }
    }

    private static class TemplateWordRule extends WordRule {
        private final IToken keyToken;

        public TemplateWordRule(TemplateWordDetector templateWordDetector, IToken keyToken, IToken valueToken, boolean b) {
            super(templateWordDetector, valueToken, b);
            this.keyToken = keyToken;
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner) {
            IToken token = super.evaluate(scanner);

            if (token == this.fDefaultToken) {
                int c = scanner.read();
                int readAhead = 1;

                while (c == '"' || c != ICharacterScanner.EOF && Character.isWhitespace((char)c)) {
                    c = scanner.read();
                    readAhead++;
                }

                for (int i = 0; i < readAhead; i++) scanner.unread();

                if (c == ':') return keyToken;
            }

            return token;
        }
    }

    public TemplateScanner(IPreferenceStore store) {

        preferenceStore = store;

        INTRINSIC_FUNCTION_TOKEN = new Token(buildTemplateToken(preferenceStore, INTRINSIC_FUNCTION).toTextAttribute());
        PSEUDO_PARAMETER_TOKEN = new Token(buildTemplateToken(preferenceStore, PSEUDO_PARAMETER).toTextAttribute());
        RESOURCE_TYPE_TOKEN = new Token(buildTemplateToken(preferenceStore, RESOURCE_TYPE).toTextAttribute());

        KEY_TOKEN = new Token(buildTemplateToken(preferenceStore, KEY).toTextAttribute());
        VALUE_TOKEN = new Token(buildTemplateToken(preferenceStore, VALUE).toTextAttribute());

        // TODO: Can we really ignore case for CloudFormation templates?
        WhitespaceRule whitespaceRule = new WhitespaceRule(new TemplateWhitespaceDetector());
        TemplateWordRule templateWordRule = new TemplateWordRule(new TemplateWordDetector(), KEY_TOKEN, VALUE_TOKEN, true);

        TemplateSchemaRules schemaRules = TemplateSchemaRules.getInstance();

        for (PseudoParameter pseudoParameter : schemaRules.getPseudoParameters()) {
            templateWordRule.addWord(pseudoParameter.getName(), PSEUDO_PARAMETER_TOKEN);
        }

        for (IntrinsicFunction intrinsicFunction : schemaRules.getIntrinsicFuntions()) {
            templateWordRule.addWord(intrinsicFunction.getName(), INTRINSIC_FUNCTION_TOKEN);
        }

        for (String resourceType : schemaRules.getResourceTypeNames()) {
            templateWordRule.addWord(resourceType, RESOURCE_TYPE_TOKEN);
        }

        setRules(new IRule[] {
                whitespaceRule,
                templateWordRule
        });
    }

    public void resetTokens() {
        KEY_TOKEN.setData(buildTemplateToken(preferenceStore, KEY).toTextAttribute());
        VALUE_TOKEN.setData(buildTemplateToken(preferenceStore, VALUE).toTextAttribute());
        INTRINSIC_FUNCTION_TOKEN.setData(buildTemplateToken(preferenceStore, INTRINSIC_FUNCTION).toTextAttribute());
        PSEUDO_PARAMETER_TOKEN.setData(buildTemplateToken(preferenceStore, PSEUDO_PARAMETER).toTextAttribute());
        RESOURCE_TYPE_TOKEN.setData(buildTemplateToken(preferenceStore, RESOURCE_TYPE).toTextAttribute());
    }

    public void dispose() {}
}