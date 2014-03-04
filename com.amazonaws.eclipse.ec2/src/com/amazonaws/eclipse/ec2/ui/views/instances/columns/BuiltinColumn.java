package com.amazonaws.eclipse.ec2.ui.views.instances.columns;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.TagFormatter;
import com.amazonaws.eclipse.ec2.keypairs.KeyPairManager;
import com.amazonaws.eclipse.ec2.ui.views.instances.InstancesViewInput;
import com.amazonaws.services.ec2.model.Instance;

public class BuiltinColumn extends TableColumn {
	private int column;
	private final DateFormat dateFormat;
	private InstancesViewInput viewInput;
	private KeyPairManager keyPairManager = new KeyPairManager();

	public BuiltinColumn(int column, InstancesViewInput viewInput) {
		this.column = column;
		dateFormat = DateFormat.getDateTimeInstance();
		this.viewInput = viewInput;
	}

	public static final int INSTANCE_ID_COLUMN = 1;
	public static final int PUBLIC_DNS_COLUMN = 2;
	public static final int IMAGE_ID_COLUMN = 3;
	public static final int ROOT_DEVICE_COLUMN = 4;
	public static final int STATE_COLUMN = 5;
	public static final int INSTANCE_TYPE_COLUMN = 6;
	public static final int AVAILABILITY_ZONE_COLUMN = 7;
	public static final int KEY_NAME_COLUMN = 8;
	public static final int LAUNCH_TIME_COLUMN = 9;
	public static final int SECURITY_GROUPS_COLUMN = 10;
	public static final int TAGS_COLUMN = 11;

	@Override
	public String getText(Instance instance) {
		switch (this.column) {
		case INSTANCE_ID_COLUMN:
			return instance.getInstanceId();
		case PUBLIC_DNS_COLUMN:
			return instance.getPublicDnsName();
		case ROOT_DEVICE_COLUMN:
			return instance.getRootDeviceType();
		case STATE_COLUMN:
			return instance.getState().getName();
		case INSTANCE_TYPE_COLUMN:
			return instance.getInstanceType().toString();
		case AVAILABILITY_ZONE_COLUMN:
			return instance.getPlacement().getAvailabilityZone();
		case IMAGE_ID_COLUMN:
			return instance.getImageId();
		case KEY_NAME_COLUMN:
			return instance.getKeyName();
		case LAUNCH_TIME_COLUMN:
			if (instance.getLaunchTime() == null)
				return "";
			return dateFormat.format(instance.getLaunchTime());
		case SECURITY_GROUPS_COLUMN:
			return formatSecurityGroups(viewInput.securityGroupMap.get(instance
					.getInstanceId()));
		case TAGS_COLUMN:
			return TagFormatter.formatTags(instance.getTags());
		default:
			return "???";
		}
	}

	/**
	 * Takes the list of security groups and turns it into a comma separated
	 * string list.
	 * 
	 * @param securityGroups
	 *            A list of security groups to turn into a comma separated
	 *            string list.
	 * 
	 * @return A comma separated list containing the contents of the specified
	 *         list of security groups.
	 */
	private String formatSecurityGroups(List<String> securityGroups) {
		if (securityGroups == null)
			return "";

		String allSecurityGroups = "";
		for (String securityGroup : securityGroups) {
			if (allSecurityGroups.length() > 0)
				allSecurityGroups += ", ";
			allSecurityGroups += securityGroup;
		}

		return allSecurityGroups;
	}

	@Override
	public Image getImage(Instance instance) {
		switch (column) {
		case INSTANCE_ID_COLUMN:
			return Ec2Plugin.getDefault().getImageRegistry().get("server");
		case KEY_NAME_COLUMN:
			if (keyPairManager.isKeyPairValid(AwsToolkitCore.getDefault()
					.getCurrentAccountId(), instance.getKeyName())) {
				return Ec2Plugin.getDefault().getImageRegistry().get("check");
			} else {
				return Ec2Plugin.getDefault().getImageRegistry().get("error");
			}

		case STATE_COLUMN:
			String state = instance.getState().getName().toLowerCase();
			return stateImageMap.get(state);
		}

		return null;
	}

	/** Map of instance states to images representing those states */
	private static final Map<String, Image> stateImageMap = new HashMap<String, Image>();

