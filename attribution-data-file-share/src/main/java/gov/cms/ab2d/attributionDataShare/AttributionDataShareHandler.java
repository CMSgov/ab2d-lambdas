package gov.cms.ab2d.attributionDataShare;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import gov.cms.ab2d.lambdalibs.lib.FileUtil;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static gov.cms.ab2d.attributionDataShare.AttributionDataShareConstants.*;

public class AttributionDataShareHandler implements RequestStreamHandler {

    // Writes out a file to the FILE_PATH.
    // I.E: "P.AB2D.NGD.REQ.D240209.T1122001"

    String select1 = "SELECT mbi,effective_date,opt_out_flag FROM (\n" +
            "  SELECT *, ROW_NUMBER() OVER (ORDER BY mbi DESC) AS row_num\n" +
            "  FROM current_mbi\n" +
            ") subquery\n" +
            "WHERE row_num <= 10000";

    String select2 = "SELECT mbi,effective_date,opt_out_flag FROM (\n" +
            "  SELECT *, ROW_NUMBER() OVER (ORDER BY mbi DESC) AS row_num\n" +
            "  FROM current_mbi\n" +
            ") subquery\n" +
            "WHERE row_num > 10000";

    private static BufferedWriter bufferedWriter;

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        logger.log("AttributionDataShare Lambda is started");

        String currentDate = new SimpleDateFormat(REQ_FILE_NAME_PATTERN).format(new Date());
        String fileName = REQ_FILE_NAME + currentDate;
        String fileFullPath = FILE_PATH + fileName;
        var parameterStore = AttributionParameterStore.getParameterStore();
        AttributionDataShareHelper helper = helperInit(fileName, fileFullPath, logger);

        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        try (var dbConnection = DriverManager.getConnection(parameterStore.getDbHost(), parameterStore.getDbUser(), parameterStore.getDbPassword())){
            long startSelect = System.currentTimeMillis();

            executorService.execute(new Utils(fileFullPath, select1, dbConnection, getWriter(fileFullPath), latch, logger));
            executorService.execute(new Utils(fileFullPath, select2, dbConnection, getWriter(fileFullPath), latch, logger));

            latch.await();

            long finishSelect = System.currentTimeMillis();

            logger.log("Total Select TIME ms: ---------- " + (finishSelect - startSelect));
         //   helper.mtpUpload(getAsyncS3Client(ENDPOINT, parameterStore));

        } catch (NullPointerException | SQLException ex) {
            throwAttributionDataShareException(logger, ex);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            FileUtil.deleteDirectoryRecursion(Paths.get(fileFullPath));
            logger.log("AttributionDataShare Lambda is completed");
        }
    }

    private static synchronized BufferedWriter getWriter(String fileFullPath)
    {
        try{
            if( bufferedWriter == null )
            {
                bufferedWriter =  new BufferedWriter(new FileWriter(fileFullPath, true));
            }

            return bufferedWriter;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }



    public S3AsyncClient getAsyncS3Client(String endpoint, AttributionParameterStore parameterStore) throws URISyntaxException {
        var client = S3AsyncClient.crtCreate();


        if (endpoint.equals(ENDPOINT)) {
            var stsClient = StsClient
                    .builder()
                    .region(S3_REGION)
                    .build();

            var request = AssumeRoleRequest
                    .builder()
                    .roleArn(parameterStore.getRole())
                    .roleSessionName("roleSessionName")
                    .build();

            var credentials = StsAssumeRoleCredentialsProvider
                    .builder()
                    .stsClient(stsClient)
                    .refreshRequest(request)
                    .build();

            client =
                    S3AsyncClient.crtBuilder()
                            .credentialsProvider(credentials)
                            .region(S3_REGION)
                            .targetThroughputInGbps(20.0)
                            .minimumPartSizeInBytes(8 * 1025 * 1024L)
                            .build();
        }
        return client;
    }

    AttributionDataShareHelper helperInit(String fileName, String fileFullPath, LambdaLogger logger) {
        return new AttributionDataShareHelper(fileName, fileFullPath, logger);
    }

    void throwAttributionDataShareException(LambdaLogger logger, Exception ex) {
        logger.log(ex.getMessage());
        throw new AttributionDataShareException(ex.getMessage(), ex);
    }

}
