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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import com.amazon.aws.samplecode.travellog.aws.S3PhotoUtil;
import com.amazon.aws.samplecode.travellog.aws.S3StorageManager;
import com.amazon.aws.samplecode.travellog.aws.TravelLogStorageObject;
import com.amazon.aws.samplecode.travellog.dao.TravelLogDAO;
import com.amazon.aws.samplecode.travellog.entity.Comment;
import com.amazon.aws.samplecode.travellog.entity.Commenter;
import com.amazon.aws.samplecode.travellog.entity.Entry;
import com.amazon.aws.samplecode.travellog.entity.Journal;
import com.amazon.aws.samplecode.travellog.entity.Photo;
/**
 * This class is responsible for calling out to S3 and downloading prepackaged data
 * for loading into the travellog system.  The zip bundles that are downloaded are
 * just a collection of properties files that map to the entities we store in the
 * database.
 */
public class DataLoader implements Runnable {

	private String bucketName;
	private String storagePath;

	//A map to keep track of the entries we're creating
	private Map<Integer, Entry> entryMap = new LinkedHashMap<Integer, Entry>();
	private Journal journal;

	private SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
	private SimpleDateFormat hourFormatter = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a z");

	private TravelLogDAO dao;

	private static Logger logger = Logger.getLogger(DataLoader.class.getName());

	/**
	 * Basic constructor for the data loader, setting up where to retrieve the zip bundle from
	 * @param bucketName bucket for the zip bundle
	 * @param storagePath the storage path within the bucket that points to the zip bundle
	 * @param dao DAO to write the data that we're importing
	 */
	public DataLoader (String bucketName, String storagePath, TravelLogDAO dao) {
		this.bucketName=bucketName;
		this.storagePath=storagePath;
		this.dao = dao;
	}

	public void run() {
		try {
			//Create S3 storage object to use in request for bundle
			TravelLogStorageObject obj = new TravelLogStorageObject();
			obj.setBucketName(bucketName);
			obj.setStoragePath(storagePath);

			//Make request to load from S3 storage
			S3StorageManager manager = new S3StorageManager();
			InputStream input = manager.loadInputStream(obj);

			//Create temporary directory
			File tmpDir = File.createTempFile("travellog", "");
			tmpDir.delete(); //Wipe out temporary file to replace with a directory
			tmpDir.mkdirs();

			//Extract downloaded data to the temporary directory
			TravelLogFileUtil.extractZipToDirectory(tmpDir, input);

			//Clear out any previously existing data
			purgeData();

			//Load the new data
			loadData(tmpDir);

			//cleanup
			tmpDir.delete();
		}
		catch (Exception e) {

			logger.log(Level.SEVERE,e.getMessage(),e);
		}
	}

	/**
	 * This method will go into a specified directory and load all the data
	 * into the journal.  The file structure is made up of a series of
	 * property files to provide a simple name/value pair matching that can
	 * then be loaded through our SimpleJPA objects.
	 *
	 * The journal object is in a file "journal" and then entries are in sequential
	 * order like this:
	 *
	 * <ul>
	 * <li>entry.1</li>
	 * <li>entry.2</li>
	 * <li>entry.3</li>
	 * <ul>
	 *
	 * Photo metadata is stored with a prefix like this:
	 * <ul>
	 * <li>photo.[entry #].[sequence].txt</li>
	 * </ul>
	 *
	 * So for example, photos associated with entry #2 would be as follows:
	 * <ul>
	 * <li>photo.1.1</li>
	 * <li>photo.1.2</li>
	 * </ul>
	 *
	 * Photos will be loaded in the order of the sequence id.  The txt file contains
	 * a property "file" that points to the actual image that should be loaded.
	 *
	 * Comments use the same nomenclature as photos.
	 *
	 * @param directory the directory where the imported data has been extracted to
	 * @throws IOException
	 * @throws ParseException
	 */
	private void loadData (File directory) throws IOException, ParseException {

		//Load journal
		File journalFile = new File(directory,"journal");
		Properties journalProps = new Properties();
		journalProps.load(new FileInputStream(journalFile));
		journal = buildJournalFromProps(journalProps);
		dao.saveJournal(journal);

		//Load entries
		File [] entries =  directory.listFiles(new EntryFilter());
		for (File entryFile:entries) {
			Properties entryProps = new Properties();
			entryProps.load(new FileInputStream(entryFile));
			Entry entry = buildEntryFromProps(entryProps);
			dao.saveEntry(entry);

			//Parse out entry id
			String [] entryNameSplit = entryFile.getName().split("\\.");
			int entryId = Integer.parseInt(entryNameSplit[1]);
			entryMap.put(entryId, entry);
		}

		//Load photos
		File [] photos = directory.listFiles(new PhotoFilter());


		for (File photoFile:photos) {
			Properties photoProps = new Properties();
			photoProps.load(new FileInputStream(photoFile));
			Photo photo = buildPhotoFromProps(photoProps);

			//Parse out entry id
			String [] photoNameSplit = photoFile.getName().split("\\.");
			int entryId = Integer.parseInt(photoNameSplit[1]);
			Entry entry = entryMap.get(entryId);

			photo.setEntry(entry);
			dao.savePhoto(photo);

			//Load jpeg file
			String fileName = photoProps.getProperty("file");
			File file = new File(directory,fileName);
			byte [] photoData = FileUtils.readFileToByteArray(file);
			photo = S3PhotoUtil.storePhoto(photo, photoData);
			dao.savePhoto(photo);

		}


		//Load comments
		File [] comments = directory.listFiles(new CommentFilter());

		for (File commentFile:comments) {
			Properties commentProps = new Properties();
			commentProps.load(new FileInputStream(commentFile));
			Comment comment = buildCommentFromProps(commentProps);

			//Parse out entry id
			String [] commentNameSplit = commentFile.getName().split("\\.");
			int entryId = Integer.parseInt(commentNameSplit[1]);
			Entry entry = entryMap.get(entryId);

			comment.setEntry(entry);
			dao.saveCommenter(comment.getCommenter());
			dao.saveComment(comment);
		}
	}

