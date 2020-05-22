/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.egit.ui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.repository.tree.RepositoryTreeNodeType;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;

import com.amazonaws.eclipse.core.egit.EGitCredentialsProvider;
import com.amazonaws.eclipse.core.egit.RepositorySelection;
import com.amazonaws.eclipse.core.egit.SourceBranchFailureDialog;
import com.amazonaws.eclipse.core.egit.UIText;

@SuppressWarnings("restriction")
public class SourceBranchPage extends WizardPage {

    private RepositorySelection validatedRepoSelection;

    private Ref head;

    private final List<Ref> availableRefs = new ArrayList<>();

    private Label label;

    private String transportError;

    private Button selectB;

    private Button unselectB;

    private CachedCheckboxTreeViewer refsViewer;

    private UserPasswordCredentials credentials;

    private String helpContext = null;

    public SourceBranchPage() {
        super(SourceBranchPage.class.getName());
        setTitle(UIText.SourceBranchPage_title);
        setDescription(UIText.SourceBranchPage_description);
    }

    public List<Ref> getSelectedBranches() {
        Object[] checkedElements = refsViewer.getCheckedElements();
        Ref[] checkedRefs = new Ref[checkedElements.length];
        System.arraycopy(checkedElements, 0, checkedRefs, 0, checkedElements.length);
        return Arrays.asList(checkedRefs);
    }

    public List<Ref> getAvailableBranches() {
        return availableRefs;
    }

    public Ref getHEAD() {
        return head;
    }

    public boolean isSourceRepoEmpty() {
        return availableRefs.isEmpty();
    }

