package qupath.ext.extensionmanager.core.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
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
     * Find files recursively in the specified directory. Some files and directories can be skipped.
     *
     * @param directory the directory from where to start the search
     * @param filesToFind a predicate indicating if a file should be returned
     * @param directoriesToSkip a predicate indicating directories to skip during the search
     * @return a list of all files that respect the file predicate, are descendant of the provided directory,
     * are not descendant of the directories to skip, and whose depth is below the provided one
     * @param depth the maximum number of directory levels to visit
     * @throws IOException if an I/O error occurs
     * @throws SecurityException if the user doesn't have sufficient rights to search in the provided directory
     */
    public static List<Path> findFilesRecursively(
            Path directory,
            Predicate<Path> filesToFind,
            Predicate<Path> directoriesToSkip,
            int depth
    ) throws IOException {
        List<Path> files = new ArrayList<>();

        logger.debug("Searching files in {} with depth {}", directory, depth);
        Files.walkFileTree(
                directory,
                EnumSet.noneOf(FileVisitOption.class),
                depth,
                new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (directoriesToSkip.test(dir)) {
                            logger.debug("Skipping directory {}", dir);
                            return FileVisitResult.SKIP_SUBTREE;
                        } else {
                            logger.debug("Search will continue in {}", dir);
                            return FileVisitResult.CONTINUE;
                        }
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (filesToFind.test(file)) {
                            logger.debug("File {} found", file);
                            files.add(file);
                        } else {
                            logger.debug("File {} found but not retained as it doesn't match the predicate", file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                }
        );

        return files;
    }

    /**
     * Indicate whether the provided path is a directory and is not empty.
     *
     * @param path the path to check
     * @return whether the provided path is a directory and is not empty
     * @throws IOException if an I/O error occurs
     * @throws SecurityException if the user doesn't have sufficient rights to read the file
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
     * Delete the provided file or directory, and delete all its children
     * recursively if it's a directory.
     *
     * @param directoryToBeDeleted the file or directory to delete
     * @throws IOException if an I/O error occurs
     * @throws SecurityException if the user doesn't have sufficient rights to delete
     * some files
     */
    public static void deleteDirectoryRecursively(File directoryToBeDeleted) throws IOException {
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

    /**
     * Strip the provided name from characters that would be invalid
     * in a file name. This is not guaranteed to work for any character.
     *
     * @param name the name to strip characters from
     * @return the provided name without characters that would be invalid in a file name
     */
    public static String stripInvalidFilenameCharacters(String name) {
        return name.replaceAll("[\\\\/:\"*?<>|\\n\\r]+", "");
    }

    /**
     * Get the name of the file or directory denoted by the path contained in the
     * provided URI.
     *
     * @param uri the URI containing the file name to retrieve
     * @return the file name denoted by the path contained in the provided URI
     * @throws NullPointerException if the path of the provided URI is undefined
     * @throws java.nio.file.InvalidPathException if the path of the provided URI cannot be
     * converted to a Path
     */
    public static String getFileNameFromURI(URI uri) {
        return Paths.get(uri.getPath()).getFileName().toString();
    }
}
