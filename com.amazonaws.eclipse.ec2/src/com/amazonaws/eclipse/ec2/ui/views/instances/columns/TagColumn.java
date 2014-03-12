package com.amazonaws.eclipse.ec2.ui.views.instances.columns;

import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;

public class TagColumn extends TableColumn {
	private String key;

	public TagColumn(String key) {
		this.key = key;
	}

	@Override
	public String getText(Instance instance) {
		return getTagValue(key, instance.getTags());
	}

	@Override
	public Image getImage(Instance instance) {
		return null;
	}
	
	private String getTagValue(String key, List<Tag> tags) {
		for (Tag t : tags) {
			if (t.getKey().equals(key))
				return t.getValue();
		}
		return "";
	}

	@Override
	public int compare(Instance i1, Instance i2) {
		return getText(i1).compareTo(getText(i2));
	}

	public String getTagName() {
		return key;
	}
	
	@Override
	public String getColumnName() {
		return "Tag:" + key;
	}
}
