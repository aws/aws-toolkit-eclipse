package com.amazonaws.eclipse.cloudformation.templates.editor.hyperlink;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.amazonaws.eclipse.cloudformation.templates.TemplateNode;
import com.amazonaws.eclipse.cloudformation.templates.editor.DocumentUtils;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor.TemplateDocument;

public class TemplateHyperlink implements IHyperlink {

    IRegion region = null;
    TemplateDocument document = null;
    TemplateEditor editor = null;
    String hyperlinkText = null;

    public TemplateHyperlink(IRegion region, TemplateDocument document, String hyperlinkText, TemplateEditor editor) {
        this.region = region;
        this.document = document;
        this.editor = editor;
        this.hyperlinkText = hyperlinkText;
    }

    @Override
    public IRegion getHyperlinkRegion() {
        return region;
    }

    @Override
    public String getTypeLabel() {
        return null;
    }

    @Override
    public String getHyperlinkText() {
        return hyperlinkText;
    }

    @Override
	public void open() {
	    TemplateNode node = document.findNamedNode(hyperlinkText);
    	DocumentUtils.highlightNode(node);
	}
}
