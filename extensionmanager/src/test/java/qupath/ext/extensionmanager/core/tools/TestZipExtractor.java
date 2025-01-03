package qupath.ext.extensionmanager.core.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

public class TestZipExtractor {

    private static final Map<String, String> FILE_IN_ARCHIVE_TO_CONTENT = Map.of(
            "file1", "This is the content of file 1",
            "file2", "This is the content of file 2"
    );

    @Test
    void Check_Extraction_When_Invalid_Input_Path() {
        Assertions.assertThrows(
                IOException.class,
                () -> ZipExtractor.extractZipToFolder(
                        Paths.get("/invalid_path"),
                        Files.createTempDirectory(null),
                        progress -> {}
                )
        );
    }

    @Test
    void Check_Extraction_When_Invalid_Output_Path() {
        Assertions.assertThrows(
                IOException.class,
                () -> ZipExtractor.extractZipToFolder(
                        Paths.get(Objects.requireNonNull(TestZipExtractor.class.getResource("archive.zip")).getPath()),
                        Paths.get("/invalid_path"),
                        progress -> {}
                )
        );
    }

    @Test
    void Check_Extraction_Content() throws IOException, InterruptedException {
        Path outputFolder = Files.createTempDirectory(null);

        ZipExtractor.extractZipToFolder(
                Paths.get(Objects.requireNonNull(TestZipExtractor.class.getResource("archive.zip")).getPath()),
                outputFolder,
                progress -> {}
        );

        for (var entry: FILE_IN_ARCHIVE_TO_CONTENT.entrySet()) {
            Assertions.assertEquals(
                    entry.getValue(),
                    Files.readString(outputFolder.resolve(entry.getKey()))
            );
        }
    }
}
