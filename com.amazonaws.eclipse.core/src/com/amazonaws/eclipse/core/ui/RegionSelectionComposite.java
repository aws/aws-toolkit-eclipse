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

public class RegionSelectionComposite extends Composite {

    private String serviceName;

    private Combo regionSelectionCombo;

    private List<SelectionListener> listeners = new ArrayList<SelectionListener>();

    public RegionSelectionComposite(final Composite parent, final int style) {
        this(parent, style, null);
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

}
