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

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

import javax.persistence.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UserDetails;


/**
 * Contains the basic user information for the administrator of the journal.  Could be expanded later
 * to provide user accounts that do not qualify as admin, but this would require additional 
 * infrastructure for managing users, resetting passwords, etc.  
 * 
 * Implements the UserDetails interface to support authentication via Spring.  Currently there is
 * only one role supported, ROLE_ADMIN, and it is hard coded since we only ever have a single admin
 * user.  This would need to be expanded to provide additional user accounts.
 */
@Entity
public class User implements Serializable, UserDetails {

	
	private static final long serialVersionUID = 1629672935573849314L;

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private String id;
	
	@Column(unique=true, nullable=false)
	private String username;

	private String password;
	
	//Admin role is only role for an authenticated user so it's hard coded here
	private static final String ROLE_ADMIN="ROLE_ADMIN";
	private static final GrantedAuthority auth = new GrantedAuthorityImpl(ROLE_ADMIN);
	private static final Collection<GrantedAuthority> authorities = new LinkedList<GrantedAuthority>();
	static {
		authorities.add(auth);
	}
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	/**
	 * All methods below this point are to implement the UserDetails interface.  
	 * They are all Transient because they are hard coded in the class for the
	 * time being.  If account expiration, etc, were implemented as part of a broader
	 * move to make this a multi-user app, these would be stored as well.
	 */
	@Transient
	public Collection<GrantedAuthority> getAuthorities() {
		return authorities;
	}
	
	
	@Transient
	public boolean isAccountNonExpired() {
		return true;
	}
	
	@Transient
	public boolean isAccountNonLocked() {
		return true;
	}
	
	@Transient
	public boolean isCredentialsNonExpired() {
		return true;
	}
	
	@Transient
	public boolean isEnabled() {
		return true;
	}

	
   
}
