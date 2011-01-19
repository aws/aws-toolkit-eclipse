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


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.amazon.aws.samplecode.travellog.dao.TravelLogDAO;

/**
 * This is the authentication implementation for the TravelLog app.  It implements the
 * Spring UserDetails service to retrieve user information from SimpleDB.
 */
public class SpringAuthenticator implements UserDetailsService {


	public SpringAuthenticator() throws Exception {
		super();
	}

	private TravelLogDAO dao;
	
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
		UserDetails details = dao.getUser(username);
		if (details == null) {
			throw new UsernameNotFoundException("Unknown user: "+username);
		}
		return details;
	}

    @Autowired
    public void setTravelLogDAO (TravelLogDAO dao) {
    	this.dao = dao;
    }

}