	static {
		stateImageMap.put("running", Ec2Plugin.getDefault().getImageRegistry()
				.get("status-running"));
		stateImageMap.put("rebooting", Ec2Plugin.getDefault()
				.getImageRegistry().get("status-rebooting"));
		stateImageMap.put("shutting-down", Ec2Plugin.getDefault()
				.getImageRegistry().get("status-waiting"));
		stateImageMap.put("pending", Ec2Plugin.getDefault().getImageRegistry()
				.get("status-waiting"));
		stateImageMap.put("stopping", Ec2Plugin.getDefault().getImageRegistry()
				.get("status-waiting"));
		stateImageMap.put("stopped", Ec2Plugin.getDefault().getImageRegistry()
				.get("status-terminated"));
		stateImageMap.put("terminated", Ec2Plugin.getDefault()
				.getImageRegistry().get("status-terminated"));
	}

	@Override
	public int compare(Instance i1, Instance i2) {
		switch (column) {
		case INSTANCE_ID_COLUMN:
			return (i1.getInstanceId().compareTo(i2.getInstanceId()));
		case PUBLIC_DNS_COLUMN:
			return (i1.getPublicDnsName().compareTo(i2.getPublicDnsName()));
		case ROOT_DEVICE_COLUMN:
			return (i1.getRootDeviceType().compareTo(i2.getRootDeviceType()));
		case STATE_COLUMN:
			return (i1.getState().getName().compareTo(i2.getState().getName()));
		case INSTANCE_TYPE_COLUMN:
			return (i1.getInstanceType().compareTo(i2.getInstanceType()));
		case AVAILABILITY_ZONE_COLUMN:
			return (i1.getPlacement().getAvailabilityZone().compareTo(i2
					.getPlacement().getAvailabilityZone()));
		case IMAGE_ID_COLUMN:
			return (i1.getImageId().compareTo(i2.getImageId()));
		case KEY_NAME_COLUMN:
			String k1 = i1.getKeyName();
			String k2 = i2.getKeyName();
			if (k1 == null)
				k1 = "";
			if (k2 == null)
				k2 = "";
			return k1.compareTo(k2);
		case LAUNCH_TIME_COLUMN:
			return (i1.getLaunchTime().compareTo(i2.getLaunchTime()));
		case SECURITY_GROUPS_COLUMN:
			return compareSecurityGroups(i1, i2);
		case TAGS_COLUMN:
			return TagFormatter.formatTags(i1.getTags()).compareTo(
					TagFormatter.formatTags(i2.getTags()));
		default:
			return 0;
		}
	}

	/**
	 * Compares the security groups for the specified instances and returns a
	 * -1, 0, or 1 depending on the comparison. See compareTo() for more details
	 * on the returned value.
	 * 
	 * @param i1
	 *            The first instance to compare.
	 * @param i2
	 *            The second instance to compare.
	 * 
	 * @return -1, 0, or 1 depending on the comparison of the security groups in
	 *         the specified instances.
	 */
	private int compareSecurityGroups(Instance i1, Instance i2) {
		List<String> groups1 = viewInput.securityGroupMap.get(i1
				.getInstanceId());
		List<String> groups2 = viewInput.securityGroupMap.get(i2
				.getInstanceId());

		String formattedList1 = formatSecurityGroups(groups1);
		String formattedList2 = formatSecurityGroups(groups2);

		return formattedList1.compareTo(formattedList2);
	}

	@Override
	public String getColumnName() {
		switch (this.column) {
		case INSTANCE_ID_COLUMN:
			return "Instance ID";
		case PUBLIC_DNS_COLUMN:
			return "Public DNS Name";
		case ROOT_DEVICE_COLUMN:
			return "Root Device Type";
		case STATE_COLUMN:
			return "State";
		case INSTANCE_TYPE_COLUMN:
			return "Type";
		case AVAILABILITY_ZONE_COLUMN:
			return "Availability Zone";
		case IMAGE_ID_COLUMN:
			return "Image ID";
		case KEY_NAME_COLUMN:
			return "Key Pair";
		case LAUNCH_TIME_COLUMN:
			return "Launch Time";
		case SECURITY_GROUPS_COLUMN:
			return "Security Groups";
		case TAGS_COLUMN:
			return "Tags";
		default:
			return "???";
		}
	}

}
