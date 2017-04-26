package com.amazonaws.eclipse.opsworks.deploy.wizard.model;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.opsworks.model.Source;
import com.amazonaws.services.opsworks.model.SourceType;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.util.SdkHttpUtils;

public class S3ApplicationSource {

    public static String BUCKET_NAME = "bucketName";
    public static String KEY_NAME = "keyName";
    public static String REGION = "region";
    public static String AS_PUBLIC_HTTP_ARCHIVE = "asPublicHttpArchive";

    private String bucketName;
    private String keyName;
    private Region region;
    private boolean asPublicHttpArchive = true; // always true when creating new apps

    /**
     * @return null if the specified application source is not supported.
     */
    public static S3ApplicationSource parse(Source source) {
        S3ApplicationSource s3Source = new S3ApplicationSource();

        if ("archive".equals(source.getType())) {
            s3Source.setAsPublicHttpArchive(true);
        } else if ("s3".equals(source.getType())) {
            s3Source.setAsPublicHttpArchive(false);
        } else {
            return null;
        }

        // parse the s3 URL
        AmazonS3URI s3Uri = null;
        try {
            s3Uri = new AmazonS3URI(source.getUrl());
        } catch (Exception e) {
            return null;
        }

        if (s3Uri.getBucket() == null || s3Uri.getBucket().isEmpty()) {
            return null;
        }
        if (s3Uri.getKey() == null || s3Uri.getKey().isEmpty()) {
            return null;
        }
        if (s3Uri.getRegion() != null && RegionUtils.getRegion(s3Uri.getRegion()) == null) {
            return null;
        }

        s3Source.setBucketName(s3Uri.getBucket());
        s3Source.setKeyName(s3Uri.getKey());
        if (s3Uri.getRegion() != null) {
            s3Source.setRegion(RegionUtils.getRegion(s3Uri.getRegion()));
        }

        return s3Source;
    }

    /**
     * @return the Source object that can be used in a CreateApp request
     */
    public Source toGenericSource() {
        Source source = new Source();

        source.setType(asPublicHttpArchive ? SourceType.Archive : SourceType.S3);
        source.setUrl(String.format(
                "http://%s.s3.amazonaws.com/%s",
                SdkHttpUtils.urlEncode(bucketName, false),
                SdkHttpUtils.urlEncode(keyName, true)));

        return source;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public boolean isAsPublicHttpArchive() {
        return asPublicHttpArchive;
    }

    public void setAsPublicHttpArchive(boolean asPublicHttpArchive) {
        this.asPublicHttpArchive = asPublicHttpArchive;
    }

    @Override
    public String toString() {
        return String.format(
                "{ region=%s, bucket=%s, key=%s, isPublic=%s, url=%s }",
                region == null ? null : region.getName(),
                bucketName, keyName, asPublicHttpArchive,
                toGenericSource().getUrl());
    }
}
