package gov.cms.ab2d.optout;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class OptOutProducerTest {
    private final LambdaLogger logger = Mockito.mock(LambdaLogger.class);
    private final CountDownLatch latch = Mockito.mock(CountDownLatch.class);

    private final String path = "/tmp/optout/";

    @BeforeEach
    public void before() throws IOException {
        Path dest = Paths.get(path);
        Files.createDirectories(dest);
    }

    @AfterEach
    public void cleanup() throws IOException {
        File optOutDir = new File(path);
        if (optOutDir.exists()) {
            FileUtils.cleanDirectory(optOutDir);
            FileUtils.forceDelete(optOutDir);
        }
    }

    @Test
     void optOutProducerRunTest() throws URISyntaxException, IOException {
        BlockingQueue<OptOutMessage> queue = new LinkedBlockingQueue<>();
        copyFileFromResources("optOutDummy.txt");

        new OptOutProducer(path, queue, latch, logger).run();

        assertEquals(3, queue.size()); // 1 for poisonPill
        OptOutMessage message = queue.poll();
        assertNotNull(message);
        assertEquals("7GU6ME5FA64", message.getOptOutInformation().getMbi());
        assertFalse(message.getOptOutInformation().isOptOut());
        assertFalse(message.isPoisonPill());
        message = queue.poll();
        assertNotNull(message);
        assertEquals("9GE7P86TE35", message.getOptOutInformation().getMbi());
        assertTrue(message.getOptOutInformation().isOptOut());
        assertFalse(message.isPoisonPill());
        message = queue.poll();
        assertNotNull(message);
        assertNull(message.getOptOutInformation());
        assertTrue(message.isPoisonPill());

        verify(latch).countDown();

    }

    @Test
     void invalidLineTest() throws URISyntaxException, IOException {
        BlockingQueue<OptOutMessage> queue = new LinkedBlockingQueue<>();
        copyFileFromResources("invalidLine.txt");
        assertDoesNotThrow(() -> new OptOutProducer(path, queue, latch, logger).run());
        assertEquals(2, queue.size()); // 1 valid lines and 1 poisonPill
    }

    @Test
    void noFileToParseTest() {
        BlockingQueue<OptOutMessage> queue = new LinkedBlockingQueue<>();
        assertDoesNotThrow(() -> new OptOutProducer(path, queue, latch, logger).run());
        assertEquals(1, queue.size()); // 1 poisonPill
    }

    @Test
    void optOutProducerInterruptedExceptionTest() throws InterruptedException {
        OptOutProducer producer = Mockito.mock(OptOutProducer.class);
        doThrow(new InterruptedException()).when(producer).process();
        assertDoesNotThrow(producer::run);
    }

    @Test
    void optOutProducerIOExceptionTest() throws InterruptedException, IOException {
        OptOutProducer p = Mockito.mock(OptOutProducer.class);
        doThrow(new IOException()).when(p).parseFile(any());
        assertDoesNotThrow(p::run);
    }
    @Test
    void temporaryFileDeletionTest() throws URISyntaxException, IOException {
        copyFileFromResources("optOutDummy.txt");
        Path p = Paths.get(path);
        OptOutUtils.deleteDirectoryRecursion(p);
        assertFalse(Files.exists(p));
    }

    private void copyFileFromResources(String fileName) throws URISyntaxException, IOException {
        Path pathIn = Paths.get(Objects.requireNonNull(getClass().getResource("/" + fileName)).toURI());
        Path pathOut = Paths.get(path + fileName);
        Files.copy(pathIn, pathOut, StandardCopyOption.REPLACE_EXISTING);
    }
}
