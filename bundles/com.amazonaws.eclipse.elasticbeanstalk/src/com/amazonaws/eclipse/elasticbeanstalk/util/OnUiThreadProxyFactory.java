/*
 * Copyright 2015 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Display;

/**
 * Proxy Factory to create a dynamic proxy that will intercept all method calls and run them on the
 * UI thread of the default {@link Display}
 */
public final class OnUiThreadProxyFactory {

    /**
     * Get dynamic proxy that will run all method calls on the UI thread
     * 
     * @param interfaceClass
     *            Interface to proxy
     * @param interfaceImpl
     *            Implementation to delegate calls to after wrapping them in a runnable to invoke on
     *            the UI thread
     * @return Proxy class
     */
    public static <T> T getProxy(Class<T> interfaceClass, T interfaceImpl) {
        Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[] { interfaceClass },
                new OnUiThreadProxyFactory.OnUiThreadProxyHandler<>(interfaceImpl));
        return interfaceClass.cast(proxy);
    }

    /**
     * Smiple handler to wrap calls in a runnable and submit them to be run on the UI thread of the
     * default display
     * 
     * @param <T>
     *            Type of interface being proxied
     */
    private static final class OnUiThreadProxyHandler<T> implements InvocationHandler {

        private final T interfaceImpl;

        public OnUiThreadProxyHandler(T impl) {
            this.interfaceImpl = impl;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final AtomicReference<Object> toReturnRef = new AtomicReference<>();
            final AtomicReference<Throwable> throwRef = new AtomicReference<>();

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        toReturnRef.set(method.invoke(interfaceImpl, args));
                    } catch (InvocationTargetException e) {
                        throwRef.set(e.getTargetException());
                    } catch (UndeclaredThrowableException e) {
                        throwRef.set(e.getUndeclaredThrowable());
                    } catch (Exception e) {
                        throwRef.set(e);
                    }
                }
            });

            // Throw the exception if any
            if (throwRef.get() != null) {
                throw throwRef.get();
            }

            return toReturnRef.get();
        }

    }
}