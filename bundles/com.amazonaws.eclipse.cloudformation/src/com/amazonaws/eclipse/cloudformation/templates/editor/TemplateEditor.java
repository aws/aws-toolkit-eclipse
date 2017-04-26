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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.amazonaws.eclipse.cloudformation.templates.TemplateArrayNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateObjectNode;

public class TemplateEditor extends TextEditor {

    public interface TemplateDocumentListener {
        public void templateDocumentChanged();
    }

    public class TemplateDocument extends Document {
        private ArrayList<TemplateDocumentListener> listeners = new ArrayList<TemplateDocumentListener>();
        private TemplateNode model;
        /** The path from the root to the current position in the Json Document.*/
        private List<String> path;

        public void addTemplateDocumentListener(TemplateDocumentListener listener) {
            listeners.add(listener);
        }

        public void removeTemplateDocumentListener(TemplateDocumentListener listener) {
            listeners.remove(listener);
        }

        public TemplateNode getModel() {
            return model;
        }

        public void setModel(TemplateNode root) {
            this.model = root;

            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    for (TemplateDocumentListener listener : listeners) {
                        listener.templateDocumentChanged();
                    }
                }
            });
        }

        public TemplateNode findNode(int documentOffset) {
            return findNode(documentOffset, model);
        }

        private TemplateNode findNode(int documentOffset, TemplateNode node) {
            if (node.getStartLocation().getCharOffset() > documentOffset) {
                throw new RuntimeException("Out of bounds in node search");
            }

            if (node.isObject()) {
                TemplateObjectNode object = (TemplateObjectNode)node;
                for (Entry<String, TemplateNode> entry : object.getFields()) {
                    entry.getKey();
                    if (match(documentOffset, entry.getValue())) {
                        return findNode(documentOffset, entry.getValue());
                    }
                }
            } else if (node.isArray()) {
                TemplateArrayNode array = (TemplateArrayNode)node;
                for (TemplateNode member : array.getMembers()) {
                    if (match(documentOffset, member)) {
                        return findNode(documentOffset, member);
                    }
                }
            }
            return node;
        }

        public List<String> getPath() {
            return path;
        }

        public void setPath(List<String> path) {
            this.path = path;
        }
    }

    private boolean match(int offset, TemplateNode node) {
        return offset >= node.getStartLocation().getCharOffset() &&
               offset <=  node.getEndLocation().getCharOffset();
    }

    private final class TemplateDocumentProvider extends FileDocumentProvider {
        @Override
        protected IDocument createEmptyDocument() {
            return new TemplateDocument();
        }
    }

    private IDocumentProvider createDocumentProvider(IEditorInput input) {
        if (input instanceof IFileEditorInput) {
            return new TemplateDocumentProvider();
        } else if (input instanceof FileStoreEditorInput) {
            String message = "Unable to open templates from outside the workspace.  Copy it into a workspace project first.";

            IStatus status = new Status(IStatus.ERROR, CloudFormationPlugin.PLUGIN_ID, message);
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);

            throw new RuntimeException(message);
        } else {
            return new TemplateDocumentProvider();
        }
    }

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {
        setDocumentProvider(createDocumentProvider(input));
        super.doSetInput(input);
    }


    private TemplateContentOutlinePage myOutlinePage;

    public TemplateEditor() {
        setDocumentProvider(new TemplateDocumentProvider());
        setSourceViewerConfiguration(new TemplateSourceViewerConfiguration());
    }

    public TemplateDocument getTemplateDocument() {
        return (TemplateDocument)this.getDocumentProvider().getDocument(getEditorInput());
    }

    public Object getAdapter(Class required) {
        if (IContentOutlinePage.class.equals(required)) {
           if (myOutlinePage == null) {
               TemplateDocument document = (TemplateDocument)this.getDocumentProvider().getDocument(getEditorInput());
               if (document != null) {
                   myOutlinePage = new TemplateContentOutlinePage(document);
               }
           }
           return myOutlinePage;
        }
        return super.getAdapter(required);
     }

}
