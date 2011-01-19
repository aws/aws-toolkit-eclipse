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


import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.directwebremoting.annotations.RemoteProperty;
import org.directwebremoting.annotations.RemoteProxy;

import java.text.SimpleDateFormat;

/**
 * The entry class maps to a single journal entry. 
 */
@Entity
@RemoteProxy
public class Entry {
	@Id 
	@GeneratedValue(strategy=GenerationType.AUTO)
	@RemoteProperty
	private String id;

	@RemoteProperty
	private String title;

	@RemoteProperty
	private String entryText;
	
	@RemoteProperty
	private String destination;
	
	private Date date;

	@RemoteProperty
	private String formattedDate;

	private String snsArn;
	
	private static final SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
	
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
		this.formattedDate = formatter.format(date);
	}
	
	private Journal journal;


	@ManyToOne(cascade=CascadeType.ALL)
	public Journal getJournal() {
		return journal;
	}
	
	public void setJournal(Journal journal) {
		this.journal = journal;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Lob //Lob annotation tells SimpleJPA to store the entry text in S3
	public String getEntryText() {
		return entryText;
	}
	public void setEntryText(String entryText) {
		this.entryText = entryText;
	}

	
	
	public String getDestination() {
		return destination;
	}
	public void setDestination(String destination) {
		this.destination = destination;
	}


	@Transient
	public String getFormattedDate() {
		return formattedDate;
	}

	public void setFormattedDate(String formattedDate) {
		this.formattedDate = formattedDate;
	}

	

	public String getSnsArn() {
		return snsArn;
	}

	public void setSnsArn(String snsArn) {
		this.snsArn = snsArn;
	}

	@Transient
	public SimpleDateFormat getFormatter() {
		return formatter;
	}

}
