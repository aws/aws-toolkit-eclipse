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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.amazon.aws.samplecode.travellog.entity.Photo;
import com.amazon.aws.samplecode.travellog.util.Configuration;

/**
 * This is a utility class that handles the logic of storing and removing photos
 * from S3.  When storing it will take the original photo and create two resized
 * versions: a thumbnail, and a "websize" version.  It will then store the original,
 * and the resized images on S3.
 *
 * These resized images are an ideal case for using S3's reduced redundancy storage.
 * The images, if lost, can be regenerated from the original image which will be stored
 * with full redundancy.  This way there is some cost savings without risking
 * true data loss since we can always regenerate the lost data.
 */
public class S3PhotoUtil {

    private static final String FULLSIZE_SUFFIX = ".jpg";
    private static final String WEBSIZE_SUFFIX = "_web.jpg";
    private static final String THUMB_SUFFIX = "_thumb.jpg";

    //These sizes are hard coded for now, but could be customized for a more
    //dynamic site interface
    private static final int THUMBNAIL_LONG_EDGE=150;
    private static final int WEBSIZE_LONG_EDGE=600;

    //The base name for the photo bucket.  We will create a truly unique name
    //by prepending the AWS key
    private static final String PHOTO_BUCKET="travellog-photo";

    private static String uniqueBucketName;

    //Initialize the unique bucket name
    static {
        Configuration config = Configuration.getInstance();
        uniqueBucketName = (config.getProperty("accessKey")+"-"+PHOTO_BUCKET).toLowerCase(); //AWSSDK requires bucket name to be lower case
    }

    private static Logger logger = Logger.getLogger(S3PhotoUtil.class.getName());


    private S3PhotoUtil () {}

    /**
     * Deletes a photo from S3 based on the paths stored in the Photo object.
     * This will delete both the original photo and the resized versions
     * that were created during the storage process.
     *
     * @param photo the photo to be deleted
     */
    public static void deletePhotoFromS3 (Photo photo) {

        if (photo.getOriginalPath()!=null) {
            deleteFromPath(photo.getId()+FULLSIZE_SUFFIX);
        }
        if (photo.getThumbnailPath()!=null) {
            deleteFromPath(photo.getId()+THUMB_SUFFIX);
        }
        if (photo.getWebsizePath()!=null) {
            deleteFromPath(photo.getId()+WEBSIZE_SUFFIX);
        }
    }

    /**
     * A convenience method to create a storage object for the specified path
     * and then delete that object from S3
     * @param path the path to the s3 object to be deleted
     */
    private static void deleteFromPath (String path) {
        S3StorageManager mgr = new S3StorageManager();
        TravelLogStorageObject obj = new TravelLogStorageObject();
        obj.setBucketName(uniqueBucketName);
        obj.setStoragePath(path);
        mgr.delete(obj);
    }


    /**
     * Loads original photo from S3, returning the raw image data.
     * @param photo the photo object specifying storage path of the original photo
     * @return raw image data
     * @throws IOException
     */
    public static InputStream loadOriginalPhoto (Photo photo) throws IOException {
        S3StorageManager mgr = new S3StorageManager();
        TravelLogStorageObject obj = new TravelLogStorageObject();
        obj.setBucketName(uniqueBucketName);
        obj.setStoragePath(photo.getId()+FULLSIZE_SUFFIX);
        return mgr.loadInputStream(obj);
    }


    /**
     * Stores a photo on S3 and then updates the underlying Photo object with
     * the URL's that point to the image locations on S3.
     *
     * The scaling process creates three separate images, a web size, a thumbnail,
     * and then the untouched original upload.  The original is stored using full
     * S3 redundancy, but the thumbnail and web size versions are stored using
     * reduced redundancy.  In the event of a loss of reduced redundancy data, the
     * thumbnail and websize data could be regenerated if need be but that's not
     * implemented here.
     *
     * @param photo the photo data to be stored (should be initialized with a photo ID before being passed in)
     * @param photoData raw data for the photo itself
     * @throws IOException
     */
    public static Photo storePhoto ( Photo photo, byte [] photoData) throws IOException {

        //Store various photo sizes
        String thumbnailPath = storeThumbnail(photo, photoData);
        photo.setThumbnailPath(thumbnailPath);

        String websizePath = storeWebsize(photo,photoData);
        photo.setWebsizePath(websizePath);

        String originalPath = storeOriginal(photo,photoData);
        photo.setOriginalPath(originalPath);

        return photo;
    }

    /**
     * Scales the incoming photo data to a thumbnail size, stores it on S3, and then
     * returns the storage path for the photo on S3.  This method uses reduced
     * redundancy storage to reduce storage costs.  Should there be a loss of the
     * thumbnail data on S3, it could be regenerated from the original full size image.
     *
     * @param photo metadata for the photo
     * @param photoData the raw data from the photo
     * @return storage path used on S3 (derived from the id of the photo and a predetermined suffix)
     * @throws IOException
     */
    private static String storeThumbnail (Photo photo, byte [] photoData) throws IOException {
        byte [] thumbnail = scalePhoto(THUMBNAIL_LONG_EDGE, photoData);
        TravelLogStorageObject obj = getStorageObject(thumbnail,photo.getId()+THUMB_SUFFIX);
        S3StorageManager mgr = new S3StorageManager();
        mgr.storePublicRead(obj, true);
        return obj.getAwsUrl();
    }

