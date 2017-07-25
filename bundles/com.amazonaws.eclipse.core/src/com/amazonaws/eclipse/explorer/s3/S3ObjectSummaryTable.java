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
package com.amazonaws.eclipse.explorer.s3;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILazyTreePathContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PluginTransfer;
import org.eclipse.ui.part.PluginTransferData;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.explorer.s3.actions.DeleteObjectAction;
import com.amazonaws.eclipse.explorer.s3.actions.EditObjectPermissionsAction;
import com.amazonaws.eclipse.explorer.s3.actions.EditObjectTagsAction;
import com.amazonaws.eclipse.explorer.s3.actions.GeneratePresignedUrlAction;
import com.amazonaws.eclipse.explorer.s3.dnd.S3ObjectSummaryDropAction;
import com.amazonaws.eclipse.explorer.s3.dnd.UploadDropAssistant;
import com.amazonaws.eclipse.explorer.s3.dnd.UploadFilesJob;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;

/**
 * S3 object listing with virtual directory support.
 */
public class S3ObjectSummaryTable extends Composite {

    private final static Object LOADING = new Object();
    private final static Object LOADING_DONE = new Object();

    private final static String DEFAULT_DELIMITER = "/";

    private static final int KEY_COL = 0;
    private static final int ETAG_COL = 1;
    private static final int OWNER_COL = 2;
    private static final int SIZE_COL = 3;
    private static final int STORAGE_CLASS_COL = 4;
    private static final int LAST_MODIFIED_COL = 5;

    private final String bucketName;
    private final String accountId;
    private final String regionId;

    private final Map<TreePath, Object[]> children;
    private final TreeViewer viewer;

    private final Map<ImageDescriptor, Image> imageCache = new HashMap<>();

    private final class S3ObjectSummaryContentProvider implements // ITreePathContentProvider,
            ILazyTreePathContentProvider {

        private TreeViewer viewer;

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            this.viewer = (TreeViewer) viewer;
        }

