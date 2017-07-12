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
package com.amazonaws.eclipse.dynamodb.editor;

import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueEditor.*;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;

/**
 * One row in the scan conditions editor.
 */
final class ScanConditionRow extends Composite {

    private static final ComparisonOperator[] COMPARISON_OPERATORS = new ComparisonOperator[] { ComparisonOperator.EQ,
            ComparisonOperator.NE, ComparisonOperator.GT, ComparisonOperator.GE, ComparisonOperator.LT,
            ComparisonOperator.LE, ComparisonOperator.BETWEEN, ComparisonOperator.BEGINS_WITH, ComparisonOperator.IN, 
            ComparisonOperator.CONTAINS, ComparisonOperator.NOT_CONTAINS, ComparisonOperator.NULL,
            ComparisonOperator.NOT_NULL, };

    private static final String[] COMPARISON_OPERATOR_STRINGS = new String[] { "Equals", "Not equals", "Greater than",
            "Greater than or equals", "Less than", "Less than or equals", "Between", "Begins with", "In", "Contains",
            "Does not contain", "Is null", "Is not null", };

    private static final int EQ = 0;

    private String attributeName;
    private AttributeValue comparisonValue = new AttributeValue();
    private int dataType = S;
    private ComparisonOperator comparisonOperator = ComparisonOperator.EQ;

    private final List<Control> conditionallyShownControls = new ArrayList<>();

    private boolean enabled = true;
    private boolean valid = false;

    /*
     * Shared with the parent editor object, so must be carefully synchronized.
     */
    private final Collection<String> knownAttributes;

    /**
     * Meaningful fields we have to track for fancy UI swapping.
     */
    private Text singleValueEditor;
    private Text listValueEditor;
    private Button multiValueEditorButton;
    private Text betweenTextOne;
    private Label betweenTextLabel;
    private Text betweenTextTwo;
    private Button dataTypeButton;
    private Combo dataTypeCombo;

    /**
     * Returns the attribute name for the scan condition.
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Returns the scan condition represented by this row.
     */
    public Condition getScanCondition() {
        Condition condition = new Condition().withComparisonOperator(comparisonOperator);
        switch (dataType) {
        case S: // fall through
        case SS:
            if ( comparisonOperator.equals(ComparisonOperator.BETWEEN) ) {
                // should only be two here
                condition.withAttributeValueList(new AttributeValue().withS(comparisonValue.getSS().get(0)),
                        new AttributeValue().withS(comparisonValue.getSS().get(1)));
            } else if ( comparisonOperator.equals(ComparisonOperator.IN) ) {
                List<AttributeValue> attributeValues = new LinkedList<>();
                for ( String value : comparisonValue.getSS() ) {
                    attributeValues.add(new AttributeValue().withS(value));
                }
                condition.withAttributeValueList(attributeValues);
            } else if ( comparisonOperator.equals(ComparisonOperator.NULL)
                    || comparisonOperator.equals(ComparisonOperator.NOT_NULL) ) {
                // empty attribute value list
            } else {
                condition.withAttributeValueList(comparisonValue);
            }
            break;
        case N: // fall through
        case NS:
            if ( comparisonOperator.equals(ComparisonOperator.BETWEEN) ) {
                // should only be two here
                condition.withAttributeValueList(new AttributeValue().withN(comparisonValue.getNS().get(0)),
                        new AttributeValue().withN(comparisonValue.getNS().get(1)));
            } else if ( comparisonOperator.equals(ComparisonOperator.IN) ) {
                List<AttributeValue> attributeValues = new LinkedList<>();
                for ( String value : comparisonValue.getNS() ) {
                    attributeValues.add(new AttributeValue().withN(value));
                }
                condition.withAttributeValueList(attributeValues);
            } else if ( comparisonOperator.equals(ComparisonOperator.NULL)
                    || comparisonOperator.equals(ComparisonOperator.NOT_NULL) ) {
                // empty attribute value list
            } else {
                condition.withAttributeValueList(comparisonValue);
            }
            break;
        default:
            throw new RuntimeException("Unrecognized data type " + dataType);
        }
    
        return condition;
    }

