package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.optout.OptOutConstants.*;

public class OptOutS3 {
    private final S3Client s3Client;
    private final String fileName;
    private final LambdaLogger logger;

    public OptOutS3(S3Client s3Client, String fileName, LambdaLogger logger) {
        this.s3Client = s3Client;
        this.fileName = fileName;
        this.logger = logger;
    }

    public BufferedReader openFileS3() {
        try {
            //Checking if object exists
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(BFD_S3_BUCKET_NAME)
                    .key(fileName)
                    .build();

            s3Client.headObject(headObjectRequest);

            var getObjectRequest = GetObjectRequest.builder()
                    .bucket(BFD_S3_BUCKET_NAME)
                    .key(fileName)
                    .build();

            var s3ObjectResponse = s3Client.getObject(getObjectRequest);
            return new BufferedReader(new InputStreamReader(s3ObjectResponse));
        } catch (SdkClientException ex) {
            var errorMessage = "Unable to load credentials to connect S3 bucket";
            logger.log(errorMessage);
            throw new OptOutException(errorMessage, ex);
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                var errorMessage = "Object " + fileName + " does not exist. " + ex.getMessage();
                logger.log(errorMessage);
                throw new OptOutException(errorMessage, ex);
            } else {
                logger.log(ex.getMessage());
                throw ex;
            }
        }
    }

    public String createResponseOptOutFile(String responseContent) {
        try {
            var key = RESPONSE_FILE_NAME + new SimpleDateFormat(RESPONSE_FILE_NAME_PATTERN).format(new Date());
            var objectRequest = PutObjectRequest.builder()
                    .bucket(BFD_S3_BUCKET_NAME)
                    .key(key)
                    .build();

            s3Client.putObject(objectRequest, RequestBody.fromString(responseContent));
            return key;
        } catch (AmazonS3Exception ex) {
            var errorMessage = "Response OptOut file cannot be created. ";
            logger.log(errorMessage + ex.getMessage());
            throw new OptOutException(errorMessage, ex);
        }
    }

    //ToDo: AB2D-5796 Delete Opt-out file from S3 (inbound)
//    public static void deleteFileFromS3() {
//        // The map always contains 2 lines: the first and last from the optout file.
//        if (optOutResultMap.size() == 2) {
//            try {
//                var request = DeleteObjectRequest.builder()
//                        .bucket(BFD_S3_BUCKET_NAME)
//                        .key(fileName)
//                        .build();
//
//                S3_CLIENT.deleteObject(request);
//            } catch (SdkClientException ex) {
//                logger.log(ex.getMessage());
//            }
//        }
//        else {
//            // Slack alert
//        }
//    }


}
