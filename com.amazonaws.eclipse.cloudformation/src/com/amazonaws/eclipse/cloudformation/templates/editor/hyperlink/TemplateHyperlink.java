package com.amazonaws.eclipse.cloudformation.templates.editor.hyperlink;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;

import com.amazonaws.eclipse.cloudformation.templates.TemplateNode;
import com.amazonaws.eclipse.cloudformation.templates.editor.DocumentUtils;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor.TemplateDocument;

public class TemplateHyperlink implements IHyperlink {

    private static final String AWS_CLOUD_FORMATION_DOCS_VIEWER_ID = "AWSCloudFormationDocsViewer";
	private static final String GOOGLE_FEELING_LUCKY_SEARCH_URL = "http://www.google.com/search?q=%s&btnI";
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
    	if (isPseudoParameter(hyperlinkText)) {
    		// Use a Google feeling lucky search to open the AWS reference page for the AWS:: type
    		// directly, meaning we don't need to hard-code the AWS documentation URLs for each
    		// particular type, and can even handle new types we haven't seen before.
    		IWebBrowser browser;
			try {
				browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(AWS_CLOUD_FORMATION_DOCS_VIEWER_ID);
	    		URL url = new URL(String.format(GOOGLE_FEELING_LUCKY_SEARCH_URL, hyperlinkText));
	    		browser.openURL(url);
			} catch (PartInitException | MalformedURLException e) {
				e.printStackTrace();
			}
    	} else {
		    TemplateNode node = document.findNamedNode(hyperlinkText);
	    	DocumentUtils.highlightNode(node);
    	}
	}

	private boolean isPseudoParameter(String text) {
		return text.contains("AWS::");
	}
}
