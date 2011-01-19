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
package com.amazon.aws.samplecode.travellog.web;

import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazon.aws.samplecode.travellog.dao.TravelLogDAO;
import com.amazon.aws.samplecode.travellog.entity.Entry;
import com.amazon.aws.samplecode.travellog.entity.Photo;

/**
 * This class is a controller to handle the Ajax requests from the TravelLog UI.  
 * It leverages Direct Web Remoting (DWR) to simplify the Ajax coding.
 */
@Service
@RemoteProxy(name="AjaxController")
public class AjaxController {
	
	private TravelLogDAO dao;
	
	@RemoteMethod
	public Entry getEntry (String entryId) {
		Entry entry = dao.getEntry(entryId);
		
		return entry;
	}
	
	@RemoteMethod
	public Photo getPhoto (String photoId) {
		return dao.getPhoto(photoId);
	}
	
	@Autowired
    public void setTravelLogDAO (TravelLogDAO dao) {
        this.dao = dao;
    }
}
