/*
 * Copyright 2008-2011 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.ec2.keypairs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.ec2.Ec2ClientFactory;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.KeyPair;

/**
 * Manages the EC2 key pairs that the plugin knows about, including creating new
 * key pairs, registering private key files with a named key, etc.
 */
public class KeyPairManager {

	/** The suffix for private key files */
	private static final String PRIVATE_KEY_SUFFIX = ".pem";

	/** EC2 client factory for all instances to share */
	private static final Ec2ClientFactory clientFactory = new Ec2ClientFactory();

	/**
	 * Returns the private key file associated with the named key. If no private
	 * key is registered for the named key yet, this method will look in the
	 * default private key directory to see if there's a matching private key
	 * there. If nothing is found even after looking in the default private key
	 * directory, this method returns null.
	 *
	 * @param keyPairName
	 *            The name of the key being looked up.
	 * @return The file path to the associated private key file for the named
	 *         key.
	 */
	public String lookupKeyPairPrivateKeyFile(String keyPairName) {
		try {
			Properties registeredKeyPairs = loadRegisteredKeyPairs();
			String privateKeyPath = registeredKeyPairs.getProperty(keyPairName);
			if (privateKeyPath != null) return privateKeyPath;

			// Try to find the private key in the default private key directory
			// if it hasn't been explicitly registered.
			File defaultPrivateKeyDirectory = getDefaultPrivateKeyDirectory();
			File privateKey = new File(defaultPrivateKeyDirectory, keyPairName + PRIVATE_KEY_SUFFIX);
			if (privateKey.exists() && privateKey.isFile()) {
				return privateKey.getAbsolutePath();
			}
		} catch (IOException e) {
			Status status = new Status(Status.WARNING, Ec2Plugin.PLUGIN_ID,
					"Unable to load registered key pairs file: " + e.getMessage(), e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}

		return null;
	}

	/**
	 * Returns true if and only if the specified key pair is valid, meaning it
	 * has a valid registered private key file.
	 *
	 * @param keyPairName
	 *            The name of the key pair to check.
	 *
	 * @return True if and only if the specified key pair is valid, meaning it
	 *         has a valid registered private key file.
	 */
	public boolean isKeyPairValid(String keyPairName) {
		try {
			String keyFile = lookupKeyPairPrivateKeyFile(keyPairName);
			if (keyFile == null) return false;

			File f = new File(keyFile);
			return f.isFile();
		} catch (Exception e) {
			// If we catch an exception, we know this must not be valid
		}

		return false;
	}

	/**
	 * Requests a new key pair from EC2 with the specified name, and saves the
	 * private key portion in the specified directory.
	 *
	 * @param keyPairName
	 *            The name of the requested key pair.
	 * @param keyPairDirectory
	 *            The directory in which to save the private key file.
	 *
	 * @throws IOException
	 *             If any problems were encountered storing the private key to
	 *             disk.
	 * @throws AmazonClientException
	 *             If any problems were encountered requesting a new key pair
	 *             from EC2.
	 */
	public void createNewKeyPair(String keyPairName, String keyPairDirectory)
			throws IOException, AmazonClientException {

		File keyPairDirectoryFile = new File(keyPairDirectory);
		if (! keyPairDirectoryFile.exists()) {
			if (! keyPairDirectoryFile.mkdirs()) {
				throw new IOException("Unable to create directory: " + keyPairDirectory);
			}
		}

		File privateKeyFile = new File(keyPairDirectory + File.separator + keyPairName + PRIVATE_KEY_SUFFIX);

		CreateKeyPairRequest request = new CreateKeyPairRequest();
		request.setKeyName(keyPairName);
        CreateKeyPairResult response = clientFactory.getAwsClient().createKeyPair(request);
		KeyPair keyPair = response.getKeyPair();

		String privateKey = keyPair.getKeyMaterial();

		FileWriter writer = new FileWriter(privateKeyFile);
		try {
			writer.write(privateKey);
		} finally {
			writer.close();
		}

		registerKeyPair(keyPairName, privateKeyFile.getAbsolutePath());

		/*
		 * SSH requires our private key be locked down.
		 */

		try {
			/*
			 * TODO: We should model these platform differences better (and support windows).
			 */
			Runtime.getRuntime().exec("chmod 600 " + privateKeyFile.getAbsolutePath());
		} catch (IOException e) {
			Status status = new Status(Status.WARNING, Ec2Plugin.PLUGIN_ID,
					"Unable to restrict permissions on private key file: " + e.getMessage(), e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
	}

	/**
	 * Returns the default directory where the plugin assumes private keys are
	 * stored.
	 *
	 * @return The default directory where the plugin assumes private keys are
	 *         stored.
	 */
	public File getDefaultPrivateKeyDirectory() {
		String userHomeDir = System.getProperty("user.home");
		if (userHomeDir == null || userHomeDir.length() == 0) return null;

		return new File(userHomeDir + File.separator + ".ec2");
	}

	/**
	 * Registers an existing key pair and private key file with this key pair
	 * manager.  This method is only for *existing* key pairs.  If you need a
	 * new key pair created, you should be using createNewKeyPair.
	 *
	 * @param keyName
	 *            The name of the key being registered.
	 * @param privateKeyFile
	 *            The path to the private key file for the specified key pair.
	 * @throws IOException
	 *             If any problems are encountered adding the specified key pair
	 *             to the mapping of registered key pairs.
	 */
	public void registerKeyPair(String keyName, String privateKeyFile) throws IOException {
		Properties registeredKeyPairs = loadRegisteredKeyPairs();
		registeredKeyPairs.put(keyName, privateKeyFile);
		storeRegisteredKeyPairs(registeredKeyPairs);
	}


	/*
	 * Private Interface
	 */

	private Properties loadRegisteredKeyPairs() throws IOException {
		/*
		 * If the plugin isn't running (such as during tests), just return an
		 * empty property list.
		 */
		Ec2Plugin plugin = Ec2Plugin.getDefault();
		if (plugin == null) return new Properties();

		/*
		 * TODO: we could optimize this and only load the registered key pairs on startup
		 * and after changes.
		 */
		File pluginStateLocation = plugin.getStateLocation().toFile();
		File registeredKeyPairsFile = new File(pluginStateLocation, "registeredKeyPairs.properties");
		registeredKeyPairsFile.createNewFile();
		FileInputStream fileInputStream = new FileInputStream(registeredKeyPairsFile);

		try {
			Properties registeredKeyPairs = new Properties();
			registeredKeyPairs.load(fileInputStream);

			return registeredKeyPairs;
		} finally {
			fileInputStream.close();
		}
	}

	private void storeRegisteredKeyPairs(Properties registeredKeyPairs) throws IOException {
		File pluginStateLocation = Ec2Plugin.getDefault().getStateLocation().toFile();
		File registeredKeyPairsFile = new File(pluginStateLocation, "registeredKeyPairs.properties");
		registeredKeyPairsFile.createNewFile();
		FileOutputStream fileOutputStream = new FileOutputStream(registeredKeyPairsFile);
		try {
			registeredKeyPairs.store(fileOutputStream, null);
		} finally {
			fileOutputStream.close();
		}
	}

}
