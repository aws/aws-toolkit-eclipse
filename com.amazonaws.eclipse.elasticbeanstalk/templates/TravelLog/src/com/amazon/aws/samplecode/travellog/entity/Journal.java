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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Maps to a journal.  In the current implementation there is ever
 * only one journal, but data structure could support multiple journals
 * if needed later.
 */
@Entity
public class Journal {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private String id;

    @Column(unique=true, nullable=false)
    private String title;
    private String description;
    private Date startDate = Calendar.getInstance().getTime();
    private Date endDate = Calendar.getInstance().getTime();


    SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
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

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Date getStartDate() {
        return startDate;
    }
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getDateRangeString () {
        StringBuffer dateRangeSb = new StringBuffer();
        dateRangeSb.append(formatter.format(startDate));
        if (endDate!=null) {
            dateRangeSb.append(" - ").append(formatter.format(endDate));
        }
        return dateRangeSb.toString();
    }

    public void setDateRangeString (String dateRangeString) {
        //Do nothing this is never stored, always generated
    }

}
