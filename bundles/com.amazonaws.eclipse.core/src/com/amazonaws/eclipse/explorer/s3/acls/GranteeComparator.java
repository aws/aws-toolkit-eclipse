/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.s3.acls;

import java.util.Comparator;

import com.amazonaws.services.s3.model.Grantee;
import com.amazonaws.services.s3.model.GroupGrantee;

/**
 * Comparator implementation that sorts Amazon S3 Grantee objects. Group
 * Grantees are ordered first (starting with All Users, then Authenticated
 * Users), and any non-group Grantees are sorted by ID.
 */
final class GranteeComparator implements Comparator<Grantee> {
    @Override
    public int compare(Grantee g1, Grantee g2) {
        if (g1.getIdentifier().equals(g2.getIdentifier())) return 0;

        if (g1 instanceof GroupGrantee) {
            // List groups first
            if (g2 instanceof GroupGrantee == false) return -1;
            GroupGrantee gg1 = (GroupGrantee)g1;
            if (gg1 == GroupGrantee.AllUsers) return -1;
        }

        if (g2 instanceof GroupGrantee) {
            // List groups first
            if (g1 instanceof GroupGrantee == false) return 1;
            GroupGrantee gg2 = (GroupGrantee)g2;
            if (gg2 == GroupGrantee.AllUsers) return  1;
        }

        return g1.getIdentifier().compareTo(g2.getIdentifier());
    }
}
