/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.datatools.enablement.simpledb.driver;

/**
 * Wrapper around the item name to distinguish between usual and itemName strings in the UI and to forbid editing the
 * itemName once it was persisted to the SDB.
 */
public class SimpleDBItemName {

  private String itemName;
  private boolean persisted;

  /** This name is used in select queries and in the UI to represent the itemName value */
  public static final String ITEM_HEADER = "itemName()"; //$NON-NLS-1$

  /**
   * @param itemName
   */
  public SimpleDBItemName(final String itemName) {
    this.itemName = itemName;
  }

  /**
   * @param itemName
   * @param persisted
   */
  public SimpleDBItemName(final String itemName, final boolean persisted) {
    this.itemName = itemName;
    this.persisted = persisted;
  }

  /**
   * @param itemName
   */
  public void setItemName(final String itemName) {
    this.itemName = itemName;
  }

  /**
   * @return
   */
  public String getItemName() {
    return this.itemName;
  }

  /**
   * @param persisted
   *          <code>true</code> if itemName is saved to SDB or came from SDB
   */
  public void setPersisted(final boolean persisted) {
    this.persisted = persisted;
  }

  /**
   * @return <code>true</code> if itemName is saved to SDB or came from SDB
   */
  public boolean isPersisted() {
    return this.persisted;
  }

  @Override
  public String toString() {
    return this.itemName;
  }
}