	private void purgeData () {
		List<Journal> journals = dao.getJournals();
		for (Journal journal:journals) {
			List<Entry> entries = dao.getEntries(journal);
			for (Entry entry:entries) {
				List<Photo> photos = dao.getPhotos(entry);
				for (Photo photo: photos) {
					dao.deletePhoto(photo);
				}

				List<Comment> comments = dao.getComments(entry);
				for (Comment comment: comments) {
					dao.deleteCommenter(comment.getCommenter());
					dao.deleteComment(comment);
				}
				dao.deleteEntry(entry);
			}
			dao.deleteJournal(journal);
		}
	}

	/*
	 * The remaining methods are used to convert a property file into the
	 * entity we need.
	 */

	private Journal buildJournalFromProps (Properties props) throws ParseException {
		Journal journal = new Journal();
		journal.setTitle(props.getProperty("title"));
		journal.setDescription(props.getProperty("description"));
		journal.setStartDate(formatter.parse(props.getProperty("start_date")));
		journal.setEndDate(formatter.parse(props.getProperty("end_date")));
		return journal;

	}

	private Entry buildEntryFromProps (Properties props) throws ParseException {
		Entry entry = new Entry();
		entry.setTitle(props.getProperty("title"));
		entry.setEntryText(props.getProperty("entry_text"));
		entry.setDestination(props.getProperty("destination"));
		entry.setJournal(journal);
		entry.setDate(formatter.parse(props.getProperty("date")));
		return entry;
	}

	private Photo buildPhotoFromProps (Properties props) throws ParseException {
		Photo photo = new Photo();
		photo.setTitle(props.getProperty("title"));
		photo.setDate(formatter.parse(props.getProperty("date")));
		photo.setDescription(props.getProperty("description"));
		photo.setSubject(props.getProperty("subject"));
		return photo;
	}

	private Comment buildCommentFromProps (Properties props) throws ParseException {
		Comment comment = new Comment();
		comment.setBody(props.getProperty("body"));
		comment.setDate(hourFormatter.parse(props.getProperty("date")));

		Commenter commenter = new Commenter();
		commenter.setEmail(props.getProperty("commenter.email"));
		commenter.setName(props.getProperty("commenter.name"));

		comment.setCommenter(commenter);
		return comment;
	}


}

//FileFilter classes used for searching for photo, entry, and comment files

class EntryFilter implements FileFilter {

	public boolean accept(File pathname) {
		return pathname.getName().startsWith("entry");
	}

}

class PhotoFilter implements FileFilter {

	public boolean accept(File pathname) {
		return pathname.getName().startsWith("photo");
	}

}

class CommentFilter implements FileFilter {

	public boolean accept(File pathname) {
		return pathname.getName().startsWith("comment");
	}

}
