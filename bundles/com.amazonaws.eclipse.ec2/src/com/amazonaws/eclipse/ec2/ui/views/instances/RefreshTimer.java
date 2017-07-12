/*
 * Copyright 2008-2012 Amazon Technologies, Inc. 
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

package com.amazonaws.eclipse.ec2.ui.views.instances;

import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.ui.IRefreshable;

/**
 * Timer responsible for controlling when a specified control is refreshed.
 */
public class RefreshTimer implements Runnable {
    /** The default period (in milliseconds) between refreshes */
    public static final int DEFAULT_TIMER_PERIOD = 60 * 1000;
    
    /** The control that this timer is responsible for refreshing */
    private final IRefreshable control;

    /** The period (in milliseconds) between refreshes */
    private int refreshPeriodInMilliseconds;

    /**
     * Creates a new RefreshTimer ready to refresh the specified control
     * with the default period. Note that once a RefreshTimer has been
     * created, it needs to be explicitly started before it will start
     * refreshing the specified control.
     * 
     * @param control
     *            The control this timer is responsible for refreshing.
     */
    public RefreshTimer(IRefreshable control) {
        this(control, DEFAULT_TIMER_PERIOD);
    }

    /**
     * Creates a new RefreshTimer ready to refresh the specified control
     * with the specified period. Note that a RefreshTimer has been
     * created, it needs to be explicitly started before it will start
     * refreshing the specified control.
     * 
     * @param control
     *            The control this timer is responsible for refreshing.
     * @param refreshPeriodInMilliseconds
     *            The period between refreshes, in milliseconds.
     */
    public RefreshTimer(IRefreshable control, int refreshPeriodInMilliseconds) {
        this.control = control;
        this.refreshPeriodInMilliseconds = refreshPeriodInMilliseconds;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        control.refreshData();
        
        startTimer();
    }

    /**
     * Sets the period, in milliseconds, between refreshes.
     * 
     * @param refreshPeriodInMilliseconds
     *            The period, in milliseconds, between refreshes.
     */
    public void setRefreshPeriod(int refreshPeriodInMilliseconds) {
        this.refreshPeriodInMilliseconds = refreshPeriodInMilliseconds;
        
        startTimer();
    }
    
    /**
     * Starts this refresh timer.
     */
    public void startTimer() {
        Display.getDefault().timerExec(refreshPeriodInMilliseconds, this);
    }
    
    /**
     * Stops this refresh timer.
     */
    public void stopTimer() {
        Display.getDefault().timerExec(-1, this);
    }
    
}
