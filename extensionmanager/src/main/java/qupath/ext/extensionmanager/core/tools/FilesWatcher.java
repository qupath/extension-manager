package qupath.ext.extensionmanager.core.tools;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * A class that returns an ObservableList of paths to certain files that
 * are contained within a specific directory (recursively but with a maximal
 * depth of 8).
 * The list is automatically updated based on changes to the specified directory
 * (or one of its descendant).
 * Note that changes may take a few seconds to be detected.
 * <p>
 * This watcher must be {@link #close() closed} once no longer used.
 * <p>
 * This class is thread-safe.
 */
public class FilesWatcher implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(FilesWatcher.class);
    private static final int MAX_SEARCH_DEPTH = 8;
    private final ObservableList<Path> files = FXCollections.observableList(new CopyOnWriteArrayList<>());
    private final ObservableList<Path> filesImmutable = FXCollections.unmodifiableObservableList(files);
    private final ObservableValue<Path> directoryToWatch;
    private final Predicate<Path> filesToFind;
    private final Predicate<Path> directoriesToSkip;
    private RecursiveDirectoryWatcher directoryWatcher;

    /**
     * Create the watcher.
     *
     * @param directoryToWatch the root directory to watch. Already existing files of this root directory
     *                         will also be detected
     * @param filesToFind a predicate indicating if a Path corresponds to a file that should be considered
     * @param directoriesToSkip a predicate indicating if a Path corresponding to a directory must be ignored
     *                          (including its children)
     * @throws NullPointerException if one of the parameter is null
     */
    public FilesWatcher(
            ObservableValue<Path> directoryToWatch,
            Predicate<Path> filesToFind,
            Predicate<Path> directoriesToSkip
    ) {
        if (filesToFind == null) {
            throw new NullPointerException("The files to find predicate is null");
        }
        if (directoriesToSkip == null) {
            throw new NullPointerException("The directories to skip predicate is null");
        }

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
     * specified in {@link #FilesWatcher(ObservableValue, Predicate, Predicate)}. This list may be
     * updated from any thread
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

                        files.add(addedFile);
                    },
                    removedFile -> {
                        logger.debug("File {} removed", removedFile);

                        files.remove(removedFile);
                    }
            );
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            logger.debug(
                    "Error when creating files watcher for {}. Files added to this directory won't be detected.",
                    directory,
                    e
            );
        }
    }
}
