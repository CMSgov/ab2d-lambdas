package gov.cms.ab2d.optout;


import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.*;

import java.util.List;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;


public class OptOutS3 {

    private OptOutS3() {
    }

    public static final String BFD_S3_BUCKET_NAME = "ab2d-opt-out-temp-349849222861-us-east-1";

    public static final String AB2D_S3_BUCKET_NAME = "ab2d-opt-out-temp-349849222861-us-east-1";

    public static final Region S3_REGION = Region.US_EAST_1;
    public static final String FILE_PATH = "/opt/optout/";
    private static final S3AsyncClient S3_CLIENT = S3AsyncClient.builder()
            .region(S3_REGION)
            .build();

    private static final S3TransferManager TRANSFER_MANAGER = S3TransferManager.builder()
            .s3Client(S3_CLIENT)
            .build();

    public static void downloadFilesToDirectory(LambdaLogger logger) {
        DirectoryDownload directoryDownload =
                TRANSFER_MANAGER.downloadDirectory(DownloadDirectoryRequest.builder()
                        .destination(Paths.get(FILE_PATH))
                        .bucket(BFD_S3_BUCKET_NAME)
                        .build());
        try {
            CompletedDirectoryDownload completedDirectoryDownload = directoryDownload.completionFuture().join();

            completedDirectoryDownload.failedTransfers().forEach(fail ->
                    logger.log("Object failed to transfer. " + fail.toString()));
        } catch (CompletionException ex) {
            logger.log(ex.getMessage());
        }
    }

}
