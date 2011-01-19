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
package com.amazon.aws.samplecode.travellog.aws;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazon.aws.samplecode.travellog.util.Configuration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.StorageClass;


/**
 * This is a class for storage of any kind of data on S3.  There is some functionality included in this
 * class that's not used in the TravelLog application but should serve to illustrate additional
 * capabilities of S3.
 *
 */
public class S3StorageManager {

	private Date lastUpdate;

	/*
	 * The s3 client class is thread safe so we only ever need one static instance.
	 * While you can have multiple instances it is better to only have one because it's
	 * a relatively heavy weight class.
	 */
	private static AmazonS3 s3client;
	static {
		AWSCredentials creds = new BasicAWSCredentials(getKey(), getSecret());
		s3client = new AmazonS3Client(creds);
	}

	/**
	 * The bucket map keeps track of whether a bucket exists or not.  The first time
	 * any bucket name is called, it will be checked against this map and created
	 * if not already available.
	 */
	private static Map<String,Boolean> bucketMap = new LinkedHashMap<String,Boolean>();

	public Date getLastUpdate() {
		return lastUpdate;
	}

	private static final Logger logger = Logger.getLogger(S3StorageManager.class.getName());


	/**
	 * Stores a given item on S3
	 * @param obj the data to be stored
	 * @param reducedRedundancy whether or not to use reduced redundancy storage
	 * @param acl a canned access control list indicating what permissions to store this object with (can be null to leave it set to default)
	 */
	public void store(TravelLogStorageObject obj, boolean reducedRedundancy, CannedAccessControlList acl) {
		//Make sure the bucket exists before we try to use it
		checkForAndCreateBucket(obj.getBucketName());

		ObjectMetadata omd = new ObjectMetadata();
		omd.setContentType(obj.getMimeType());
		omd.setContentLength(obj.getData().length);

		ByteArrayInputStream is = new ByteArrayInputStream(obj.getData());
		PutObjectRequest request = new PutObjectRequest(obj.getBucketName(), obj.getStoragePath(), is, omd);

		//Check if reduced redundancy is enabled
		if (reducedRedundancy) {
			request.setStorageClass(StorageClass.ReducedRedundancy);
		}

		s3client.putObject(request);

		//If we have an ACL set access permissions for the the data on S3
		if (acl!=null) {
			s3client.setObjectAcl(obj.getBucketName(), obj.getStoragePath(), acl);
		}

	}

	/**
	 * This method will call out to S3 to make sure that the specified bucket
	 * exists.  If it does not exist it will create it.
	 * @param bucketName name of the bucket to be checked/created
	 */
	public void checkForAndCreateBucket(String bucketName) {
		bucketName = bucketName.toLowerCase(); //Make sure it's lower case to comply with AWS SDK requirements
		if (bucketMap.get(bucketName)==null) {

			if (s3client.doesBucketExist(bucketName)) {
				bucketMap.put(bucketName, true);
			}
			else {
				//Bucket hasn't been created yet so we create it
				s3client.createBucket(bucketName);
				bucketMap.put(bucketName, true);
			}
		}
	}


	/**
	 * This is a convenience method that stores an object as publicly readable
	 *
	 * @param obj object to be stored
	 * @param reducedRedundancy flag indicating whether to use reduced redundancy or not
	 */
	public void storePublicRead (TravelLogStorageObject obj, boolean reducedRedundancy) {
		store(obj,reducedRedundancy,CannedAccessControlList.PublicRead);
	}

	public InputStream loadInputStream (TravelLogStorageObject s3Store) throws IOException {
		S3Object s3 = getS3Object(s3Store);
		this.lastUpdate = s3.getObjectMetadata().getLastModified();
		return s3.getObjectContent();
	}

	/**
	 * Returns the raw S3 object from S3 service
	 * @param s3Store the s3 object to be loaded from the store
	 * @return the requested S3 object
	 */
	private S3Object getS3Object (TravelLogStorageObject s3Store) {
		S3Object obj = s3client.getObject(s3Store.getBucketName(),s3Store.getStoragePath());
		return obj;

	}


	/**
	 * Loads the raw object data from S3 storage
	 * @param s3Store the s3 object to be loaded from the store
	 * @return input stream for reading in the raw object
	 * @throws IOException
	 */
	public InputStream loadStream (TravelLogStorageObject s3Store) throws IOException {
		S3Object obj = getS3Object(s3Store);
		InputStream is = obj.getObjectContent();
		return is;
	}



	/**
	 * Deletes the specified S3 object from the S3 storage service.  If a
	 * storage path is passed in that has child S3 objects, it will recursively
	 * delete the underlying objects.
	 * @param s3Store the s3 object to be deleted
	 */
	public void delete (TravelLogStorageObject s3Store) {

		if (s3Store.getStoragePath() == null || s3Store.getStoragePath().equals("")) {
			logger.log(Level.WARNING,"Empty storage path passed to delete method");
			return; //We don't want to delete everything in a path
		}


		//Go through the store structure and delete child objects
		ObjectListing listing =s3client.listObjects(s3Store.getBucketName(), s3Store.getStoragePath());
		while (true) {
			List<S3ObjectSummary> objectList = listing.getObjectSummaries();
			for (S3ObjectSummary summary:objectList) {
				s3client.deleteObject(s3Store.getBucketName(),summary.getKey());
			}
			if (listing.isTruncated()) {
				listing = s3client.listNextBatchOfObjects(listing);
			}
			else {
				break;
			}
		}

	}


	/**
	 * This method will obtain a presigned URL that will expire on the given
	 * date.
	 * @param s3Store the S3 object for which to obtain a presigned url
	 * @param expirationDate date when the presigned url should expire
	 * @return the signed URL
	 */
	public URL getSignedUrl (TravelLogStorageObject s3Store, Date expirationDate) {
		logger.log(Level.FINEST,"PRESIGNED URL: "+s3Store.getBucketName()+"/"+s3Store.getStoragePath());
		return s3client.generatePresignedUrl(s3Store.getBucketName(), s3Store.getStoragePath(), expirationDate);

	}

	public static String getKey () {
		Configuration config = Configuration.getInstance();
		return config.getProperty("accessKey");
	}

	public static String getSecret () {
		Configuration config = Configuration.getInstance();
		return config.getProperty("secretKey");
	}

}
