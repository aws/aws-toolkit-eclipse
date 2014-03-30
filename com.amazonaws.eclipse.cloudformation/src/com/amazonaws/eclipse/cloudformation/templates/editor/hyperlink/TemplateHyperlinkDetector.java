package com.amazonaws.eclipse.cloudformation.templates.editor.hyperlink;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

import com.amazonaws.eclipse.cloudformation.templates.TemplateFieldNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateValueNode;
import com.amazonaws.eclipse.cloudformation.templates.editor.DocumentUtils;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor.TemplateDocument;

public class TemplateHyperlinkDetector implements IHyperlinkDetector {

    TemplateEditor editor = null;
	private static final Set<String> hyperlinkCandidateKeys;
	
	static {
		Set<String> tempSet = new HashSet<>();
		Collections.addAll(tempSet, "Ref", "DependsOn");
		hyperlinkCandidateKeys = Collections.unmodifiableSet(tempSet);
	}

    public TemplateHyperlinkDetector(TemplateEditor editor) {
        this.editor = editor;
    }
    
    /**
     * Invoked with the editor region beneath the cursor when the user holds the
     * CTRL key.
     */
    @Override
    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
        TemplateDocument document = (TemplateDocument) textViewer.getDocument();
        TemplateNode node = document.findNode(region.getOffset());

        if (node == null || !(node instanceof TemplateValueNode)) {
            return null;
        }

        int quoteStart = (int) node.getStartLocation().getCharOffset();
        int quoteEnd = (int) node.getEndLocation().getCharOffset();

        try {
            if ('"' != document.getChar(quoteStart) || '"' != document.getChar(quoteEnd)) {
                System.err.println("Unexpected: Potential hyperlink value is not quoted");
                return null;
            }

            // Make sure the previous node is a "Ref"
            if (isHyperlinkCandidate(document, quoteStart, node)) {
	            // Adjust the region end points to omit the quotes around the value
	            quoteStart++;
	            quoteEnd--;
	
	            Region linkRegion = new Region(quoteStart, quoteEnd - quoteStart + 1);
	            String linkText = ((TemplateValueNode) node).getText();
	            TemplateHyperlink link = new TemplateHyperlink(linkRegion, document, linkText, editor);
	
	            return new IHyperlink[] { link };
            }
        } catch (BadLocationException e) {
            System.err.println("Error: Exception while processing possible hyperlink value: " + e.getMessage());
        }

        return null;
    }

	private boolean isHyperlinkCandidate(TemplateDocument document, int quoteStart, TemplateNode node) {
		if (node.isValue()) {
			TemplateValueNode valueNode = TemplateValueNode.class.cast(node);
	        char previousChar = DocumentUtils.readToPreviousChar(document, quoteStart);
	        if (':' == previousChar) {
		        TemplateNode parentNode = node.getParent();
		        if (parentNode.isField()) {
		        	TemplateFieldNode fieldNode = TemplateFieldNode.class.cast(parentNode);
	        		if (!isPseudoParameter(valueNode.getText()) && hyperlinkCandidateKeys.contains(fieldNode.getText())) {
	        			return true;
	        		}
		        }
	        }
		}
		return false;
	}

	private boolean isPseudoParameter(String text) {
		return text.contains("AWS::");
	}
}
