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

package com.amazonaws.eclipse.ec2.keypairs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.services.ec2.AmazonEC2;
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

    /**
     * Returns the private key file associated with the named key, or null if no
     * key file can be found.
     *
     * @param accountId
     *            The account id that owns the key name
     * @param keyPairName
     *            The name of the key being looked up.
     * @return The file path to the associated private key file for the named
     *         key.
     */
    public String lookupKeyPairPrivateKeyFile(String accountId, String keyPairName) {
        try {
            Properties registeredKeyPairs = loadRegisteredKeyPairs(accountId);
            String privateKeyPath = registeredKeyPairs.getProperty(keyPairName);
            if ( privateKeyPath != null )
                return privateKeyPath;

        } catch ( IOException e ) {
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
     * @param accountId
     *            The account id that owns the key name
     * @param keyPairName
     *            The name of the key pair to check.
     * @return True if and only if the specified key pair is valid, meaning it
     *         has a valid registered private key file.
     */
    public boolean isKeyPairValid(String accountId, String keyPairName) {
        try {
            String keyFile = lookupKeyPairPrivateKeyFile(accountId, keyPairName);
            if ( keyFile == null )
                return false;

            File f = new File(keyFile);
            return f.isFile();
        } catch ( Exception e ) {
            // If we catch an exception, we know this must not be valid
        }

        return false;
    }

    /**
     * Requests a new key pair from EC2 with the specified name, and saves the
     * private key portion in the specified directory.
     *
     * @param accountId
     *            The account id that owns the key name
     * @param keyPairName
     *            The name of the requested key pair.
     * @param keyPairDirectory
     *            The directory in which to save the private key file.
     * @param ec2RegionOverride
     *            The region where the EC2 key pair is created.
     * @throws IOException
     *             If any problems were encountered storing the private key to
     *             disk.
     * @throws AmazonClientException
     *             If any problems were encountered requesting a new key pair
     *             from EC2.
     */
    public void createNewKeyPair(String accountId, String keyPairName, String keyPairDirectory, Region ec2RegionOverride) throws IOException,
    AmazonClientException {
        File keyPairDirectoryFile = new File(keyPairDirectory);
        if ( !keyPairDirectoryFile.exists() ) {
            if ( !keyPairDirectoryFile.mkdirs() ) {
                throw new IOException("Unable to create directory: " + keyPairDirectory);
            }
        }

        /**
         * It's possible that customers could have two keys with the same name,
         * so silently rename to avoid such a conflict. This isn't the most
         * straightforward user interface, but probably better than enforced
         * directory segregation by account, or else disallowing identical names
         * across accounts.
         */
        File privateKeyFile = new File(keyPairDirectoryFile, keyPairName + PRIVATE_KEY_SUFFIX);
        int i = 1;
        while ( privateKeyFile.exists() ) {
            privateKeyFile = new File(keyPairDirectoryFile, keyPairName + "-" + i + PRIVATE_KEY_SUFFIX);
        }

        CreateKeyPairRequest request = new CreateKeyPairRequest();
        request.setKeyName(keyPairName);
        AmazonEC2 ec2 = null;
        if (ec2RegionOverride == null) {
            ec2 = Ec2Plugin.getDefault().getDefaultEC2Client();
        } else {
            ec2 = AwsToolkitCore.getClientFactory().getEC2ClientByEndpoint(ec2RegionOverride.getServiceEndpoint(ServiceAbbreviations.EC2));
        }
        CreateKeyPairResult response = ec2.createKeyPair(request);
        KeyPair keyPair = response.getKeyPair();

        String privateKey = keyPair.getKeyMaterial();

        try (FileWriter writer = new FileWriter(privateKeyFile)) {
            writer.write(privateKey);
        }

        registerKeyPair(accountId, keyPairName, privateKeyFile.getAbsolutePath());

        /*
         * SSH requires our private key be locked down.
         */
        try {
            /*
             * TODO: We should model these platform differences better (and
             * support windows).
             */
            Runtime.getRuntime().exec("chmod 600 " + privateKeyFile.getAbsolutePath());
        } catch ( IOException e ) {
            Status status = new Status(Status.WARNING, Ec2Plugin.PLUGIN_ID,
                    "Unable to restrict permissions on private key file: " + e.getMessage(), e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }

    }

    /**
     * Requests a new key pair from EC2 with the specified name, and saves the
     * private key portion in the specified directory.
     *
     * @param accountId
     *            The account id that owns the key name
     * @param keyPairName
     *            The name of the requested key pair.
     * @param keyPairDirectory
     *            The directory in which to save the private key file.
     * @throws IOException
     *             If any problems were encountered storing the private key to
     *             disk.
     * @throws AmazonClientException
     *             If any problems were encountered requesting a new key pair
     *             from EC2.
     */
    public void createNewKeyPair(String accountId, String keyPairName, String keyPairDirectory) throws IOException, AmazonClientException {
        createNewKeyPair(accountId, keyPairName, keyPairDirectory, null);
    }

    /**
     * Returns the default directory where the plugin assumes private keys are
     * stored.
     *
     * @return The default directory where the plugin assumes private keys are
     *         stored.
     */
    public static File getDefaultPrivateKeyDirectory() {
        String userHomeDir = System.getProperty("user.home");
        if ( userHomeDir == null || userHomeDir.length() == 0 )
            return null;

        return new File(userHomeDir + File.separator + ".ec2");
    }

    /**
     * Registers an existing key pair and private key file with this key pair
     * manager. This method is only for *existing* key pairs. If you need a new
     * key pair created, you should be using createNewKeyPair.
     *
     * @param accountId
     *            The account id that owns the key name
     * @param keyName
     *            The name of the key being registered.
     * @param privateKeyFile
     *            The path to the private key file for the specified key pair.
     * @throws IOException
     *             If any problems are encountered adding the specified key pair
     *             to the mapping of registered key pairs.
     */
    public void registerKeyPair(String accountId, String keyName, String privateKeyFile) throws IOException {
        Properties registeredKeyPairs = loadRegisteredKeyPairs(accountId);
        registeredKeyPairs.put(keyName, privateKeyFile);
        storeRegisteredKeyPairs(accountId, registeredKeyPairs);
    }

    /**
     * Attempts to convert any legacy private key files by renaming them.
     */
    public static void convertLegacyPrivateKeyFiles() throws IOException {
        String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();
        File pluginStateLocation = Ec2Plugin.getDefault().getStateLocation().toFile();
        File keyPairsFile = new File(pluginStateLocation, getKeyPropertiesFileName(accountId));

        if ( !keyPairsFile.exists() ) {
            File legacyKeyPairsFile = new File(pluginStateLocation, "registeredKeyPairs.properties");
            if ( legacyKeyPairsFile.exists() ) {
                FileUtils.copyFile(legacyKeyPairsFile, keyPairsFile);
            }
        }
    }

    /*
     * Private Interface
     */

    private Properties loadRegisteredKeyPairs(String accountId) throws IOException {
        /*
         * If the plugin isn't running (such as during tests), just return an
         * empty property list.
         */
        Ec2Plugin plugin = Ec2Plugin.getDefault();
        if ( plugin == null )
            return new Properties();

        /*
         * TODO: we could optimize this and only load the registered key pairs
         * on startup and after changes.
         */
        File pluginStateLocation = plugin.getStateLocation().toFile();
        File registeredKeyPairsFile = new File(pluginStateLocation, getKeyPropertiesFileName(accountId));
        registeredKeyPairsFile.createNewFile();

        try (FileInputStream fileInputStream = new FileInputStream(registeredKeyPairsFile)) {
            Properties registeredKeyPairs = new Properties();
            registeredKeyPairs.load(fileInputStream);

            return registeredKeyPairs;
        }
    }

    private void storeRegisteredKeyPairs(String accountId, Properties registeredKeyPairs) throws IOException {
        File pluginStateLocation = Ec2Plugin.getDefault().getStateLocation().toFile();
        File registeredKeyPairsFile = new File(pluginStateLocation, getKeyPropertiesFileName(accountId));
        registeredKeyPairsFile.createNewFile();
        try (FileOutputStream fileOutputStream = new FileOutputStream(registeredKeyPairsFile)) {
            registeredKeyPairs.store(fileOutputStream, null);
        }
    }

    private static String getKeyPropertiesFileName(String accountId) {
        return "registeredKeyPairs." + accountId + ".properties";
    }

}
