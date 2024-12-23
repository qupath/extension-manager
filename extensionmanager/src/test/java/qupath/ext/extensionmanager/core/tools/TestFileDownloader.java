package qupath.ext.extensionmanager.core.tools;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import qupath.ext.extensionmanager.SimpleServer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class TestFileDownloader {

    private static final String FILE_NAME = "file";
    private static final String FILE_CONTENT = "This is a sample file";
    private static SimpleServer server;

    @BeforeAll
    static void setupServer() throws IOException {
        server = new SimpleServer(List.of(new SimpleServer.FileToServe(
                FILE_NAME,
                Objects.requireNonNull(TestFileDownloader.class.getResourceAsStream(FILE_NAME))
        )));
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void Check_Null_URI() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> FileDownloader.downloadFile(
                        null,
                        Files.createTempDirectory(null).resolve(FILE_NAME),
                        progress -> {}
                )
        );
    }

    @Test
    void Check_Null_Output_Path() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> FileDownloader.downloadFile(
                        server.getURI(FILE_NAME),
                        null,
                        progress -> {}
                )
        );
    }

    @Test
    void Check_Null_Progress() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> FileDownloader.downloadFile(
                        server.getURI(FILE_NAME),
                        Files.createTempDirectory(null).resolve(FILE_NAME),
                        null
                )
        );
    }

    @Test
    void Check_Interruptable() {
        Thread.currentThread().interrupt();

        Assertions.assertThrows(
                InterruptedException.class,
                () -> FileDownloader.downloadFile(
                        server.getURI(FILE_NAME),
                        Files.createTempDirectory(null).resolve(FILE_NAME),
                        progress -> {}
                )
        );

        Thread.interrupted(); // reset interrupted status
    }

    @Test
    void Check_Invalid_URI() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> FileDownloader.downloadFile(
                        URI.create("invalid_uri"),
                        Files.createTempDirectory(null).resolve(FILE_NAME),
                        progress -> {}
                )
        );
    }

    @Test
    void Check_Output_File_Already_Exist() throws IOException {
        Path outputPath = Files.createTempDirectory(null).resolve(FILE_NAME);
        Files.createDirectories(outputPath);

        Assertions.assertThrows(
                FileNotFoundException.class,
                () -> FileDownloader.downloadFile(
                        server.getURI(FILE_NAME),
                        outputPath,
                        progress -> {}
                )
        );
    }

    @Test
    void Check_File_Downloadable() {
        Assertions.assertDoesNotThrow(() -> FileDownloader.downloadFile(
                server.getURI(FILE_NAME),
                Files.createTempDirectory(null).resolve(FILE_NAME),
                progress -> {}
        ));
    }

    @Test
    void Check_Downloaded_File_Content() throws IOException, InterruptedException {
        Path outputPath = Files.createTempDirectory(null).resolve(FILE_NAME);

        FileDownloader.downloadFile(
                server.getURI(FILE_NAME),
                outputPath,
                progress -> {}
        );

        Assertions.assertEquals(Files.readString(outputPath), FILE_CONTENT);
    }
}
