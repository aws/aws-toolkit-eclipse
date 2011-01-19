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
package com.amazon.aws.samplecode.travellog.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.amazon.aws.samplecode.travellog.aws.S3PhotoUtil;
import com.amazon.aws.samplecode.travellog.aws.S3StorageManager;
import com.amazon.aws.samplecode.travellog.aws.TravelLogStorageObject;
import com.amazon.aws.samplecode.travellog.dao.TravelLogDAO;
import com.amazon.aws.samplecode.travellog.entity.Comment;
import com.amazon.aws.samplecode.travellog.entity.Entry;
import com.amazon.aws.samplecode.travellog.entity.Journal;
import com.amazon.aws.samplecode.travellog.entity.Photo;

/**
 * This class extracts data from SimpleDB and builds a series of
 * properties files that represent the journal data.  The data is then
 * bundled as a zip file and optionally stored out on S3 or to the file system.
 */
public class DataExtractor implements Runnable {

    private static Logger logger = Logger.getLogger(DataExtractor.class.getName());
    private TravelLogDAO dao;
    private String outputPath;
    private String bucketName;
    private String storagePath;
    private SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
    private SimpleDateFormat hourFormatter = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a z");

    /**
     * Constructor to store the extracted zip to a file path
     * @param outputPath the file path to store the zip in
     * @param dao DAO to read in the data for export
     */
    public DataExtractor (String outputPath, TravelLogDAO dao) {
        this.outputPath = outputPath;
        this.dao = dao;
    }

    /**
     * Constructor to store the extracted zip to an S3 bucket.  We will use full
     * redundancy storage here since this is a backup that we want to make sure
     * remains intact.
     *
     * @param bucketName the name of the bucket to store the zip in
     * @param storagePath the path for storing zip within specified bucket
     * @param dao DAO to read in the data for export
     */
    public DataExtractor (String bucketName, String storagePath, TravelLogDAO dao) {
        this.bucketName = bucketName;
        this.storagePath = storagePath;
        this.dao = dao;
    }

    public void run() {
        try {
            //Create temporary directory
            File tmpDir = File.createTempFile("travellog", "");
            tmpDir.delete(); //Wipe out temporary file to replace with a directory
            tmpDir.mkdirs();

            logger.log(Level.INFO,"Extract temp dir: "+tmpDir);

            //Store journal to props file
            Journal journal = dao.getJournal();
            Properties journalProps = buildProps(journal);
            File journalFile = new File(tmpDir,"journal");
            journalProps.store(new FileOutputStream(journalFile), "");

            //Iterate through entries and grab related photos
            List<Entry> entries = dao.getEntries(journal);
            int entryIndex=1;
            int imageFileIndex=1;
            for (Entry entry: entries) {
                Properties entryProps = buildProps(entry);
                File entryFile = new File(tmpDir,"entry."+(entryIndex++));
                entryProps.store(new FileOutputStream(entryFile),"");

                List<Photo> photos = dao.getPhotos(entry);
                int photoIndex = 1;
                for (Photo photo: photos) {
                    Properties photoProps = buildProps(photo);

                    InputStream photoData = S3PhotoUtil.loadOriginalPhoto(photo);
                    String imageFileName = "imgdata."+(imageFileIndex++);
                    File imageFile = new File(tmpDir,imageFileName);

                    FileOutputStream outputStream = new FileOutputStream(imageFile);
                    IOUtils.copy(photoData, outputStream);
                    photoProps.setProperty("file", imageFileName);
                    outputStream.close();
                    photoData.close();

                    File photoFile = new File(tmpDir,"photo."+(entryIndex-1)+"."+(photoIndex++));
                    photoProps.store(new FileOutputStream(photoFile),"");
                }

                List<Comment> comments = dao.getComments(entry);
                int commentIndex = 1;
                for (Comment comment: comments) {
                    Properties commentProps = buildProps(comment);
                    File commentFile = new File(tmpDir,"comment."+(entryIndex-1)+"."+commentIndex++);
                    commentProps.store(new FileOutputStream(commentFile),"");
                }
            }

            //Bundle up the folder as a zip
            final File zipOut;

            //If we have an output path store locally
            if (outputPath!=null) {
                zipOut  = new File(outputPath);
            }
            else {
                //storing to S3
                zipOut = File.createTempFile("export", ".zip");
            }

            zipOut.getParentFile().mkdirs(); //make sure directory structure is in place
            ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(zipOut);

            //Create the zip file
            File [] files = tmpDir.listFiles();
            for (File file:files) {
                ZipArchiveEntry archiveEntry = new ZipArchiveEntry(file.getName());
                byte [] fileData = FileUtils.readFileToByteArray(file);
                archiveEntry.setSize(fileData.length);
                zaos.putArchiveEntry(archiveEntry);
                zaos.write(fileData);
                zaos.flush();
                zaos.closeArchiveEntry();
            }
            zaos.close();

            //If outputpath
            if (outputPath == null) {
                TravelLogStorageObject obj = new TravelLogStorageObject();
                obj.setBucketName(bucketName);
                obj.setStoragePath(storagePath);
                obj.setData(FileUtils.readFileToByteArray(zipOut));
                obj.setMimeType("application/zip");

                S3StorageManager mgr = new S3StorageManager();
                mgr.store(obj,false,null); //Store with full redundancy and default permissions
            }

        }
        catch (Exception e) {
            logger.log(Level.SEVERE,e.getMessage(),e);
        }

    }

    /**
     * The methods below here are all used to convert objects read from SimpleDB
     * into a series of property files.  Each method handles a specific type of object.
     */

    private Properties buildProps (Journal journal)  {
        Properties props = new Properties();
        props.setProperty("title",journal.getTitle());
        props.setProperty("description",journal.getDescription());
        props.setProperty("start_date",formatter.format(journal.getStartDate()));
        props.setProperty("end_date",formatter.format(journal.getEndDate()));
        return props;

    }

    private Properties buildProps (Entry entry)  {
        Properties props = new Properties();
        props.setProperty("title",entry.getTitle());
        props.setProperty("entry_text",entry.getEntryText());
        props.setProperty("destination",entry.getDestination());
        props.setProperty("date",formatter.format(entry.getDate()));
        return props;

    }

    private Properties buildProps (Photo photo)  {
        Properties props = new Properties();
        props.setProperty("title",photo.getTitle());
        props.setProperty("date", formatter.format(photo.getDate()));
        props.setProperty("description", photo.getDescription());
        props.setProperty("subject",photo.getSubject());
        return props;
    }

    private Properties buildProps (Comment comment)  {
        Properties props = new Properties();
        props.setProperty("body",comment.getBody());
        props.setProperty("date", hourFormatter.format(comment.getDate()));
        props.setProperty("commenter.email",comment.getCommenter().getEmail());
        props.setProperty("commenter.name",comment.getCommenter().getName());

        return props;
    }

}
