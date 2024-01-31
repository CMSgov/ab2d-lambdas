package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static gov.cms.ab2d.optout.OptOutConstants.TEST_FILE_NAME;
import static gov.cms.ab2d.optout.S3MockAPIExtension.S3_CLIENT;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith({S3MockAPIExtension.class})
public class OptOutS3Test {

    private static OptOutS3 OPT_OUT_S3;

    @BeforeEach
    public void beforeEach() throws IOException {
        S3MockAPIExtension.createFile(Files.readString(Paths.get("src/test/resources/" + TEST_FILE_NAME), StandardCharsets.UTF_8));
        OPT_OUT_S3 = new OptOutS3(S3_CLIENT, TEST_FILE_NAME, mock(LambdaLogger.class));
    }

    @AfterEach
    public void afterEach() {
        S3MockAPIExtension.deleteFile(TEST_FILE_NAME);
    }

    @Test
    void openFileS3Test() {
        Assertions.assertNotNull(OPT_OUT_S3.openFileS3());
    }

    @Test
    void openFileS3ExceptionTest() {
        S3MockAPIExtension.deleteFile(TEST_FILE_NAME);
        assertThrows(OptOutException.class, () -> OPT_OUT_S3.openFileS3());
    }

    @Test
    void createResponseOptOutFileTest() {
        var key = OPT_OUT_S3.createResponseOptOutFile("text");
        assertTrue(S3MockAPIExtension.isObjectExists(key));
        S3MockAPIExtension.deleteFile(key);
    }
}
