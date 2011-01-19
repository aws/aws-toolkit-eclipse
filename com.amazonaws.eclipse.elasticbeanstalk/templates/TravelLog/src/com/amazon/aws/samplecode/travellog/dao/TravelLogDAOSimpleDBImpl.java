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
package com.amazon.aws.samplecode.travellog.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import com.amazon.aws.samplecode.travellog.aws.S3PhotoUtil;
import com.amazon.aws.samplecode.travellog.aws.S3StorageManager;
import com.amazon.aws.samplecode.travellog.entity.Comment;
import com.amazon.aws.samplecode.travellog.entity.Commenter;
import com.amazon.aws.samplecode.travellog.entity.Entry;
import com.amazon.aws.samplecode.travellog.entity.Journal;
import com.amazon.aws.samplecode.travellog.entity.Photo;
import com.amazon.aws.samplecode.travellog.entity.User;
import com.spaceprogram.simplejpa.EntityManagerFactoryImpl;

public class TravelLogDAOSimpleDBImpl implements TravelLogDAO {

    private static Map<String, String> properties = new HashMap<String, String>();
    static {
        properties.put("lobBucketName", S3StorageManager.getKey().toLowerCase() + "-travellog-lob");
    }

    private static EntityManagerFactoryImpl factory = new EntityManagerFactoryImpl("TravelLog", properties);

    public Journal getJournal(String journalId) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            Journal journal = em.find(Journal.class, journalId);
            return journal;
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }

    }

    public void saveJournal (Journal journal) {
        EntityManager em = null;
        //Storage fails if id is an empty string, so nullify it
        if (journal.getId()!=null && journal.getId().equals("")) {
            journal.setId(null);
        }
        try {
            em = factory.createEntityManager();
            em.persist(journal);

        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }

    public void deleteJournal(Journal journal) {

        //reload journal from DB so we get the entries
        //since cascades are not automatic
        journal = getJournal(journal.getId());

        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            em.remove(journal);

        }
        finally {
            if (em!=null) {
                em.close();
            }
        }

    }


    @SuppressWarnings("unchecked")
    public List<Journal> getJournals() {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            Query query = em.createQuery("select j from com.amazon.aws.samplecode.travellog.entity.Journal j");
            return (List<Journal>)query.getResultList();
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }




    public User getUser(String username) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            Query query = em.createQuery("select user from com.amazon.aws.samplecode.travellog.entity.User u where u.username=:username");
            query.setParameter("username", username);
            return (User)query.getSingleResult();
        }
        catch (NoResultException e) {
            //No matching result so return null
            return null;
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }


    public void saveUser(User user) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            em.persist(user);
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }


    public void deleteUser(User user) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            em.remove(user);
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }


    public void deleteEntry(Entry entry)  {
        EntityManager em = null;
        List<Photo> photos = getPhotos(entry);

        try {
            em = factory.createEntityManager();

            //Need to iterate through the associated photos
            //and remove them from S3
            for (Photo photo:photos) {
                S3PhotoUtil.deletePhotoFromS3(photo);
                em.remove(photo);
            }

            em.remove(entry);

        }
        finally {
            if (em!=null) {
                em.close();
            }
        }

    }

    @SuppressWarnings("unchecked")
    public List<Entry> getEntries(Journal journal) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            Query query = em.createQuery("select e from com.amazon.aws.samplecode.travellog.entity.Entry e " +
              "where e.journal=:journal and date is not null and id is not null order by e.date desc");
            query.setParameter("journal",journal);
            return (List<Entry>)query.getResultList();
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }


    public Entry getEntry(String entryId) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            Entry entry = em.find(Entry.class, entryId);
            return entry;
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }


    public void saveEntry(Entry entry) {
        EntityManager em = null;
        try {
            //Storage fails if id is an empty string, so nullify it
            if (entry.getId()!=null && entry.getId().equals("")) {
                entry.setId(null);
            }

            em = factory.createEntityManager();
            em.persist(entry);
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }



    public void deletePhoto(Photo photo)  {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            em.remove(photo);
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }

    }


    public void savePhoto(Photo photo) {
        EntityManager em = null;

        try {
            //Storage fails if id is an empty string, so nullify it
            if (photo.getId()!=null && photo.getId().equals("")) {
                photo.setId(null);
            }
            em = factory.createEntityManager();
            em.persist(photo);
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }

    }

    @SuppressWarnings("unchecked")
    public List<Photo> getPhotos(Entry entry) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            Query query = em.createQuery("select p from com.amazon.aws.samplecode.travellog.entity.Photo p where p.entry=:entry");
            query.setParameter("entry",entry);
            return (List<Photo>)query.getResultList();
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }




    public Journal getJournal() {
        List<Journal> journals = getJournals();
        for (Journal journal: journals) {
            return journal;
        }
        return null;
    }


    public Photo getPhoto(String photoId) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            Photo photo = em.find(Photo.class, photoId);
            return photo;
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<User> getUsers() {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            Query query = em.createQuery("select u from com.amazon.aws.samplecode.travellog.entity.User u");
            return (List<User>)query.getResultList();
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }




    public void deleteComment(Comment comment) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            em.remove(comment);
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }


    public void deleteCommenter(Commenter commenter) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            em.remove(commenter);
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<Comment> getComments(Entry entry) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            Query query = em.createQuery("select c from com.amazon.aws.samplecode.travellog.entity.Comment c where c.entry = :entry");
            query.setParameter("entry", entry);
            return (List<Comment>)query.getResultList();
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }


    public void saveComment(Comment comment) {
        EntityManager em = null;

        try {
            if (comment.getId()!=null && comment.getId().equals("")) {
                comment.setId(null);
            }
            em = factory.createEntityManager();
            em.persist(comment);
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }


    public void saveCommenter(Commenter commenter) {

        EntityManager em = null;

        try {
            if (commenter.getId()!=null && commenter.getId().equals("")) {
                commenter.setId(null);
            }
            em = factory.createEntityManager();
            em.persist(commenter);
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }


    public Comment getComment(String commentId) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            Comment comment = em.find(Comment.class, commentId);
            return comment;
        }
        finally {
            if (em!=null) {
                em.close();
            }
        }
    }
}
