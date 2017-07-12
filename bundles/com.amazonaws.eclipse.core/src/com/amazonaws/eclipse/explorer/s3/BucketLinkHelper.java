package com.amazonaws.eclipse.explorer.s3;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.navigator.ILinkHelper;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.s3.model.Bucket;

public class BucketLinkHelper implements ILinkHelper {

    /**
     * Finds the bucket corresponding to the editor input.
     */
    @Override
    public IStructuredSelection findSelection(IEditorInput anInput) {
        if (!( anInput instanceof BucketEditorInput ))
            return null;
        
        Object[] buckets = S3ContentProvider.getInstance().getChildren(S3RootElement.ROOT_ELEMENT);
        Object bucket = null;
        for ( Object o : buckets ) {
            Bucket b = (Bucket) o;
            if ( b.getName().equals(((BucketEditorInput) anInput).getBucketName()) ) {
                bucket = b;
            }
        }

        if ( bucket == null )
            return null;
        
        return new StructuredSelection(bucket);
    }

    /**
     * Activates the relevant editor for the selection given. If an editor for
     * this bucket isn't already open, won't open it.
     */
    @Override
    public void activateEditor(IWorkbenchPage aPage, IStructuredSelection aSelection) {
        Bucket b = (Bucket) aSelection.getFirstElement();

        try {
            for ( IEditorReference ref : aPage.getEditorReferences() ) {
                if ( ref.getEditorInput() instanceof BucketEditorInput ) {
                    if ( b.getName().equals(((BucketEditorInput) ref.getEditorInput()).getBucketName()) ) {
                        aPage.openEditor(ref.getEditorInput(), BucketEditor.ID);
                        return;
                    }
                }
            }

        } catch ( PartInitException e ) {
            AwsToolkitCore.getDefault().logError("Unable to open the Amazon S3 bucket editor: ", e);
        }
    }
}
