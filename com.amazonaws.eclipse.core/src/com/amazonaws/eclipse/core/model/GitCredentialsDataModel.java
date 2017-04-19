/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.core.model;

public class GitCredentialsDataModel {

    public static final String P_USERNAME = "username";
    public static final String P_PASSWORD = "password";
    public static final String P_SHOW_PASSWORD = "showPassword";

    private String username;
    private String password;
    private boolean showPassword;

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
    public boolean isShowPassword() {
        return showPassword;
    }
    public void setShowPassword(boolean showPassword) {
        this.showPassword = showPassword;
    }

}
