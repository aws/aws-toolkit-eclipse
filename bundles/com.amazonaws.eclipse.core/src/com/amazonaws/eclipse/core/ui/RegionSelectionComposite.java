package com.amazonaws.eclipse.core.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;

/**
 * @deprecated for {@link RegionComposite}
 */
@Deprecated
public class RegionSelectionComposite extends Composite {

    private String serviceName;

    private Combo regionSelectionCombo;

    private List<SelectionListener> listeners = new ArrayList<>();

    public RegionSelectionComposite(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public void addSelectionListener(SelectionListener listener) {
        this.listeners.add(listener);
    }

    public RegionSelectionComposite(final Composite parent, final int style, final String serviceName) {
        super(parent, style);

        this.serviceName = serviceName;
        this.setLayout(new GridLayout(3, false));
        createRegionSelectionCombo();
    }

    protected void createRegionSelectionCombo() {
        Label selectAccount = new Label(this, SWT.None);
        selectAccount.setText("Select Region:"); //$NON-NLS-1$

        regionSelectionCombo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);

        List<Region> regions = serviceName == null ? RegionUtils.getRegions()
                : RegionUtils.getRegionsForService(serviceName);

        for (Region region : regions) {
            regionSelectionCombo.add(region.getName());
            regionSelectionCombo.setData(region.getName(), region);
        }

        regionSelectionCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for ( SelectionListener listener : listeners ) {
                    listener.widgetSelected(e);
                }
            }
        });
    }

    public void setSelectRegion(String regionName) {
        regionSelectionCombo.setText(regionName);
    }

    public String getSelectedRegion() {
        return ((Region)regionSelectionCombo.getData(regionSelectionCombo.getText())).getId();
    }

    public void setSelection(int index) {
        int itemCount = regionSelectionCombo.getItemCount();
        if (index < 0 || index > itemCount - 1) {
            throw new IllegalArgumentException("The index provided is invalid!");
        }
        regionSelectionCombo.select(index);
    }

}
