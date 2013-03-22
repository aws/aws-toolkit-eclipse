package com.amazonaws.eclipse.dynamodb;

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class DynamoDBPlugin extends AbstractUIPlugin {

    public static final String IMAGE_ONE = "1";
    public static final String IMAGE_A = "a";
    public static final String IMAGE_TABLE = "table";
    public static final String IMAGE_NEXT_RESULTS = "next_results";
    
	// The plug-in ID
	public static final String PLUGIN_ID = "com.amazonaws.eclipse.dynamodb"; //$NON-NLS-1$

	// The shared instance
	private static DynamoDBPlugin plugin;
	
	/**
	 * The constructor
	 */
	public DynamoDBPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static DynamoDBPlugin getDefault() {
		return plugin;
	}

    /* (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#createImageRegistry()
     */
    @Override
    protected ImageRegistry createImageRegistry() {
        String[] images = new String[] {
                IMAGE_ONE,   "/icons/1.png",
                IMAGE_A,     "/icons/a.png",
                IMAGE_TABLE, "/icons/table.png",
                IMAGE_NEXT_RESULTS, "/icons/next_results.png",
        };

        ImageRegistry imageRegistry = super.createImageRegistry();
        Iterator<String> i = Arrays.asList(images).iterator();
        while (i.hasNext()) {
            String id = i.next();
            String imagePath = i.next();
            imageRegistry.put(id, ImageDescriptor.createFromFile(getClass(), imagePath));
        }

        return imageRegistry;    }
	
	

}
