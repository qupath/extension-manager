package qupath.ext.extensionmanager.core.tools;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.TestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestFilesWatcher {

    private static final Logger logger = LoggerFactory.getLogger(TestFilesWatcher.class);
    private static final int CHANGE_WAITING_TIME_MS = 10000;

    @Test
    void Check_Directory_To_Watch_Property_Null() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> {
                    FilesWatcher filesWatcher = new FilesWatcher(
                            null,
                            path -> true,
                            path -> false
                    );
                    filesWatcher.close();
                }
        );
    }

    @Test
    void Check_Files_To_Find_Null() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> {
                    FilesWatcher filesWatcher = new FilesWatcher(
                            new SimpleObjectProperty<>(),
                            null,
                            path -> false
                    );
                    filesWatcher.close();
                }
        );
    }

    @Test
    void Check_Directories_To_Skip_Null() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> {
                    FilesWatcher filesWatcher = new FilesWatcher(
                            new SimpleObjectProperty<>(),
                            path -> true,
                            null
                    );
                    filesWatcher.close();
                }
        );
    }

    @Test
    void Check_Directory_To_Watch_Null() throws Exception {
        FilesWatcher filesWatcher = new FilesWatcher(
                new SimpleObjectProperty<>(null),
                path -> true,
                path -> false
        );

        Assertions.assertIterableEquals(List.of(), filesWatcher.getFiles());

        filesWatcher.close();
    }

    @Test
    void Check_Files_Added_Before_Watcher_Creation_Detected() throws Exception {
        Path directoryToWatch = Files.createTempDirectory(null);
        List<Path> expectedFiles = List.of(
                Files.createFile(directoryToWatch.resolve("file1")),
                Files.createFile(directoryToWatch.resolve("file2"))
        );

        FilesWatcher filesWatcher = new FilesWatcher(
                new SimpleObjectProperty<>(directoryToWatch),
                path -> true,
                path -> false
        );

        TestUtils.assertCollectionsEqualsWithoutOrder(
                expectedFiles,
                filesWatcher.getFiles()
        );

        filesWatcher.close();
    }

    @Test
    void Check_Files_Added_After_Watcher_Creation_Detected() throws Exception {
        Path directoryToWatch = Files.createTempDirectory(null);

        FilesWatcher filesWatcher = new FilesWatcher(
                new SimpleObjectProperty<>(directoryToWatch),
                path -> true,
                path -> false
        );
        List<Path> expectedFiles = List.of(
                Files.createFile(directoryToWatch.resolve("file1")),
                Files.createFile(directoryToWatch.resolve("file2"))
        );

        TestUtils.assertCollectionsEqualsWithoutOrder(
                expectedFiles,
                filesWatcher.getFiles(),
                CHANGE_WAITING_TIME_MS  // wait for files to be detected
        );

        filesWatcher.close();
    }

    @Test
    void Check_No_Error_When_File_Added_While_Iterating() throws Exception {
        Path directoryToWatch = Files.createTempDirectory(null);

        FilesWatcher filesWatcher = new FilesWatcher(
                new SimpleObjectProperty<>(directoryToWatch),
                path -> true,
                path -> false
        );
        List<Path> expectedFiles = List.of(
                Files.createFile(directoryToWatch.resolve("file1")),
                Files.createFile(directoryToWatch.resolve("file2"))
        );
        List<Path> files = filesWatcher.getFiles();

        Assertions.assertDoesNotThrow(() -> {
            while (!files.containsAll(expectedFiles)) {
                for (Path file: files) {
                    logger.trace(file.toString());
                }
            }
        });

        filesWatcher.close();
    }

    @Test
    void Check_Files_Detected_After_Some_Are_Deleted() throws Exception {
        Path directoryToWatch = Files.createTempDirectory(null);

        FilesWatcher filesWatcher = new FilesWatcher(
                new SimpleObjectProperty<>(directoryToWatch),
                path -> true,
                path -> false
        );
        Files.createFile(directoryToWatch.resolve("file1"));
        List<Path> expectedFiles = List.of(
                Files.createFile(directoryToWatch.resolve("file2")),
                Files.createFile(directoryToWatch.resolve("file3"))
        );
        Files.delete(directoryToWatch.resolve("file1"));

        TestUtils.assertCollectionsEqualsWithoutOrder(
                expectedFiles,
                filesWatcher.getFiles(),
                CHANGE_WAITING_TIME_MS      // wait for files to be detected
        );

        filesWatcher.close();
    }

    @Test
    void Check_Files_Added_In_Sub_Folder_Detected() throws Exception {
        Path directoryToWatch = Files.createTempDirectory(null);
        Path subFolder = Files.createDirectory(directoryToWatch.resolve("sub_folder"));
        List<Path> expectedFiles = List.of(
                Files.createFile(subFolder.resolve("file1")),
                Files.createFile(subFolder.resolve("file2"))
        );

        FilesWatcher filesWatcher = new FilesWatcher(
                new SimpleObjectProperty<>(directoryToWatch),
                path -> true,
                path -> false
        );

        TestUtils.assertCollectionsEqualsWithoutOrder(
                expectedFiles,
                filesWatcher.getFiles()
        );

        filesWatcher.close();
    }

    @Test
    void Check_Files_Detected_After_Sub_Folder_Moved() throws Exception {
        Path directoryToWatch = Files.createTempDirectory(null);

        FilesWatcher filesWatcher = new FilesWatcher(
                new SimpleObjectProperty<>(directoryToWatch),
                path -> true,
                path -> false
        );
        Path subFolder = Files.createDirectory(directoryToWatch.resolve("folder"));
        Files.createFile(subFolder.resolve("file1"));
        List<Path> expectedFiles = List.of(
                Files.createFile(directoryToWatch.resolve("file2")),
                Files.createFile(directoryToWatch.resolve("file3"))
        );
        Files.move(subFolder, Files.createTempDirectory(null).resolve("folder"));

        TestUtils.assertCollectionsEqualsWithoutOrder(
                expectedFiles,
                filesWatcher.getFiles(),
                CHANGE_WAITING_TIME_MS      // wait for files to be detected
        );

        filesWatcher.close();
    }

    @Test
    void Check_Files_Detected_After_Changing_Directory() throws Exception {
        ObjectProperty<Path> directoryToWatch = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        Files.createFile(directoryToWatch.getValue().resolve("file1"));
        Files.createFile(directoryToWatch.getValue().resolve("file2"));
        directoryToWatch.set(Files.createTempDirectory(null));
        List<Path> expectedFiles = List.of(
                Files.createFile(directoryToWatch.getValue().resolve("file3")),
                Files.createFile(directoryToWatch.getValue().resolve("file4"))
        );

        FilesWatcher filesWatcher = new FilesWatcher(
                directoryToWatch,
                path -> true,
                path -> false
        );

        TestUtils.assertCollectionsEqualsWithoutOrder(
                expectedFiles,
                filesWatcher.getFiles()
        );

        filesWatcher.close();
    }

    @Test
    void Check_Files_Detected_When_Files_To_Find_Specified() throws Exception {
        Path directoryToWatch = Files.createTempDirectory(null);
        Files.createFile(directoryToWatch.resolve("file1"));
        Files.createFile(directoryToWatch.resolve("file2"));
        List<Path> expectedFiles = List.of(
                Files.createFile(directoryToWatch.resolve("file3")),
                Files.createFile(directoryToWatch.resolve("file4"))
        );

        FilesWatcher filesWatcher = new FilesWatcher(
                new SimpleObjectProperty<>(directoryToWatch),
                path -> path.endsWith("file3") || path.endsWith("file4"),
                path -> false
        );

        TestUtils.assertCollectionsEqualsWithoutOrder(
                expectedFiles,
                filesWatcher.getFiles()
        );

        filesWatcher.close();
    }

    @Test
    void Check_Files_Detected_When_Directory_Skipped() throws Exception {
        Path directoryToWatch = Files.createTempDirectory(null);
        Path directoryToSkip = Files.createDirectory(directoryToWatch.resolve("skip"));
        Files.createFile(directoryToSkip.resolve("file1"));
        Files.createFile(directoryToSkip.resolve("file2"));
        List<Path> expectedFiles = List.of(
                Files.createFile(directoryToWatch.resolve("file3")),
                Files.createFile(directoryToWatch.resolve("file4"))
        );

        FilesWatcher filesWatcher = new FilesWatcher(
                new SimpleObjectProperty<>(directoryToWatch),
                path -> true,
                directoryToSkip::equals
        );

        TestUtils.assertCollectionsEqualsWithoutOrder(
                expectedFiles,
                filesWatcher.getFiles()
        );

        filesWatcher.close();
    }
}
