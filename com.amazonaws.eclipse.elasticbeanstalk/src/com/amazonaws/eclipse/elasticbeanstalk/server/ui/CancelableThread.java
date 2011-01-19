/*
 * Copyright 2010-2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui;

public abstract class CancelableThread extends Thread {
    protected boolean running = true;
    protected boolean canceled = false;

    public synchronized final boolean isRunning() {
        return running;
    }

    public synchronized final void setRunning(boolean running) {
        this.running = running;
    }

    public synchronized final boolean isCanceled() {
        return canceled;
    }

    public synchronized final void cancel() {
        this.canceled = true;
    }
}
