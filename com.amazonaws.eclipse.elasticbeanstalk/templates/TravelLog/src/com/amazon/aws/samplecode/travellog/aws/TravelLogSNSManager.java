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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.amazon.aws.samplecode.travellog.entity.Comment;
import com.amazon.aws.samplecode.travellog.entity.Commenter;
import com.amazon.aws.samplecode.travellog.entity.Entry;
import com.amazon.aws.samplecode.travellog.util.Configuration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.DeleteTopicRequest;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.SubscribeResult;

/**
 * This is a utility class to manage SNS communication for entries and
 * comments.  It's largely responsible for taking the entities passed in
 * to the methods, breaking them down into SNS requests, and then 
 * sending those requests to the SNS client.
 * 
 * The SNS process involves three steps, all covered within this class:
 * <ol>
 * <li>Creating a new topic</li>
 * <li>Subscribing to a topic</li>
 * <li>Posting a new message to a topic</li>
 * </ol>
 * 
 * We do not have support here for unsubscribing from a topic.  SNS 
 * automatically handles the process of confirming a user's desire
 * to be subscribed.  Each message sent also includes an unsubscribe
 * link.  So this process can largely be dealt with automatically.
 *
 */
public class TravelLogSNSManager  {

	//SNS can support a variety of protocols, but in this case we only need email
	private static final String EMAIL_PROTOCOL="email";
	
	private static Logger logger = Logger.getLogger(TravelLogSNSManager.class.getName());
	
	/*
	 * The SNS client class is thread safe so we only ever need one static instance.  
	 * While you can have multiple instances it is better to only have one because it's
	 * a relatively heavy weight class.  
	 */
	private static AmazonSNSClient snsClient;
	
	static {
		AWSCredentials creds = new BasicAWSCredentials(getKey(), getSecret());
		snsClient = new AmazonSNSClient(creds);
	}

	
	/**
	 * Creates the SNS topic associated with an entry.  When the topic is created, 
	 * we will get an ARN (Amazon Resource Name) which uniquely identifies the 
	 * SNS topic.  We write that ARN to the entry entity so that we can refer to it
	 * later when subscribing commenters, etc.
	 * 
	 * @param entry the new entry that's associated with the topic
	 * @return the result returned from AWS
	 */
	public CreateTopicResult createTopic (Entry entry) {
		CreateTopicRequest request = new CreateTopicRequest(getTopicName(entry));
		CreateTopicResult result = snsClient.createTopic(request);
		entry.setSnsArn(result.getTopicArn());
		return result;
	}
	
	/**
	 * Deletes a previously created topic associated with the entry. 
	 * 
	 * @param entry the entry to be deleted
	 */
	public void deleteTopic (Entry entry) {
		DeleteTopicRequest request = new DeleteTopicRequest(entry.getSnsArn());
		snsClient.deleteTopic(request);
	}
	
	
	/**
	 * Publishes a comment to the specified entry. The method takes the comment and
	 * builds an SNS PublishRequest object.  Then the comment is published to the topic associated
	 * with the incoming entry.
	 * 
	 * @param entry the entry to publish to
	 * @param comment the comment to publish
	 * @return the result returned from AWS
	 */
	public PublishResult publish (Entry entry, Comment comment) {
		PublishRequest request = new PublishRequest();
		request.setTopicArn(entry.getSnsArn());
		
		StringBuilder subject = new StringBuilder("Comment Posted to Entry '");
		subject.append(entry.getTitle()).append("'");
		request.setSubject(subject.toString());
		
		StringBuilder body = new StringBuilder();
		body.append("The following comment was posted to the post '").append(entry.getTitle()).append("'\n");
		body.append("Posted by: ").append(comment.getCommenter().getName()).append("\n\n");
		body.append(comment.getBody());
		
		request.setMessage(body.toString());
		
		return snsClient.publish(request);
	}
	
	/**
	 * Subscribe a given commenter to future comments posted to the given entry.
	 * 
	 * @param entry the entry to subscribe the commenter to
	 * @param commenter the commenter to be subscribed
	 * @return the result returned by AWS
	 */
	public SubscribeResult subscribe (Entry entry, Commenter commenter) {
		if (StringUtils.isEmpty(entry.getSnsArn())) {
			//If ARN isn't set then entry didn't have an SNS topic created with it so we ignore
			logger.log(Level.WARNING,"Entry did not have an SNS topic associated with it");
			return null;
		}
		SubscribeRequest request = new SubscribeRequest(entry.getSnsArn(),EMAIL_PROTOCOL,commenter.getEmail());
		SubscribeResult result = snsClient.subscribe(request);
		return result;
	}
	
	/**
	 * This method returns a unique topic name by using the entry id.
	 * @param entry the entry to get a topic name for
	 * @return returns the topic name
	 */
	private String getTopicName (Entry entry) {
		return "entry_"+entry.getId();
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