    /**
     * Scales the incoming photo data to a web size, stores it on S3, and then
     * returns the storage path for the photo on S3.  This method uses reduced
     * redundancy storage to reduce storage costs.  Should there be a loss of the
     * data on S3, it could be regenerated from the original full size image.
     *
     * @param photo metadata for the photo
     * @param photoData the raw data from the photo
     * @return storage path used on S3 (derived from the id of the photo and a predetermined suffix)
     * @throws IOException
     */
    private static String storeWebsize (Photo photo, byte [] photoData) throws IOException {
        byte [] websize = scalePhoto(WEBSIZE_LONG_EDGE, photoData);
        TravelLogStorageObject obj = getStorageObject(websize,photo.getId()+WEBSIZE_SUFFIX);
        S3StorageManager mgr = new S3StorageManager();
        mgr.storePublicRead(obj, true);
        return obj.getAwsUrl();
    }

    /**
     * Stores the original full size image on S3, and then
     * returns the storage path for the photo on S3.  This method uses full
     * redundancy storage because if we lose the original there's no way to recreate it.
     * Also, in the event of data loss of a thumbnail or web size of the original, this
     * could be used to recreate the smaller images.
     *
     * @param photo metadata for the photo
     * @param photoData the raw data from the photo
     * @return storage path used on S3 (derived from the id of the photo and a predetermined suffix)
     * @throws IOException
     */
    private static String storeOriginal (Photo photo, byte [] photoData) throws IOException {
        TravelLogStorageObject obj = getStorageObject(photoData,photo.getId()+FULLSIZE_SUFFIX);
        S3StorageManager mgr = new S3StorageManager();
        mgr.storePublicRead(obj, false);
        return obj.getAwsUrl();
    }

    /**
     * Convenience method to construct a TravelLogStorageObject containing the information
     * we need to store a photo on S3
     * @param data the raw photo data
     * @param storagePath the path to use for storing the photo on s3
     * @return
     */
    private static TravelLogStorageObject getStorageObject (byte [] data, String storagePath) {
        TravelLogStorageObject obj = new TravelLogStorageObject();
        obj.setData(data);
        obj.setBucketName(uniqueBucketName);
        obj.setStoragePath(storagePath);
        return obj;
    }

    /**
     * This method will scale the photo data to the specified size.
     * It will only decrease the size of the photo, not
     * increase it.
     *
     * @param longEdgeSize
     * @param photoData the raw binary photo data
     * @return the resized photo data
     * @throws IOException
     */
    private static byte [] scalePhoto (int longEdgeSize, byte [] photoData) throws IOException {

        ByteArrayInputStream dataStream = new ByteArrayInputStream(photoData);
        ImageInputStream iis = ImageIO.createImageInputStream(dataStream);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

        //Determine image format
        final String formatName;
        if (readers.hasNext()) {
            ImageReader reader = readers.next();
            formatName = reader.getFormatName();
        }
        else {
            logger.log(Level.SEVERE,"Unsupported image type");
            return null;
        }

        BufferedImage image = ImageIO.read(iis);

        int baseWidth = image.getWidth();
        int baseHeight = image.getHeight();

        float aspectRatio = (float)baseHeight/(float)baseWidth;

        final int modWidth;
        final int modHeight;

        if (baseWidth>baseHeight) {
            //width is long edge so scale based on that
            modWidth = longEdgeSize;
            modHeight = Math.round(aspectRatio*longEdgeSize);
        }
        else {
            modHeight = longEdgeSize;
            modWidth = Math.round(aspectRatio*longEdgeSize);
        }

        return scalePhoto(modWidth,modHeight, image, formatName);
    }


    /**
     * This method will scale the photo data passed in through the constructor
     * to the specified size.  It will only decrease the size of the photo, not
     * increase it.
     *
     * @param targetWidth width to scale to
     * @param targetHeight height to scale to
     * @param image the BufferedImage that contains the raw image data
     * @param formatName the name of the format for the image (typically "jpeg")
     * @return the resized photo data
     * @throws IOException
     */
    private static byte [] scalePhoto (int targetWidth, int targetHeight, BufferedImage image, String formatName) throws IOException {

        ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
        if (targetWidth>image.getWidth() || targetHeight>image.getHeight()) {
            //we don't want to scale up.  If it's smaller just leave it alone
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, formatName,out);
            return out.toByteArray();
        }
        else {
            BufferedImage scaledImage = scalePhoto(image,targetWidth,targetHeight);
            ImageIO.write(scaledImage,formatName,imageOut);
            return imageOut.toByteArray();
        }
    }

    /**
     * private method that does the heavy lifting for scaling photos.
     * It doesn't make sense to call directly as we need to specify the
     * jpeg encoding details, etc, before we render the new image.
     *
     * @param img the BufferedImage object containing the photo to be scaled
     * @param targetWidth width to scale to
     * @param targetHeight height to scale to
     * @return the BufferedImage object that contains the resized photo data
     */
    private static BufferedImage scalePhoto (BufferedImage img, int targetWidth,
            int targetHeight) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;

        int width = img.getWidth();
        int height = img.getHeight();

        /**
         * A quirk of the image scaling algorithm is that if you scale from a very large
         * image to a small image in one step, the quality of the scaling degrades quickly
         * leading to a noisy photo.  This process instead scales a photo down in smaller
         * increments in a loop until the right size is achieved.
         */
        while (width != targetWidth || height != targetHeight) {
            if (width > targetWidth) {
                width = width/2;
                if (width < targetWidth) {
                    width = targetWidth;
                }
            }

            if ( height > targetHeight) {
                height = height/2;
                if (height< targetHeight) {
                    height = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(width, height, type);
            Graphics2D graphics = tmp.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(ret, 0, 0, width, height, null);
            graphics.dispose();

            ret = tmp;
        }

        return ret;
    }

}
