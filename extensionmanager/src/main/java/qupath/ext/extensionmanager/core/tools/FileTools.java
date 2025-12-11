package qupath.ext.extensionmanager.core.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Static functions to work with files.
 */
public class FileTools {

    private static final Logger logger = LoggerFactory.getLogger(FileTools.class);

    private FileTools() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Indicate whether the provided path is a directory and is not empty.
     *
     * @param path the path to check
     * @return whether the provided path is a directory and is not empty
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if the provided path is null
     */
    public static boolean isDirectoryNotEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                if (entries.findAny().isPresent()) {
                    logger.debug("The specified path {} is a non-empty directory", path);
                    return true;
                } else {
                    logger.debug("The specified path {} is an empty directory", path);
                    return false;
                }
            }
        } else {
            logger.debug("The specified path {} is not an existing directory", path);
            return false;
        }
    }

    /**
     * Attempt to move the provided file to trash, or delete it (and all its children recursively if it's a directory) if
     * the current platform does not support moving files to trash.
     * <p>
     * This won't do anything if the provided file doesn't exist.
     *
     * @param directoryToDelete the file or directory to delete
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if the provided directory is null
     */
    public static void moveDirectoryToTrashOrDeleteRecursively(File directoryToDelete) throws IOException {
        if (!directoryToDelete.exists()) {
            logger.debug("Can't delete {}: the path does not exist", directoryToDelete);
            return;
        }

        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            logger.debug("Attempting to move {} to trash", directoryToDelete);
            if (!moveToTrash(desktop, directoryToDelete)) {
                logger.debug("Failed to move {} to trash. Deleting it", directoryToDelete);
                deleteDirectoryRecursively(directoryToDelete);
            }
        } else {
            logger.debug("Moving to trash not supported. Deleting {}", directoryToDelete);
            deleteDirectoryRecursively(directoryToDelete);
        }
    }

    /**
     * Strip the provided name from characters that would be invalid in a file name. This is not guaranteed to work for
     * any character.
     *
     * @param name the name to strip characters from
     * @return the provided name without characters that would be invalid in a file name
     * @throws NullPointerException if the provided name is null
     */
    public static String stripInvalidFilenameCharacters(String name) {
        return name.replaceAll("[\\\\/:\"*?<>|\\n\\r]+", "");
    }

    /**
     * Get the name of the file or directory denoted by the path contained in the provided URI.
     *
     * @param uri the URI containing the file name to retrieve
     * @return the file name denoted by the path contained in the provided URI
     * @throws NullPointerException if the path of the provided URI is undefined
     * @throws java.nio.file.InvalidPathException if the path of the provided URI cannot be converted to a Path
     */
    public static String getFileNameFromURI(URI uri) {
        return Paths.get(uri.getPath()).getFileName().toString();
    }

    /**
     * Indicate whether a file is a (direct or not) parent of another file. The provided files don't have to exist.
     *
     * @param possibleParent the file that may be a parent of the other file
     * @param possibleChild the file that may be a child of the other file
     * @return a boolean indicating whether the possible parent is actually a parent of the other file
     * @throws NullPointerException if the possible child is null
     */
    public static boolean isFileParentOfAnotherFile(File possibleParent, File possibleChild) {
        File parent = possibleChild.getParentFile();
        while (parent != null) {
            if (parent.equals(possibleParent)) {
                return true;
            } else {
                parent = parent.getParentFile();
            }
        }
        return false;
    }

    private static boolean moveToTrash(Desktop desktop, File fileToDelete) {
        if (SwingUtilities.isEventDispatchThread() || !isWindows()) {
            // It seems safe to call move to trash from any thread on macOS and Linux
            // We can't use the EDT on macOS because of https://bugs.openjdk.org/browse/JDK-8087465
            return desktop.moveToTrash(fileToDelete);
        } else {
            // EXCEPTION_ACCESS_VIOLATION associated with moveToTrash reported on Windows 11.
            // https://github.com/qupath/qupath/issues/1738
            // Could not be replicated (but we didn't have Windows 11...); taking a guess that this might help.
            try {
                SwingUtilities.invokeAndWait(() -> moveToTrash(desktop, fileToDelete));
            } catch (Exception e) {
                logger.warn("Cannot move file {} to trash", fileToDelete, e);
                return false;
            }
            return !fileToDelete.exists();
        }
    }

    private static void deleteDirectoryRecursively(File directoryToBeDeleted) throws IOException {
        logger.debug("Deleting children of {}", directoryToBeDeleted);
        File[] childFiles = directoryToBeDeleted.listFiles();
        if (childFiles != null) {
            for (File file : childFiles) {
                deleteDirectoryRecursively(file);
            }
        }

        logger.debug("Deleting {}", directoryToBeDeleted);
        Files.deleteIfExists(directoryToBeDeleted.toPath());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
