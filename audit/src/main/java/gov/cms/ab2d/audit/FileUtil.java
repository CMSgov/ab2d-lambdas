package gov.cms.ab2d.audit;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileUtil {
    public static void delete(File file, LambdaLogger logger) {
        try {
            Files.delete(file.toPath());
            logger.log(file.getName() + " deleted");
        } catch (IOException exception) {
            exception.printStackTrace();
            logger.log(exception.getMessage());
            logger.log(String.format("File/directory %s could not be deleted", file.getAbsolutePath()));
        }
    }

    public static void findAllMatchingFilesAndParentDirs(String directoryName, Set<File> files, String endsWith) {
        File directory = new File(directoryName);
        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if (fList != null)
            for (File file : fList) {
                if (file.getName()
                        .endsWith(endsWith)) {
                    files.add(file);
                }
                if (file.isDirectory()) {
                    findAllMatchingFilesAndParentDirs(file.getAbsolutePath(), files, endsWith);
                }
            }
    }

    public static Set<File> findMatchingDirectories(String baseDirectory, Pattern pattern) {
        File directory = new File(baseDirectory);
        File[] fList = directory.listFiles();
        return Arrays.stream(Optional.ofNullable(fList)
                        .orElse(new File[]{}))
                .filter(f -> pattern.matcher(f.getName())
                        .matches())
                .collect(Collectors.toSet());
    }

}
