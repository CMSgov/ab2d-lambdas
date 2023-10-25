package gov.cms.ab2d.optout;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;


public class OptOutS3 implements Runnable {

    private final boolean isLocal;
    private final CountDownLatch latch;

    private final LambdaLogger logger;

    public OptOutS3(boolean isLocal, CountDownLatch latch, LambdaLogger logger){
        this.isLocal = isLocal;
        this.latch = latch;
        this.logger = logger;
    }

    private static final AmazonS3 S3_CLIENT = AmazonS3ClientBuilder.standard()
            .withRegion(OptOutUtils.S3_REGION)
            .build();
    @Override
    public void run() {
        if(isLocal) downloadFileFromS3();
        else copyFilesFromS3();
    }

    private void downloadFileFromS3() {
        logger.log("Downloading " + OptOutUtils.FILE_NAME + " from S3 bucket " + OptOutUtils.BFD_S3_BUCKET_NAME);
        try {
            S3ObjectInputStream s3is = S3_CLIENT.getObject(OptOutUtils.BFD_S3_BUCKET_NAME, OptOutUtils.FILE_NAME).getObjectContent();
            FileOutputStream fos = new FileOutputStream(OptOutUtils.FILE_PATH);
            byte[] read_buf = new byte[1024];
            int read_len;
            while ((read_len = s3is.read(read_buf)) > 0) {
                fos.write(read_buf, 0, read_len);
            }
            s3is.close();
            fos.close();
        } catch (AmazonServiceException ex) {
            logger.log(ex.getErrorMessage());
            throw new OptOutException(ex);
        } catch (IOException ex) {
            logger.log(ex.getMessage());
            throw new OptOutException(ex);
        }
        finally {
            latch.countDown();
        }
    }

    private void copyFilesFromS3(){
        try {
            S3Objects.inBucket(S3_CLIENT, OptOutUtils.BFD_S3_BUCKET_NAME).forEach((S3ObjectSummary objectSummary) -> {
                String key = objectSummary.getKey();
                CopyObjectRequest copyObjRequest = new CopyObjectRequest(OptOutUtils.BFD_S3_BUCKET_NAME, key, OptOutUtils.AB2D_S3_BUCKET_NAME, "new"+key);
                S3_CLIENT.copyObject(copyObjRequest);
            });
        } catch (AmazonServiceException ex) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            logger.log(ex.getMessage());
            throw new OptOutException(ex);
        } catch (SdkClientException ex) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            logger.log(ex.getMessage());
            throw new OptOutException(ex);
        }
        finally {
            latch.countDown();
        }
    }
}
