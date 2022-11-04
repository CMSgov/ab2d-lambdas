package gov.cms.ab2d.audit;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import static gov.cms.ab2d.audit.FileUtil.delete;
import static gov.cms.ab2d.audit.FileUtil.findAllFiles;
import static gov.cms.ab2d.audit.FileUtil.findMatchingDirectories;

public class AuditEventHandler implements RequestStreamHandler {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger log = context.getLogger();
        log.log("Audit Lambda started");
        debug();
        Properties properties = PropertiesUtil.loadProps();
        String efs = properties.getProperty("AB2D_EFS_MOUNT");
        int fileTTL = Integer.parseInt(properties.getProperty("audit.files.ttl.hours"));
        Set<File> files = findMatchingDirectories(efs, UUID_PATTERN);
        findAllFiles(efs, files, ".ndjson");
        files.forEach(file -> {
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
        });
        outputStream.write("ok".getBytes(StandardCharsets.UTF_8));
        log.log("Audit Lambda completed");
    }

    private boolean fileOldEnough(File file, int fileTTL) throws IOException {
        return ((FileTime) Files.getAttribute(file.getAbsoluteFile()
                .toPath(), "creationTime")).toInstant()
                .isBefore(Instant.now()
                        .minus(fileTTL, ChronoUnit.HOURS));
    }


    private void debug() throws IOException {
        FileWriter fileWriter = new FileWriter("/tmp/debug.txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.print(OffsetDateTime.now());
        printWriter.printf("Product name is %s and its price is %d $", "iPhone", 1000);
        printWriter.close();
    }


}
