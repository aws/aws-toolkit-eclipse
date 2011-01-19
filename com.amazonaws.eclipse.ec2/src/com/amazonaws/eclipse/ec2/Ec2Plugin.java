/*
 * Copyright 2008-2011 Amazon Technologies, Inc. 
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

package com.amazonaws.eclipse.ec2;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The entry point to the EC2 plugin. 
 */
public class Ec2Plugin extends AbstractUIPlugin {

	/** The singleton instance of this plugin */
	private static Ec2Plugin plugin;

	/** The ID of this plugin */
	public static final String PLUGIN_ID = "com.amazonaws.eclipse.ec2";

	/** The id of the AWS Toolkit region preference page */
	public static final String REGION_PREFERENCE_PAGE_ID = "com.amazonaws.eclipse.ec2.preferences.RegionsPreferencePage";

	
	/**
	 * Returns the singleton instance of this plugin.
	 * 
	 * @return The singleton instance of this plugin.
	 */
	public static Ec2Plugin getDefault() {
		return plugin;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		/*
		 * TODO: We could look for a system property specifying where a log file
		 *       to write messages to. That might be useful for debugging issues
		 *       for users.
		 */
		
		Logger rootLogger = Logger.getLogger("");
		for (Handler handler : rootLogger.getHandlers()) {
			rootLogger.removeHandler(handler);
		}
		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.FINE);
		rootLogger.addHandler(consoleHandler);
		
		Logger amazonLogger = Logger.getLogger("com.amazonaws.eclipse");
		amazonLogger.setLevel(Level.ALL);
		for (Handler handler : amazonLogger.getHandlers()) {
			amazonLogger.removeHandler(handler);
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#createImageRegistry()
	 */
	@Override
	protected ImageRegistry createImageRegistry() {
		// TODO: make sure we're using all of these...
		String[] images = new String[] {"add",       			"/icons/add2.png",
		                                "bundle",    			"/icons/package.png",
		                                "clipboard", 			"/icons/clipboard.png",
		                                "configure", 			"/icons/gear_add.png",
		                                "check",     			"/icons/check2.png",
		                                "console",   			"/icons/console.png",
		                                "error",     			"/icons/error.png",
		                                "info",          		"/icons/info.gif",
		                                "launch",    			"/icons/server_into.png",
		                                "reboot",    			"/icons/replace2.png",
		                                "refresh",   			"/icons/refresh.gif",
		                                "remove",    			"/icons/delete2.png",
		                                "snapshot",  			"/icons/camera.png",
		                                "terminate",   			"/icons/media_stop_red.png",
                                        "stop",                 "/icons/media_pause.png",
                                        "start",                "/icons/media_play_green.png",
		                                
		                                "filter",				"/icons/filter.gif",
		                                
		                                "status-running",		"/icons/green-circle.png",
		                                "status-rebooting",		"/icons/blue-circle.png",
		                                "status-terminated",	"/icons/red-circle.png",
		                                "status-waiting",		"/icons/yellow-circle.png",
		                                
		                                "server",               "/icons/server.png",
                                        "volume",               "/icons/harddisk.png",
                                        "shield",               "/icons/shield1.png",
                                        "ami",                  "/icons/ami_icon.png",
		};
		
		int i = 0;
		ImageRegistry imageRegistry = new ImageRegistry(Display.getCurrent());
		while (i < images.length - 1) {
			String id = images[i++];
			String imagePath = images[i++];
			imageRegistry.put(id, ImageDescriptor.createFromFile(getClass(), imagePath));
		}

		return imageRegistry;
	}

}