    public boolean isAllSelected() {
        return availableRefs.size() == refsViewer.getCheckedElements().length;
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite panel = new Composite(parent, SWT.NULL);
        final GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        panel.setLayout(layout);

        label = new Label(panel, SWT.NONE);
        label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        FilteredCheckboxTree fTree = new FilteredCheckboxTree(panel, null,
                SWT.NONE, new PatternFilter()) {
            /*
             * Overridden to check page when refreshing is done.
             */
            @Override
            protected WorkbenchJob doCreateRefreshJob() {
                WorkbenchJob refreshJob = super.doCreateRefreshJob();
                refreshJob.addJobChangeListener(new JobChangeAdapter() {
                    @Override
                    public void done(IJobChangeEvent event) {
                        if (event.getResult().isOK()) {
                            getDisplay().asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    checkPage();
                                }
                            });
                        }
                    }
                });
                return refreshJob;
            }
        };
        refsViewer = fTree.getCheckboxTreeViewer();

        ITreeContentProvider provider = new ITreeContentProvider() {

            @Override
            public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
                // nothing
            }

            @Override
            public void dispose() {
                // nothing
            }

            @Override
            public Object[] getElements(Object input) {
                return ((List) input).toArray();
            }

            @Override
            public boolean hasChildren(Object element) {
                return false;
            }

            @Override
            public Object getParent(Object element) {
                return null;
            }

            @Override
            public Object[] getChildren(Object parentElement) {
                return null;
            }
        };
        refsViewer.setContentProvider(provider);
        refsViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                if (((Ref)element).getName().startsWith(Constants.R_HEADS))
                    return ((Ref)element).getName().substring(Constants.R_HEADS.length());
                return ((Ref)element).getName();
            }

            // FIX_WHEN_MIN_IS_20203 In older versions, get icon returns a Icon but in newer
            // it return an icon descriptor. Differentiate using reflection (ew)
            @Override
            public Image getImage(Object element) {
            	final Object icon = RepositoryTreeNodeType.REF.getIcon();
            	Method method = null;
            	try {
            		method = icon.getClass().getMethod("createImage");
            	} catch (NoSuchMethodException e) {
            		if (icon instanceof Image) {
            			return (Image) icon;
            		} else {
            			return null;
            		}
            	}
        		try {
        			Object resolvedIcon = method.invoke(icon);
        			if (resolvedIcon instanceof Image) {
            			return (Image) resolvedIcon;
            		} else {
            			return null;
            		}
        		} catch (Exception e) {
        			return null;
        		}
            }
        });

        refsViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                checkPage();
            }
        });
        final Composite bPanel = new Composite(panel, SWT.NONE);
        bPanel.setLayout(new RowLayout());
        selectB = new Button(bPanel, SWT.PUSH);
        selectB.setText(UIText.SourceBranchPage_selectAll);
        selectB.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                refsViewer.setAllChecked(true);
                checkPage();
            }
        });
        unselectB = new Button(bPanel, SWT.PUSH);
        unselectB.setText(UIText.SourceBranchPage_selectNone);
        unselectB.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                refsViewer.setAllChecked(false);
                checkPage();
            }
        });
        bPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Dialog.applyDialogFont(panel);
        setControl(panel);
        checkPage();
    }

    public void setSelection(RepositorySelection selection){
        revalidate(selection);
    }

    public void setCredentials(UserPasswordCredentials credentials) {
        this.credentials = credentials;
    }

    /**
     * Set the ID for context sensitive help
     *
     * @param id
     *            help context
     */
    public void setHelpContext(String id) {
        helpContext = id;
    }

    @Override
    public void performHelp() {
        PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpContext);
    }

    /**
     * Check internal state for page completion status. This method should be
     * called only when all necessary data from previous form is available.
     */
    private void checkPage() {
        setMessage(null);
        int checkedElementCount = refsViewer.getCheckedElements().length;
        selectB.setEnabled(checkedElementCount != availableRefs.size());
        unselectB.setEnabled(checkedElementCount != 0);
        if (transportError != null) {
            setErrorMessage(transportError);
            setPageComplete(false);
            return;
        }

        if (getSelectedBranches().isEmpty()) {
            setErrorMessage(UIText.SourceBranchPage_errorBranchRequired);
            setPageComplete(false);
            return;
        }
        setErrorMessage(null);
        setPageComplete(true);
    }

    private void checkForEmptyRepo() {
        if (isSourceRepoEmpty()) {
            setErrorMessage(null);
            setMessage(UIText.SourceBranchPage_repoEmpty, IMessageProvider.WARNING);
            setPageComplete(true);
        }
    }

    private void revalidate(final RepositorySelection newRepoSelection) {
        if (newRepoSelection.equals(validatedRepoSelection)) {
            // URI hasn't changed, no need to refill the page with new data
            checkPage();
            return;
        }

        label.setText(NLS.bind(UIText.SourceBranchPage_branchList,
                newRepoSelection.getURI().toString()));
        label.getParent().layout();

        validatedRepoSelection = null;
        transportError = null;
        head = null;
        availableRefs.clear();
        refsViewer.setInput(null);
        setPageComplete(false);
        setErrorMessage(null);
        setMessage(null);
        label.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                revalidateImpl(newRepoSelection);
            }
        });
    }

    private void revalidateImpl(final RepositorySelection newRepoSelection) {
        if (label.isDisposed() || !isCurrentPage())
            return;

        final ListRemoteOperation listRemoteOp;
        final URIish uri = newRepoSelection.getURI();
        try {
            final Repository db = FileRepositoryBuilder
                    .create(new File("/tmp")); //$NON-NLS-1$
            int timeout = Activator.getDefault().getPreferenceStore().getInt(
                    UIPreferences.REMOTE_CONNECTION_TIMEOUT);
            listRemoteOp = new ListRemoteOperation(db, uri, timeout);
            if (credentials != null)
                listRemoteOp
                        .setCredentialsProvider(new EGitCredentialsProvider(
                                credentials.getUser(), credentials
                                        .getPassword()));
            getContainer().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    listRemoteOp.run(monitor);
                }
            });
        } catch (InvocationTargetException e) {
            Throwable why = e.getCause();
            transportError(why);
            if (showDetailedFailureDialog())
                SourceBranchFailureDialog.show(getShell(), uri);
            return;
        } catch (IOException e) {
            transportError(UIText.SourceBranchPage_cannotCreateTemp);
            return;
        } catch (InterruptedException e) {
            transportError(UIText.SourceBranchPage_remoteListingCancelled);
            return;
        }

        final Ref idHEAD = listRemoteOp.getRemoteRef(Constants.HEAD);
        head = null;
        boolean headIsMaster = false;
        final String masterBranchRef = Constants.R_HEADS + Constants.MASTER;
        for (final Ref r : listRemoteOp.getRemoteRefs()) {
            final String n = r.getName();
            if (!n.startsWith(Constants.R_HEADS))
                continue;
            availableRefs.add(r);
            if (idHEAD == null || headIsMaster)
                continue;
            if (r.getObjectId().equals(idHEAD.getObjectId())) {
                headIsMaster = masterBranchRef.equals(r.getName());
                if (head == null || headIsMaster)
                    head = r;
            }
        }
        Collections.sort(availableRefs, new Comparator<Ref>() {
            @Override
            public int compare(final Ref o1, final Ref o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        if (idHEAD != null && head == null) {
            head = idHEAD;
            availableRefs.add(0, idHEAD);
        }

        validatedRepoSelection = newRepoSelection;
        refsViewer.setInput(availableRefs);
        refsViewer.setAllChecked(true);
        checkPage();
        checkForEmptyRepo();
    }

    private void transportError(final Throwable why) {
        Activator.logError(why.getMessage(), why);
        Throwable cause = why.getCause();
        if (why instanceof TransportException && cause != null)
            transportError(NLS.bind(getMessage(why), why.getMessage(),
                    cause.getMessage()));
        else
            transportError(why.getMessage());
    }

    private String getMessage(final Throwable why) {
        if (why.getMessage().endsWith("Auth fail")) //$NON-NLS-1$
            return UIText.SourceBranchPage_AuthFailMessage;
        else
            return UIText.SourceBranchPage_CompositeTransportErrorMessage;
    }

    private void transportError(final String msg) {
            transportError = msg;
            checkPage();
    }

    private boolean showDetailedFailureDialog() {
        return Activator
                .getDefault()
                .getPreferenceStore()
                .getBoolean(UIPreferences.CLONE_WIZARD_SHOW_DETAILED_FAILURE_DIALOG);
    }

}