    public ScanConditionRow(final Composite parent, final Collection<String> knownAttributes) {

        super(parent, SWT.NONE);
        this.knownAttributes = knownAttributes;

        GridLayoutFactory.fillDefaults().numColumns(10).margins(5, 0).applyTo(this);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(this);

        // Red X to remove control
        final Button removeCondition = new Button(this, SWT.PUSH);
        removeCondition.setToolTipText("Remove condition");
        removeCondition.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_REMOVE));
        removeCondition.setText("");
        removeCondition.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                parent.setRedraw(false);
                dispose();
                parent.layout(true);
                parent.setRedraw(true);
            }
        });

        // Check box for enablement of condition
        final Button enabledButton = new Button(this, SWT.CHECK);
        enabledButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                enabled = enabledButton.getSelection();
                Control[] children = getChildren();
                // Skip the first two controls, which are the remove button and
                // the enable checkbox.
                for ( int i = 2; i < children.length; i++ ) {
                    children[i].setEnabled(enabled);
                }
                validate();
            }
        });
        enabledButton.setSelection(true);

        // Attribute name field
        final Label attributeNameLabel = new Label(this, SWT.None);
        attributeNameLabel.setText("Attribute:");

        final Text attributeNameText = new Text(this, SWT.BORDER);
        attributeNameText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        attributeNameText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                attributeName = attributeNameText.getText();
                validate();
            }
        });
        setupAttributeNameContentAssist(attributeNameText);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(attributeNameText);

        // Comparison selection combo
        final Combo comparison = new Combo(this, SWT.READ_ONLY | SWT.DROP_DOWN);
        comparison.setItems(COMPARISON_OPERATOR_STRINGS);
        comparison.select(EQ);
        comparison.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                comparisonOperator = COMPARISON_OPERATORS[comparison.getSelectionIndex()];
                configureComparisonEditorFields();
                validate();
            }

        });

        singleValueEditor = new Text(this, SWT.BORDER);
        singleValueEditor.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                setAttribute(comparisonValue, Arrays.asList(singleValueEditor.getText()),
                        dataTypeCombo.getSelectionIndex() == STRING ? S : N);
            }
        });
        GridDataFactory.fillDefaults().grab(true, false).applyTo(singleValueEditor);
        conditionallyShownControls.add(singleValueEditor);
        
        listValueEditor = new Text(this, SWT.BORDER);
        listValueEditor.setEditable(false);
        listValueEditor.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                invokeMultiValueEditorDialog();
            }
        });
        GridDataFactory.fillDefaults().grab(true, false).applyTo(listValueEditor);
        conditionallyShownControls.add(listValueEditor);
        
        multiValueEditorButton = new Button(this, SWT.None);
        multiValueEditorButton.setText("...");
        multiValueEditorButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                invokeMultiValueEditorDialog(); 
            }
        });
        GridDataFactory.swtDefaults().applyTo(multiValueEditorButton);
        conditionallyShownControls.add(multiValueEditorButton);
        
        betweenTextOne = new Text(this, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(betweenTextOne);
        ModifyListener betweenModifyListener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                setAttribute(comparisonValue, Arrays.asList(betweenTextOne.getText(), betweenTextTwo.getText()),
                        dataTypeCombo.getSelectionIndex() == STRING ? SS : NS);
            }
        };
        betweenTextOne.addModifyListener(betweenModifyListener);
        conditionallyShownControls.add(betweenTextOne);

        betweenTextLabel = new Label(this, SWT.READ_ONLY);
        betweenTextLabel.setText(" and ");
        GridDataFactory.swtDefaults().applyTo(betweenTextLabel);
        conditionallyShownControls.add(betweenTextLabel);
        
        betweenTextTwo = new Text(this, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(betweenTextTwo);
        betweenTextTwo.addModifyListener(betweenModifyListener);
        conditionallyShownControls.add(betweenTextTwo);
        
        dataTypeButton = new Button(this, SWT.None);
        dataTypeButton.setImage(DynamoDBPlugin.getDefault().getImageRegistry().get(DynamoDBPlugin.IMAGE_A));
        GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.TOP).grab(false, true).applyTo(dataTypeButton);

        dataTypeCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
        dataTypeCombo.setItems(DATA_TYPE_ITEMS);
        dataTypeCombo.select(STRING);
        dataTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Collection<String> values = getValuesFromAttribute(comparisonValue);
                switch (dataTypeCombo.getSelectionIndex()) {
                case STRING:
                    if ( values.size() > 1 ) {
                        dataType = SS;
                    } else {
                        dataType = S;
                    }
                    break;
                case NUMBER:
                    if ( values.size() > 1 ) {
                        dataType = NS;
                    } else {
                        dataType = N;
                    }
                    break;
                default:
                    throw new RuntimeException("Unexpected selection index "
                            + dataTypeCombo.getSelectionIndex());
                }
                setAttribute(comparisonValue, values, dataType);
            }
        });
        GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.TOP).grab(false, true).exclude(true).applyTo(dataTypeCombo);
        dataTypeCombo.setVisible(false);        
        
        configureDataTypeControlSwap(dataTypeButton, dataTypeCombo, this);
        conditionallyShownControls.add(dataTypeButton);
        
        configureComparisonEditorFields();
    }
    
    private void invokeMultiValueEditorDialog() {
        MultiValueAttributeEditorDialog multiValueEditorDialog = new MultiValueAttributeEditorDialog(Display
                .getDefault().getActiveShell(), comparisonValue, dataTypeCombo.getSelectionIndex());

        int returnValue = multiValueEditorDialog.open();
        if ( returnValue == 0 || returnValue == 1) { // Save set or single
            setAttribute(comparisonValue, multiValueEditorDialog.getValues(),
                    dataTypeCombo.getSelectionIndex() == STRING ? SS : NS);
            listValueEditor.setText(format(comparisonValue));
        }
    }
    
    /**
     * Configures the editor row based on the current comparison, hiding and
     * showing editor elements as necessary.
     */
    private void configureComparisonEditorFields() {
        clearAttributes(comparisonValue);
        
        setRedraw(false);
        
        betweenTextOne.setText("");
        betweenTextTwo.setText("");
        listValueEditor.setText("");
        singleValueEditor.setText("");

        List<Control> toHide = new LinkedList<>();
        toHide.addAll(conditionallyShownControls);
        toHide.add(dataTypeCombo);
        for ( Control c : toHide ) {
            c.setVisible(false);
            GridDataFactory.createFrom((GridData) c.getLayoutData()).exclude(true).applyTo(c);
        }
        
        Collection<Control> toShow = new LinkedList<>();               
        switch (comparisonOperator) {
        case BEGINS_WITH:
            toShow.add(singleValueEditor);
            dataType = S;
            break;
        case BETWEEN:
            toShow.add(betweenTextOne);
            toShow.add(betweenTextLabel);
            toShow.add(betweenTextTwo);
            toShow.add(dataTypeButton);
            switch (dataType) {
            case N:
            case NS:
                dataType = N;
                break;
            case S:
            case SS:
                dataType = S;
                break;
            }
            break;
        case IN:
            toShow.add(dataTypeButton);
            toShow.add(multiValueEditorButton);
            toShow.add(listValueEditor);
            switch (dataType) {
            case N:
            case NS:
                dataType = NS;
                break;
            case S:
            case SS:
                dataType = SS;
                break;
            }
            break;
        case EQ:
        case GE:
        case GT:
        case LE:
        case LT:
        case NE:
        case CONTAINS:
        case NOT_CONTAINS:
            toShow.add(dataTypeButton);
            toShow.add(singleValueEditor);
            switch (dataType) {
            case N:
            case NS:
                dataType = N;
                break;
            case S:
            case SS:
                dataType = S;
                break;
            }
            break;
        case NOT_NULL:
        case NULL:
            break;
        default:
            throw new RuntimeException("Unknown comparison " + comparisonOperator);
        }
        
        for ( Control c : toShow ) {
            c.setVisible(true);
            GridDataFactory.createFrom((GridData) c.getLayoutData()).exclude(false).applyTo(c);
        }
        
        layout();
        
        setRedraw(true);
    }    
    
    /**
     * Returns whether this scan condition should be included in the scan
     * request sent to DynamoDB
     */
    public boolean shouldExecute() {
        return enabled && valid;
    }

    private void validate() {
        valid = true;
    }

    /**
     * Sets up content assist on the Text control given with the list of valid
     * completions for it. Returns the ContestAssist object.
     */
    private ContentAssistCommandAdapter setupAttributeNameContentAssist(final Text text) {

        // Our simplified proposer doesn't quite work with a vanilla content
        // adapter implementation, so we have to tweak it a bit to get the
        // desired behavior.
        TextContentAdapter controlContentAdapter = new TextContentAdapter() {

            @Override
            public void insertControlContents(Control control, String value, int cursorPosition) {
                text.setText(value);
            }
        };

        ContentAssistCommandAdapter assist = new ContentAssistCommandAdapter(text, controlContentAdapter,
                new StringContentProposalProvider(), null, null, true);
        // the assist adapter turns off auto-complete for normal typing, so turn
        // it back on
        assist.setAutoActivationCharacters(null);
        assist.setAutoActivationDelay(100);

        return assist;
    }

    /**
     * An IContentProposalProvider that deals in a set of strings
     */
    final class StringContentProposalProvider implements IContentProposalProvider {

        @Override
        public IContentProposal[] getProposals(String contents, int position) {
            synchronized (knownAttributes) {
                List<ContentProposal> list = new ArrayList<>();
                String target = contents.trim().toLowerCase();
                for ( String name : knownAttributes ) {
                    if ( name.toLowerCase().startsWith(target) ) {
                        list.add(new ContentProposal(name));
                    }
                }
                return list.toArray(new IContentProposal[list.size()]);
            }
        }
    }
}
