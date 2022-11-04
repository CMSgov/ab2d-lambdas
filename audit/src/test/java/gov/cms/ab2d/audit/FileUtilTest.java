package gov.cms.ab2d.audit;

import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;

class FileUtilTest {
    @Test
    void deleteFile() throws IOException {
        String path = System.getProperty("java.io.tmpdir") + "/" + RandomString.make(10) + ".test";
        Files.write(Paths.get(path), "test".getBytes(StandardCharsets.UTF_8));
        File file = new File(path);
        TestContext context = new TestContext();
        FileUtil.delete(file, context.getLogger());
        assertFalse(file.exists());
        Mockito.verifyNoInteractions(context.getLogger());
    }

    @Test
    void findFiles() throws IOException {
        String path = System.getProperty("java.io.tmpdir") + "/" + RandomString.make(10) + ".test";
        Files.write(Paths.get(path), "test".getBytes(StandardCharsets.UTF_8));
        File file = new File(path);
        TestContext context = new TestContext();
        FileUtil.delete(file, context.getLogger());
        assertFalse(file.exists());
        Mockito.verifyNoInteractions(context.getLogger());
    }

}
