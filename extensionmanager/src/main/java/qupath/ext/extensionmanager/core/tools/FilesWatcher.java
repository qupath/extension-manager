package qupath.ext.extensionmanager.core.tools;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

/**
 * A class that returns an ObservableList of paths to certain files that
 * are contained within a specific directory (recursively).
 * The list is automatically updated based on changes to the specified directory
 * (or one of its descendant).
 * <p>
 * This watcher must be {@link #close() closed} once no longer used.
 */
public class FilesWatcher implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(FilesWatcher.class);
    private static final int MAX_JAR_SEARCH_DEPTH = 8;
    private final ObservableList<Path> files = FXCollections.observableArrayList();
    private final ObservableList<Path> filesImmutable = FXCollections.unmodifiableObservableList(files);
    private final ObservableValue<String> directoryToWatch;
    private final Predicate<Path> filesToFind;
    private final Predicate<Path> directoriesToSkip;
    private RecursiveDirectoryWatcher directoryWatcher;

    /**
     * Create the watcher.
     *
     * @param directoryToWatch the root directory to watch. Its
     * @param filesToFind a predicate indicating if a Path corresponds to a file that should be considered
     * @param directoriesToSkip a predicate indicating if a Path corresponding to a directory must be ignored
     *                          (including its children)
     */
    public FilesWatcher(ObservableValue<String> directoryToWatch, Predicate<Path> filesToFind, Predicate<Path> directoriesToSkip) {
        this.directoryToWatch = directoryToWatch;
        this.filesToFind = filesToFind;
        this.directoriesToSkip = directoriesToSkip;

        setFiles();
        setDirectoryWatcher();

        directoryToWatch.addListener((p, o, n) -> {
            setFiles();
            setDirectoryWatcher();
        });
    }

    @Override
    public void close() throws Exception {
        if (directoryWatcher != null) {
            directoryWatcher.close();
        }
    }

    /**
     * @return a read-only list of files that are currently present according to the parameters
     * specified in {@link #FilesWatcher(ObservableValue, Predicate, Predicate)}
     */
    public ObservableList<Path> getFiles() {
        return filesImmutable;
    }

    private synchronized void setFiles() {
        files.clear();

        String directory = directoryToWatch.getValue();
        if (directory == null) {
            return;
        }

        try {
            files.addAll(FileTools.findFilesRecursively(
                    Paths.get(directory),
                    filesToFind,
                    directoriesToSkip,
                    MAX_JAR_SEARCH_DEPTH
            ));
        } catch (IOException | InvalidPathException | SecurityException e) {
            logger.debug(String.format("Error when searching files in %s", directory), e);
        }
    }

    private synchronized void setDirectoryWatcher() {
        if (directoryWatcher != null) {
            try {
                directoryWatcher.close();
            } catch (Exception e) {
                logger.debug("Error when closing directory watcher", e);
            }
        }

        String directory = directoryToWatch.getValue();
        if (directory == null) {
            return;
        }

        try {
            directoryWatcher = new RecursiveDirectoryWatcher(
                    Paths.get(directory),
                    MAX_JAR_SEARCH_DEPTH,
                    filesToFind,
                    directoriesToSkip,
                    addedFile -> {
                        synchronized (this) {
                            files.add(addedFile);
                        }
                    },
                    removedFile -> {
                        synchronized (this) {
                            files.remove(removedFile);
                        }
                    }
            );
        } catch (IOException | InvalidPathException | UnsupportedOperationException | SecurityException e) {
            logger.debug(String.format(
                    "Error when creating extension directory watcher for %s. Extensions manually installed won't be detected.",
                    directory
            ), e);
        }
    }
}
