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
package com.amazonaws.eclipse.explorer;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.navigator.CommonDragAdapterAssistant;
import org.eclipse.ui.part.PluginTransfer;

import com.amazonaws.services.s3.model.S3ObjectSummary;

public class DragAdapterAssistant extends CommonDragAdapterAssistant {

    @Override
    public void dragStart(DragSourceEvent anEvent, IStructuredSelection aSelection) {

        anEvent.doit = aSelection.size() == 1 && aSelection.getFirstElement() instanceof S3ObjectSummary;

        /*
         * We need to make sure that our drag is treated *only* as a plugin
         * transfer, whereas the superclass defaults to treating all such events
         * as either LocalSelectionTransfer or PluginTransfer. In the case of
         * the former, the drag adapter for other views won't recognize the
         * object being dropped and so disallows it.
         */
        DragSource source = ((DragSource) anEvent.getSource());
        source.setTransfer(getSupportedTransferTypes());
        //printEvent(anEvent);
    }

    public DragAdapterAssistant() {
    }

    /*
     * This list is added to the list of defaults, so it's a no-op in its
     * intended context. However, we include it here as a convenience for
     * dragStart()
     */
    @Override
    public Transfer[] getSupportedTransferTypes() {
        return new Transfer[] { PluginTransfer.getInstance(), };
    }

    private void printEvent(DragSourceEvent e) {
        System.out.println("\n\n\nEVENT START\n\n\n");

        StringBuffer sb = new StringBuffer();
        DragSource source = ((DragSource) e.widget);
        sb.append("widget: ");
        sb.append(e.widget);
        sb.append(", source.Transfer: ");
        sb.append(source.getTransfer().length + " elements; ");
        for ( Transfer transfer : source.getTransfer() ) {
            sb.append(transfer).append("; ");
        }
        sb.append(System.identityHashCode(source));
        sb.append(", time: ");
        sb.append(e.time);
        sb.append(", operation: ");
        sb.append(e.detail);
        sb.append(", type: ");
        sb.append(e.dataType != null ? e.dataType.type : 0);
        sb.append(", doit: ");
        sb.append(e.doit);
        sb.append(", data: ");
        sb.append(e.data);
        sb.append(", dataType: ");
        sb.append(e.dataType);
        sb.append("\n");
        System.out.println(sb.toString());
        System.out.println("\n\n\nEVENT END\n\n\n");
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.navigator.CommonDragAdapterAssistant#setDragData(org.eclipse.swt.dnd.DragSourceEvent, org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public boolean setDragData(DragSourceEvent anEvent, IStructuredSelection aSelection) {
        //printEvent(anEvent);
        return true;
    }

}
