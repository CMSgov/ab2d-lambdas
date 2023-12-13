package gov.cms.ab2d.optout;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import static gov.cms.ab2d.optout.OptOutConstants.*;

public class OptOutS3 {

    private OptOutS3() {
    }

    public static final S3Client S3_CLIENT = S3Client.builder()
            .region(S3_REGION)
            .build();

    public static BufferedReader openFileS3(String fileName) {
        var getObjectRequest = GetObjectRequest.builder()
                .bucket(BFD_S3_BUCKET_NAME)
                .key(fileName)
                .build();

        var s3ObjectResponse = S3_CLIENT.getObject(getObjectRequest);
        return new BufferedReader(new InputStreamReader(s3ObjectResponse));
    }

    public static void createResponseOptOutFile(String responseContent) {
        try {
            var objectRequest = PutObjectRequest.builder()
                    .bucket(BFD_S3_BUCKET_NAME)
                    .key(RESPONSE_FILE_NAME + new SimpleDateFormat(RESPONSE_FILE_NAME_PATTERN).format(new Date()))
                    .build();

            S3_CLIENT.putObject(objectRequest, RequestBody.fromString(responseContent));

        } catch (AmazonS3Exception ex) {
            // handle exception
            // Alert?
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
