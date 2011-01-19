/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * 
 *  http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazon.aws.samplecode.travellog.entity;


import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.directwebremoting.annotations.DataTransferObject;
import org.directwebremoting.annotations.RemoteProperty;

/**
 * The photo class represents the metadata for a given uploaded photo.  It does not itself handle 
 * the storage of the actual binary photo data.  Instead, when a photo is stored, the S3PhotoUtil class
 * handles the process of resizing images and storing them on S3.  Once stored on S3, the paths to the
 * images are recorded in the Photo class and stored in SimpleDB.  
 * 
 * @see S3PhotoUtil
 */
@Entity
@DataTransferObject
public class Photo {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@RemoteProperty
	private String id;
	@RemoteProperty
	private String originalPath;
	@RemoteProperty
	private String websizePath;
	@RemoteProperty
	private String thumbnailPath;
	@RemoteProperty
	private String title;
	@RemoteProperty
	private String subject;
	@RemoteProperty
	private String description;
	@RemoteProperty
	private Date date;
	@RemoteProperty
	private String formattedDate;
	
	private SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
	
	private Entry entry;
	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOriginalPath() {
		return originalPath;
	}

	public void setOriginalPath(String originalPath) {
		this.originalPath = originalPath;
	}

	public String getWebsizePath() {
		return websizePath;
	}

	public void setWebsizePath(String websizePath) {
		this.websizePath = websizePath;
	}

	public String getThumbnailPath() {
		return thumbnailPath;
	}

	public void setThumbnailPath(String thumbnailPath) {
		this.thumbnailPath = thumbnailPath;
	}

	@ManyToOne(cascade=CascadeType.ALL)
	public Entry getEntry() {
		return entry;
	}


	public void setEntry(Entry entry) {
		this.entry = entry;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
		this.formattedDate = formatter.format(date);
	}

	@Transient
	public String getFormattedDate() {
		return formattedDate;
	}

	public void setFormattedDate(String formattedDate) {
		this.formattedDate = formattedDate;
	}
}
