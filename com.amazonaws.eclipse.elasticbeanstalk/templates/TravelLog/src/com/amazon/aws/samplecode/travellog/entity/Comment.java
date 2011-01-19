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
import java.util.Calendar;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 * The comment object contains the information associated with a single comment
 * record in the journal.  It has a relationship with the commenter object to track
 * who posted the comment, as well as the entry the comment is associated with.
 */
@Entity
public class Comment {

	@Id 
	@GeneratedValue(strategy=GenerationType.AUTO)
	private String id;
	private String body;
	private Commenter commenter;
	private Date date = Calendar.getInstance().getTime();
	private Entry entry;
	private String formattedDate;
	
	private static final SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a z");
	
	@Lob
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	
	@ManyToOne(cascade=CascadeType.ALL)
	public Commenter getCommenter() {
		return commenter;
	}
	public void setCommenter(Commenter commenter) {
		this.commenter = commenter;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
		this.formattedDate = formatter.format(date);
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	@ManyToOne(cascade=CascadeType.ALL)
	public Entry getEntry() {
		return entry;
	}
	public void setEntry(Entry entry) {
		this.entry = entry;
	}
	
	@Transient
	public String getFormattedDate() {
		return formattedDate;
	}

	public void setFormattedDate(String formattedDate) {
		this.formattedDate = formattedDate;
	}
	
}
