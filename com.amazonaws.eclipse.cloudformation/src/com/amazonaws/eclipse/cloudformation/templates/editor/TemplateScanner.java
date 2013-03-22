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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;

import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateSourceViewerConfiguration.TemplateColorProvider;
import com.amazonaws.eclipse.cloudformation.templates.schema.IntrinsicFunction;
import com.amazonaws.eclipse.cloudformation.templates.schema.PseudoParameter;
import com.amazonaws.eclipse.cloudformation.templates.schema.TemplateSchemaRules;

public class TemplateScanner extends RuleBasedScanner {

    private static final TemplateColorProvider COLOR_PROVIDER = new TemplateColorProvider();

    private static final IToken INTRINSIC_FUNCTION_TOKEN = new Token(new TextAttribute(COLOR_PROVIDER.getColor(TemplateColorProvider.PURPLE), null, SWT.BOLD));
    private static final IToken PSEUDO_PARAMETER_TOKEN = new Token(new TextAttribute(COLOR_PROVIDER.getColor(TemplateColorProvider.PURPLE), null, SWT.BOLD));
    private static final IToken RESOURCE_TYPE_TOKEN = new Token(new TextAttribute(COLOR_PROVIDER.getColor(TemplateColorProvider.PURPLE), null, SWT.BOLD));

    private static final IToken KEY_TOKEN = new Token(new TextAttribute(COLOR_PROVIDER.getColor(TemplateColorProvider.GREEN), null, SWT.BOLD));
    private static final IToken VALUE_TOKEN = new Token(new TextAttribute(COLOR_PROVIDER.getColor(TemplateColorProvider.BLUE)));

    private static final class TemplateWordDetector implements IWordDetector {

        private static final Set<Character> SYMBOLS = new HashSet<Character>();

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

        public boolean isWordStart(char c) {
            if (Character.isWhitespace(c)) return false;

            if (SYMBOLS.contains(c)) return false;

            return true;
        }

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

                if (((char)c) == ':') return keyToken;
            }

            return token;
        }
    }

    public TemplateScanner() {
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

    public void dispose() {}
}