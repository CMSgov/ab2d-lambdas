package gov.cms.ab2d.audit;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import static gov.cms.ab2d.audit.FileUtil.delete;
import static gov.cms.ab2d.audit.FileUtil.findAllMatchingFilesAndParentDirs;
import static gov.cms.ab2d.audit.FileUtil.findMatchingDirectories;

public class AuditEventHandler implements RequestStreamHandler {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12}");

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger log = context.getLogger();
        log.log("Audit Lambda started");
        Properties properties = PropertiesUtil.loadProps();
        int fileTTL = Integer.parseInt(properties.getProperty("audit.files.ttl.hours"));
        String efs = properties.getProperty("AB2D_EFS_MOUNT");
        Set<File> files = findMatchingDirectories(efs, UUID_PATTERN);
        findAllMatchingFilesAndParentDirs(efs, files, ".ndjson");
        files.forEach(file -> deleteExpired(file, fileTTL, log));
        outputStream.write("{\"status\": \"ok\" }".getBytes(StandardCharsets.UTF_8));
        log.log("Audit Lambda completed");
    }

    private void deleteExpired(File file, int fileTTL, LambdaLogger log) {
        try {
            if (file.exists() && fileOldEnough(file, fileTTL)) {
                File dir = null;
                if (!file.isDirectory()) {
                    dir = file.getParentFile();
                    delete(file, log);
                }
                if (dir != null && dir.isDirectory() && Optional.ofNullable(dir.listFiles())
                        .orElse(new File[]{}).length == 0) {
                    delete(dir, log);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean fileOldEnough(File file, int fileTTL) throws IOException {
        return ((FileTime) Files.getAttribute(file.getAbsoluteFile()
                .toPath(), "creationTime")).toInstant()
                .isBefore(Instant.now()
                        .minus(fileTTL, ChronoUnit.HOURS));
    }

}
