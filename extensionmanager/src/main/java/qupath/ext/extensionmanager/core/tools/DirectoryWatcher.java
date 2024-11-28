package qupath.ext.extensionmanager.core.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * A class that listens to added and removed files of a directory.
 * <p>
 * This watcher must be {@link #close() closed} once no longer used.
 */
public class DirectoryWatcher implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(DirectoryWatcher.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final WatchService watchService;

    /**
     * Set up some listeners to be called when files are added or removed
     * from the provided directory.
     *
     * @param directoryToWatch the path to the directory to watch
     * @param onFileAdded a function that will be called when a new file is added to the provided directory.
     *                    Its parameter will be the path of the new file. This function may be called from
     *                    any thread
     * @param onFileDeleted a function that will be called when a file is removed from the provided directory.
     *                      Its parameter will be the path of the deleted file. This function may be called
     *                      from any thread
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if watching file system is not supported by
     * this system
     * @throws java.nio.file.NotDirectoryException if there is no directory on the provided path
     * @throws SecurityException if the user doesn't have enough rights to read the provided directory
     */
    public DirectoryWatcher(Path directoryToWatch, Consumer<Path> onFileAdded, Consumer<Path> onFileDeleted) throws IOException {
        watchService = FileSystems.getDefault().newWatchService();

        directoryToWatch.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE
        );

        executor.execute(() -> {
            WatchKey key;

            try {
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path filename = (Path) event.context();

                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            onFileAdded.accept(filename);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            onFileDeleted.accept(filename);
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                logger.debug("Watching {} interrupted", directoryToWatch, e);
                Thread.currentThread().interrupt();
            } catch (ClosedWatchServiceException e) {
                logger.debug("Service watching {} closed", directoryToWatch, e);
            }
        });
    }

    @Override
    public void close() throws IOException {
        executor.shutdownNow();
        executor.close();

        watchService.close();
    }
}
