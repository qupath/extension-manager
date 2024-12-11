package qupath.ext.extensionmanager.core.tools;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
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
    private static final int MAX_SEARCH_DEPTH = 8;
    private final ObservableList<Path> files = FXCollections.observableArrayList();
    private final ObservableList<Path> filesImmutable = FXCollections.unmodifiableObservableList(files);
    private final ObservableValue<Path> directoryToWatch;
    private final Predicate<Path> filesToFind;
    private final Predicate<Path> directoriesToSkip;
    private RecursiveDirectoryWatcher directoryWatcher;

    /**
     * Create the watcher.
     *
     * @param directoryToWatch the root directory to watch
     * @param filesToFind a predicate indicating if a Path corresponds to a file that should be considered
     * @param directoriesToSkip a predicate indicating if a Path corresponding to a directory must be ignored
     *                          (including its children)
     */
    public FilesWatcher(
            ObservableValue<Path> directoryToWatch,
            Predicate<Path> filesToFind,
            Predicate<Path> directoriesToSkip
    ) {
        this.directoryToWatch = directoryToWatch;
        this.filesToFind = filesToFind;
        this.directoriesToSkip = directoriesToSkip;

        setDirectoryWatcher();
        directoryToWatch.addListener((p, o, n) -> setDirectoryWatcher());
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

    private synchronized void setDirectoryWatcher() {
        Path directory = directoryToWatch.getValue();

        logger.debug("Clearing file list and resetting directory watcher to watch {}", directory);
        files.clear();

        if (directoryWatcher != null) {
            try {
                directoryWatcher.close();
            } catch (Exception e) {
                logger.debug("Error when closing directory watcher", e);
            }
        }

        if (directory == null) {
            logger.debug("The directory to watch is null. No file will be detected");
            return;
        }

        try {
            directoryWatcher = new RecursiveDirectoryWatcher(
                    directory,
                    MAX_SEARCH_DEPTH,
                    filesToFind,
                    directoriesToSkip,
                    addedFile -> {
                        logger.debug("File {} added", addedFile);

                        synchronized (this) {
                            files.add(addedFile);
                        }
                    },
                    removedFile -> {
                        logger.debug("File {} removed", removedFile);

                        synchronized (this) {
                            files.remove(removedFile);
                        }
                    }
            );
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            logger.debug(String.format(
                    "Error when creating files watcher for %s. Files added to this directory won't be detected.",
                    directory
            ), e);
        }
    }
}
