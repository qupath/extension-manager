package qupath.ext.extensionmanager.core.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A class that listens to added and removed files of a directory and its children.
 * <p>
 * This watcher must be {@link #close() closed} once no longer used.
 */
public class RecursiveDirectoryWatcher implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(RecursiveDirectoryWatcher.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<WatchKey, Path> keys = new HashMap<>();
    private final WatchService watchService;
    private final int depth;
    private final Predicate<Path> directoriesToSkip;

    /**
     * Set up some listeners to be called when files are added or removed
     * from the provided directory or one of its children.
     *
     * @param directoryToWatch the path of the root directory to watch
     * @param depth the maximum number of directory levels to watch
     * @param filesToFind a predicate indicating files to consider. Listeners won't be called for
     *                    files that don't match this predicate
     * @param directoriesToSkip a predicate indicating directories not to watch
     * @param onFileAdded a function that will be called when a new file is added to one of the watched directory.
     *                    Its parameter will be the path of the new file. This function may be called from
     *                    any thread
     * @param onFileDeleted a function that will be called when a file is removed from one of the watched directory.
     *                      Its parameter will be the path of the deleted file. This function may be called
     *                      from any thread. Note that if a folder containing a file is deleted, this function
     *                      won't be called for this file
     * @throws IOException if an I/O error occurs
     * @throws UnsupportedOperationException if watching file system is not supported by
     * this system
     * @throws java.nio.file.NotDirectoryException if there is no directory on the provided path
     * @throws SecurityException if the user doesn't have enough rights to read the provided directory
     */
    public RecursiveDirectoryWatcher(
            Path directoryToWatch,
            int depth,
            Predicate<Path> filesToFind,
            Predicate<Path> directoriesToSkip,
            Consumer<Path> onFileAdded,
            Consumer<Path> onFileDeleted
    ) throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.depth = depth;
        this.directoriesToSkip = directoriesToSkip;

        registerDirectory(directoryToWatch);

        executor.execute(() -> {
            WatchKey key;

            try {
                while ((key = watchService.take()) != null) {
                    Path parentFolder = keys.get(key);

                    if (parentFolder == null) {
                        throw new NullPointerException(String.format(
                                "%s is missing from %s", key, keys
                        ));
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        Path filename = (Path) event.context();
                        Path filePath = parentFolder.resolve(filename);

                        if (Files.isDirectory(filePath) && kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            registerDirectory(filePath);
                        }

                        if (filesToFind.test(filePath)) {
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                onFileAdded.accept(filePath);
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                onFileDeleted.accept(filePath);
                            }
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                logger.debug("Watching {} interrupted", directoryToWatch, e);
                Thread.currentThread().interrupt();
            } catch (ClosedWatchServiceException e) {
                logger.debug("Service watching {} closed", directoryToWatch, e);
            } catch (IOException | NullPointerException | SecurityException e) {
                logger.error(String.format("Error when watching directory %s", directoryToWatch), e);
            }
        });
    }

    private void registerDirectory(Path directory) throws IOException {
        Files.walkFileTree(
                directory,
                EnumSet.noneOf(FileVisitOption.class),
                depth,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (directoriesToSkip.test(dir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        } else {
                            keys.put(
                                    dir.register(
                                            watchService,
                                            StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE
                                    ),
                                    dir
                            );

                            return FileVisitResult.CONTINUE;
                        }
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
