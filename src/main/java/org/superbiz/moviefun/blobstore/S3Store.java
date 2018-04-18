package org.superbiz.moviefun.blobstore;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.tomcat.util.http.fileupload.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

public class S3Store implements BlobStore{

    AmazonS3Client s3Client;
    String s3BucketName;
    public S3Store(AmazonS3Client s3Client, String s3BucketName) {
        this.s3Client = s3Client;
        this.s3BucketName = s3BucketName;
    }

    @Override
    public void put(Blob blob) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(blob.contentType);
        s3Client.putObject(s3BucketName, blob.name, blob.inputStream, metadata);
    }

    @Override
    public Optional<Blob> get(String name) throws IOException {
        //return Optional.empty();
        S3Object s3Obj = s3Client.getObject(s3BucketName, name);

        byte[] imageBytes = new byte[2 * 4096];
        s3Obj.getObjectContent().read(imageBytes);

        ObjectMetadata metadata = s3Obj.getObjectMetadata();

        Blob retBlob = new Blob(name, new ByteArrayInputStream(imageBytes), metadata.getContentType());

        return Optional.ofNullable(retBlob);
    }

    @Override
    public void deleteAll() {
        //TODO

    }
}
