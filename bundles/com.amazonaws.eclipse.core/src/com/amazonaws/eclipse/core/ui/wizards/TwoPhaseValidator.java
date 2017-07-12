/*
 * Copyright 2008-2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.ui.wizards;

import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.map.WritableMap;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

/**
 * An Eclipse MultiValidator that runs a two-phase validation process
 * using pluggable InputValidator strategies. First, an optional synchronous
 * validation phase is run to check for well-formed input. If this phase
 * passes, an optional asynchronous phase is run to eg make a call to a
 * remote service to check an existence constraint.
 * <p/>
 * Both phases are optional, although a TwoPhaseValidator with a no-op for
 * both phases is a particularly inefficient way of doing nothing.
 */
class TwoPhaseValidator extends MultiValidator {

    /**
     * How long to wait before actually running async validation, to cut
     * down on churn when the user is actively changing the input.
     */
    private static final long ASYNC_DELAY_MILLIS = 200;

    private final IObservableValue observableInput;
    private final InputValidator syncValidator;
    private final InputValidator asyncValidator;

    /**
     * A cache of values we've previously validated asynchronously; if we
     * see these values again, we'll be lazy and simply report the cached
     * value rather than re-running (potentially-expensive) async validation.
     * <p/>
     * The cache is not size-bound, since it's <i>probably</i> not an issue
     * in practice, and WritableMap doesn't expose an easy way to expire
     * entries from the cache.
     * <p/>
     * Access is protected by a lock on the TwoPhaseValidator.
     */
    private final IObservableMap asyncCache;

    /**
     * The currently-scheduled async validation job (or null if no job
     * is currently scheduled). We remember this so we can cancel the
     * job if the input changes before the job has actually started
     * running.
     * <p/>
     * Access is protected by a lock on the TwoPhaseValidator.
     */
    private Job asyncValidationJob;

    /**
     * Constructor.
     *
     * @param observableInput the value to validate
     * @param syncValidator the optional synchronous validator
     * @param asyncValidator the optional asynchronous validator
     */
    public TwoPhaseValidator(
        final IObservableValue observableInput,
        final InputValidator syncValidator,
        final InputValidator asyncValidator
    ) {
        this.observableInput = observableInput;
        this.syncValidator = syncValidator;
        this.asyncValidator = asyncValidator;

        if (asyncValidator == null) {
            asyncCache = null;
        } else {
            asyncCache = new WritableMap();

            // Observe the cache; the background validation job will write
            // it's status to the cache. If the user hasn't changed the input
            // value since we started the async validation, we'll find the
            // result in the cache when revalidating and update the UI
            // as appropriate.

            super.observeValidatedMap(asyncCache);
        }
    }

    /**
     * Validate the current input value.
     *
     * @return an OK status if the value is valid, an error otherwise
     */
    @Override
    protected IStatus validate() {
        Object input = observableInput.getValue();

        if (syncValidator != null) {
            // Run synchronous validation synchronously
            IStatus rval = syncValidator.validate(input);
            if (!rval.isOK()) {
                return rval;
            }
        }

        if (asyncValidator == null) {
            // Nothing else to do, just report OK.
            return ValidationStatus.ok();
        }

        synchronized (this) {
            // If there is a pending async validation job, cancel it; the
            // value has changed, so it is no longer relevant.
            if (asyncValidationJob != null) {
                asyncValidationJob.cancel();
                asyncValidationJob = null;
            }

            // Check for a cached validation of the current value; if there
            // is one, we can go ahead and return that rather than kicking
            // off a background validation.

            IStatus cachedStatus = (IStatus) asyncCache.get(input);
            if (cachedStatus != null) {
                return cachedStatus;
            }

            // No cached validation status; schedule an async validation job
            // to run if the value stays stable for a little while. In the
            // meantime, report that we're still in the middle of validating.

            asyncValidationJob = new AsyncValidationJob(input);
            asyncValidationJob.schedule(ASYNC_DELAY_MILLIS);

            return ValidationStatus.error("Validating...");
        }
    }

    /**
     * A background job that runs our asyncValidator with a given input
     * and calls back to the UI thread when it's finished to report whether
     * the input was valid or not
     */
    private class AsyncValidationJob extends Job {

        private final Object input;

        /**
         * Constructor input the input to validate
         */
        public AsyncValidationJob(final Object input) {
            super("AWS Toolkit Async Validation Job");
            super.setPriority(Job.DECORATE);

            this.input = input;
        }

        /**
         * Run the async validator and call back to the UI thread with its
         * result.
         */
        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            final IStatus rval = asyncValidator.validate(input);

            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    synchronized (TwoPhaseValidator.this) {

                        // If we haven't been canceled and replaced,
                        // deregister us so we can be GC'd.
                        if (asyncValidationJob == AsyncValidationJob.this) {
                            asyncValidationJob = null;
                        }

                        // Drop the status into the cache. The
                        // TwoPhaseValidator is watching the cache, and so
                        // validation will re-run, picking up the cached
                        // value this time.

                        asyncCache.put(input, rval);
                    }
                }
            });

            return Status.OK_STATUS;
        }
    }
}