        @Override
        public void dispose() {
            for ( Image img : imageCache.values() ) {
                img.dispose();
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.jface.viewers.ITreePathContentProvider#getParents(java
         * .lang.Object)
         */
        @Override
        public TreePath[] getParents(Object element) {
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.jface.viewers.ILazyTreePathContentProvider#updateElement
         * (org.eclipse.jface.viewers.TreePath, int)
         */
        @Override
        public void updateElement(TreePath parentPath, int index) {
            cacheChildren(parentPath);

            Object[] childNodes = children.get(parentPath);
            if ( index >= childNodes.length )
                return;

            viewer.replace(parentPath, index, childNodes[index]);
            updateHasChildren(parentPath.createChildPath(childNodes[index]));
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.jface.viewers.ILazyTreePathContentProvider#updateChildCount
         * (org.eclipse.jface.viewers.TreePath, int)
         */
        @Override
        public void updateChildCount(TreePath treePath, int currentChildCount) {
            cacheChildren(treePath);
            Object[] objects = children.get(treePath);

            viewer.setChildCount(treePath, objects.length);
            for ( int i = 0; i < objects.length; i++ ) {
                if ( objects[i] instanceof IPath ) {
                    viewer.setHasChildren(treePath.createChildPath(objects[i]), true);
                }
            }

            return;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.jface.viewers.ILazyTreePathContentProvider#updateHasChildren
         * (org.eclipse.jface.viewers.TreePath)
         */
        @Override
        public void updateHasChildren(TreePath path) {
            viewer.setHasChildren(path, path.getSegmentCount() > 0 && path.getLastSegment() instanceof IPath);
        }

        /*
         * Non-virtual tree content provider implementation below
         */

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.jface.viewers.ITreePathContentProvider#getElements(java
         * .lang.Object)
         */
        @SuppressWarnings("unused")
        public Object[] getElements(Object inputElement) {
            TreePath treePath = new TreePath(new Object[0]);
            cacheChildren(treePath);
            return children.get(treePath);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.jface.viewers.ITreePathContentProvider#getChildren(org
         * .eclipse.jface.viewers.TreePath)
         */
        @SuppressWarnings("unused")
        public Object[] getChildren(TreePath parentPath) {
            cacheChildren(parentPath);

            return children.get(parentPath);
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.jface.viewers.ITreePathContentProvider#hasChildren(org
         * .eclipse.jface.viewers.TreePath)
         */
        @SuppressWarnings("unused")
        public boolean hasChildren(TreePath path) {
            return path.getLastSegment() instanceof IPath;
        }
    }

    private final class S3ObjectSummaryLabelProvider implements ITableLabelProvider {

        @Override
        public void removeListener(ILabelProviderListener listener) {
        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return true;
        }

        @Override
        public void dispose() {
        }

        @Override
        public void addListener(ILabelProviderListener listener) {
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if ( element == LOADING ) {
                return "Loading...";
            } else if ( element instanceof IPath ) {
                if ( columnIndex == 0 )
                    return ((IPath) element).lastSegment();
                else
                    return "";
            }

            S3ObjectSummary s = (S3ObjectSummary) element;
            switch (columnIndex) {
            case KEY_COL:
                int index = s.getKey().lastIndexOf(DEFAULT_DELIMITER);
                if ( index > 0 ) {
                    return s.getKey().substring(index + 1);
                } else {
                    return s.getKey();
                }
            case ETAG_COL:
                return s.getETag();
            case OWNER_COL:
                return s.getOwner().getDisplayName();
            case SIZE_COL:
                return "" + s.getSize();
            case STORAGE_CLASS_COL:
                return s.getStorageClass();
            case LAST_MODIFIED_COL:
                return s.getLastModified().toString();
            }
            return "";
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            if ( columnIndex == 0 && element != LOADING ) {
                if ( element instanceof IPath ) {
                    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
                }
                S3ObjectSummary s = (S3ObjectSummary) element;
                return getCachedImage(PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(s.getKey()));
            }
            return null;
        }

        public Image getCachedImage(ImageDescriptor img) {
            if ( !imageCache.containsKey(img) ) {
                imageCache.put(img, img.createImage());
            }
            return imageCache.get(img);
        }
    }

    public S3ObjectSummaryTable(String accountId, String bucketName, String s3Endpoint, Composite composite, FormToolkit toolkit, int style) {
        super(composite, style);

        this.accountId = accountId;
        this.regionId = RegionUtils.getRegionByEndpoint(s3Endpoint).getId();
        this.bucketName = bucketName;
        this.children = Collections.synchronizedMap(new HashMap<TreePath, Object[]>());

        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        setLayout(gridLayout);

        Composite sectionComp = toolkit.createComposite(this, SWT.None);
        sectionComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        sectionComp.setLayout(new GridLayout(1, false));

        Composite headingComp = toolkit.createComposite(sectionComp, SWT.None);
        headingComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        headingComp.setLayout(new GridLayout());
        Label label = toolkit.createLabel(headingComp, "Object listing: drag and drop local files to the table view to upload to S3 bucket");
        label.setFont(JFaceResources.getHeaderFont());
        label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

        Composite tableHolder = toolkit.createComposite(sectionComp, SWT.None);
        tableHolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        FillLayout layout = new FillLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 10;
        layout.type = SWT.VERTICAL;
        tableHolder.setLayout(layout);

        Composite tableComp = toolkit.createComposite(tableHolder, SWT.None);
        TreeColumnLayout tableColumnLayout = new TreeColumnLayout();
        tableComp.setLayout(tableColumnLayout);

        viewer = new TreeViewer(tableComp, SWT.BORDER | SWT.VIRTUAL | SWT.MULTI);
        viewer.getTree().setLinesVisible(true);
        viewer.getTree().setHeaderVisible(true);
        viewer.setUseHashlookup(true);
        viewer.setLabelProvider(new S3ObjectSummaryLabelProvider());
        viewer.setContentProvider(new S3ObjectSummaryContentProvider());

        Tree tree = viewer.getTree();

        createColumns(tableColumnLayout, tree);

        initializeDragAndDrop();

        final TreePath rootPath = new TreePath(new Object[0]);
        final Thread t = cacheChildren(rootPath);

        viewer.setInput(LOADING);

        new Thread() {

            @Override
            public void run() {
                try {
                    t.join();
                } catch ( InterruptedException e1 ) {
                    return;
                }

                Display.getDefault().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        // Preserve the current column widths
                        int[] colWidth = new int[viewer.getTree().getColumns().length];
                        int i = 0;
                        for ( TreeColumn col : viewer.getTree().getColumns() ) {
                            colWidth[i++] = col.getWidth();
                        }

                        viewer.setInput(LOADING_DONE);

                        i = 0;
                        for ( TreeColumn col : viewer.getTree().getColumns() ) {
                            col.setWidth(colWidth[i++]);
                        }
                    }
                });
            }

        }.start();

