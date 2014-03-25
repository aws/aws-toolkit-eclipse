package com.amazonaws.eclipse.cloudformation.templates.editor.hyperlink;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

import com.amazonaws.eclipse.cloudformation.templates.TemplateNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateObjectNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateValueNode;
import com.amazonaws.eclipse.cloudformation.templates.editor.DocumentUtils;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor.TemplateDocument;

public class TemplateHyperlinkDetector implements IHyperlinkDetector {

    TemplateEditor editor = null;

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

            char previousChar = DocumentUtils.readToPreviousChar(document, quoteStart);
            if (':' != previousChar) {
                return null;
            }

            // Make sure the previous node is a "Ref"
            TemplateNode prevNode = document.findNode(quoteStart - 1);
            if (!(prevNode instanceof TemplateObjectNode) || null == TemplateObjectNode.class.cast(prevNode).get("Ref")) {
                return null;
            }
        } catch (BadLocationException e) {
            System.err.println("Error: Exception while processing possible hyperlink value: " + e.getMessage());
        }

        // Adjust the region end points to omit the quotes around the value
        quoteStart++;
        quoteEnd--;

        Region linkRegion = new Region(quoteStart, quoteEnd - quoteStart + 1);
        String linkText = ((TemplateValueNode) node).getText();
        TemplateHyperlink link = new TemplateHyperlink(linkRegion, document, linkText, editor);

        return new IHyperlink[] { link };
    }
}
