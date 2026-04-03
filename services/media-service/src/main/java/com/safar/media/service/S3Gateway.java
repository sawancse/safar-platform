package com.safar.media.service;

public interface S3Gateway {
    /**
     * Generates a presigned PUT URL valid for 15 minutes.
     * @return the presigned upload URL
     */
    String generatePresignedUrl(String s3Key, String contentType);

    byte[] download(String s3Key);

    void upload(String s3Key, byte[] data, String contentType);
}