        hookContextMenu();
    }

    protected void initializeDragAndDrop() {
        DragSource dragSource = new DragSource(viewer.getTree(), DND.DROP_COPY | DND.DROP_DEFAULT | DND.DROP_MOVE);
        dragSource.setTransfer(new Transfer[] { PluginTransfer.getInstance() });
        dragSource.addDragListener(new DragSourceAdapter() {

            @Override
            public void dragStart(DragSourceEvent event) {
                event.doit = false;
                ISelection selection = viewer.getSelection();
                if ( selection instanceof IStructuredSelection ) {
                    if ( ((IStructuredSelection) selection).size() == 1
                            && ((IStructuredSelection) selection).getFirstElement() instanceof S3ObjectSummary ) {
                        event.doit = true;
                    }
                }
            }

            @Override
            public void dragSetData(DragSourceEvent event) {
                Object o = ((ITreeSelection) viewer.getSelection()).getFirstElement();
                if ( o instanceof S3ObjectSummary ) {
                    S3ObjectSummary s = (S3ObjectSummary) o;
                    event.data = new PluginTransferData(S3ObjectSummaryDropAction.ID, S3ObjectSummaryDropAction
                            .encode(s));
                } else {
                    event.doit = false;
                }
            }

        });

        DropTarget dropTarget =  new DropTarget(viewer.getTree(), DND.DROP_COPY | DND.DROP_DEFAULT | DND.DROP_MOVE);
        dropTarget.setTransfer(new Transfer[] { LocalSelectionTransfer.getTransfer(), FileTransfer.getInstance() });
        dropTarget.addDropListener(new DropTargetAdapter() {
            @Override
            public void drop(DropTargetEvent event) {
                File[] files = UploadDropAssistant.getFileToDrop(event.currentDataType);

                if (files == null || files.length == 0) {
                    return;
                }

                String prefix = "";
                if ( event.item instanceof TreeItem ) {
                    TreeItem item = (TreeItem) event.item;
                    if ( item.getData() instanceof IPath ) {
                        IPath path = (IPath) item.getData();
                        prefix = path.toString();
                    }
                }

                final TransferManager transferManager = new TransferManager(getS3Client());

                UploadFilesJob uploadFileJob = new UploadFilesJob(String.format("Upload files to bucket %s", bucketName),
                        bucketName, files, transferManager);
                uploadFileJob.setRefreshRunnable(new Runnable() {
                    @Override
                    public void run() {
                        refresh(null);
                    }
                });
                uploadFileJob.schedule();
            }
        });
    }

    public synchronized AmazonS3 getS3Client() {
        return AwsToolkitCore.getClientFactory(accountId).getS3ClientByRegion(regionId);
    }

    protected void createColumns(TreeColumnLayout tableColumnLayout, Tree tree) {

        TreeColumn column = new TreeColumn(tree, SWT.NONE);
        column.setText("Key");
        ColumnWeightData cwd_column = new ColumnWeightData(30);
        cwd_column.minimumWidth = 1;
        tableColumnLayout.setColumnData(column, cwd_column);

        TreeColumn column_1 = new TreeColumn(tree, SWT.NONE);
        column_1.setMoveable(true);
        column_1.setText("E-tag");
        tableColumnLayout.setColumnData(column_1, new ColumnWeightData(15));

        TreeColumn column_2 = new TreeColumn(tree, SWT.NONE);
        column_2.setMoveable(true);
        column_2.setText("Owner");
        tableColumnLayout.setColumnData(column_2, new ColumnWeightData(15));

        TreeColumn column_3 = new TreeColumn(tree, SWT.NONE);
        column_3.setMoveable(true);
        column_3.setText("Size");
        tableColumnLayout.setColumnData(column_3, new ColumnWeightData(15));

        TreeColumn column_4 = new TreeColumn(tree, SWT.NONE);
        column_4.setMoveable(true);
        column_4.setText("Storage class");
        tableColumnLayout.setColumnData(column_4, new ColumnWeightData(10));

        TreeColumn column_5 = new TreeColumn(tree, SWT.NONE);
        column_5.setMoveable(true);
        column_5.setText("Last modified");
        tableColumnLayout.setColumnData(column_5, new ColumnWeightData(15));
    }

    /**
     * Fills in the children for the tree path given, which must either be empty
     * or end in a File object.
     */
    protected Thread cacheChildren(final TreePath treePath) {
        if ( children.containsKey(treePath) )
            return null;

        children.put(treePath, new Object[] { LOADING });
        Thread thread = new Thread() {

            @Override
            public void run() {
                String prefix;
                if ( treePath.getSegmentCount() == 0 ) {
                    prefix = "";
                } else {
                    prefix = ((IPath) treePath.getLastSegment()).toString();
                }

                List<S3ObjectSummary> filteredObjectSummaries = new LinkedList<>();
                List<String> filteredCommonPrefixes = new LinkedList<>();
                ObjectListing listObjectsResponse = null;

                AmazonS3 s3 = getS3Client();
                do {
                    if (listObjectsResponse == null) {
                        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                            .withBucketName(S3ObjectSummaryTable.this.bucketName)
                            .withDelimiter(DEFAULT_DELIMITER)
                            .withPrefix(prefix);
                        listObjectsResponse = s3.listObjects(listObjectsRequest);
                    } else {
                        listObjectsResponse = s3.listNextBatchOfObjects(listObjectsResponse);
                    }

                    for ( S3ObjectSummary s : listObjectsResponse.getObjectSummaries() ) {
                        if ( !s.getKey().equals(prefix) ) {
                            filteredObjectSummaries.add(s);
                        }
                    }

                    filteredCommonPrefixes.addAll(listObjectsResponse.getCommonPrefixes());
                } while (listObjectsResponse.isTruncated());


                final Object[] objects = new Object[filteredObjectSummaries.size() + filteredCommonPrefixes.size()];
                filteredObjectSummaries.toArray(objects);

                int i = filteredObjectSummaries.size();
                for ( String commonPrefix : filteredCommonPrefixes ) {
                    objects[i++] = new Path(null, commonPrefix);
                }

                children.put(treePath, objects);

                viewer.getTree().getDisplay().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        if ( treePath.getSegmentCount() == 0 )
                            viewer.setChildCount(treePath, objects.length);
                        viewer.refresh();
                    }
                });
            }

        };

        thread.start();
        return thread;
    }

    /**
     * Hooks a context menu for the table control.
     */
    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            @Override
            public void menuAboutToShow(IMenuManager manager) {
                manager.add(new DeleteObjectAction(S3ObjectSummaryTable.this));
                manager.add(new Separator());
                manager.add(new EditObjectPermissionsAction(S3ObjectSummaryTable.this));
                manager.add(new EditObjectTagsAction(S3ObjectSummaryTable.this));
                manager.add(new Separator());
                manager.add(new GeneratePresignedUrlAction(S3ObjectSummaryTable.this));
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);

        menuMgr.createContextMenu(this);
    }

    /**
     * Returns all selected S3 summaries in the table.
     */
    public Collection<S3ObjectSummary> getSelectedObjects() {
        IStructuredSelection s = (IStructuredSelection) viewer.getSelection();
        List<S3ObjectSummary> summaries = new LinkedList<>();
        Iterator<?> iter = s.iterator();
        while ( iter.hasNext() ) {
            Object next = iter.next();
            if ( next instanceof S3ObjectSummary )
                summaries.add((S3ObjectSummary) next);
        }
        return summaries;
    }

    /**
     * Refreshes the table, optionally at at given root.
     */
    public void refresh(String prefix) {
        if ( prefix == null ) {
            children.clear();
            viewer.refresh();
        } else {
            List<IPath> paths = new LinkedList<>();
            IPath p = new Path("");
            for ( String dir : prefix.split("/") ) {
                p = p.append(dir + "/");
                paths.add(p);
            }
            TreePath treePath = new TreePath(paths.toArray());
            children.remove(treePath);
            viewer.refresh(new Path(prefix));
        }
    }
}
