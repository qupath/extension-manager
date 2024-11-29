package qupath.ext.extensionmanager.core.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
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

        Files.walkFileTree(
                directory,
                EnumSet.noneOf(FileVisitOption.class),
                depth,
                new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (directoriesToSkip.test(dir)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        } else {
                            return FileVisitResult.CONTINUE;
                        }
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (filesToFind.test(file)) {
                            files.add(file);
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
                return entries.findAny().isPresent();
            }
        } else {
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
        File[] childFiles = directoryToBeDeleted.listFiles();
        if (childFiles != null) {
            for (File file : childFiles) {
                deleteDirectoryRecursively(file);
            }
        }

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
}
