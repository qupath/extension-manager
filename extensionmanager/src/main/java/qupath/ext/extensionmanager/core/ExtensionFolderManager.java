package qupath.ext.extensionmanager.core;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.index.model.Extension;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.Registry;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.core.tools.DirectoryWatcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A class to manage the extension folder (containing extension installation
 * files).
 * <p>
 * This class is thread-safe.
 * <p>
 * This manager must be {@link #close() closed} once no longer used.
 */
class ExtensionFolderManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionFolderManager.class);
    private static final String REGISTRY_NAME = "registry.json";
    private static final Gson gson = new Gson();
    private final StringProperty extensionFolderPath;
    private DirectoryWatcher extensionDirectoryWatcher;
    /**
     * A type of files to retrieve.
     */
    public enum FileType {
        /**
         * The folder containing the JAR of an extension
         */
        MAIN_JAR("main-jar"),
        /**
         * The folder containing the Javadocs JAR of an extension
         */
        JAVADOCS("javadocs-dependencies"),
        /**
         * The folder containing the required dependencies of an extension.
         * They can be JAR or ZIP files.
         */
        REQUIRED_DEPENDENCIES("required-dependencies"),
        /**
         * The folder containing the optional dependencies of an extension.
         * They can be JAR or ZIP files.
         */
        OPTIONAL_DEPENDENCIES("optional-dependencies");

        private final String name;

        FileType(String name) {
            this.name = name;
        }
    }

    /**
     * Create the extension folder manager.
     *
     * @param extensionFolderPath a string property pointing to the path the extension folder
     *                            should have. If it changes, the content of the old path will
     *                            NOT be copied to the new path
     */
    public ExtensionFolderManager(StringProperty extensionFolderPath) {
        this.extensionFolderPath = extensionFolderPath;

        setExtensionDirectoryWatcher();
        extensionFolderPath.addListener((p, o, n) ->
                setExtensionDirectoryWatcher()
        );
    }

    @Override
    public void close() throws Exception {
        if (extensionDirectoryWatcher != null) {
            extensionDirectoryWatcher.close();
        }
    }

    /**
     * @return the registry that was last saved with {@link #saveRegistry(Registry)}
     * @throws IOException if an I/O error occurs while reading the registry file
     * @throws java.io.FileNotFoundException if the registry file does not exist
     * @throws java.nio.file.InvalidPathException if the path to the registry cannot be created
     * @throws SecurityException if the user doesn't have enough rights to read the registry
     * @throws NullPointerException if the registry file exists but is empty
     * @throws JsonSyntaxException if the registry file exists but contain a malformed JSON element
     * @throws JsonIOException if there was a problem reading from the registry file
     */
    public synchronized Registry getSavedRegistry() throws IOException {
        try(
                FileReader fileReader = new FileReader(Paths.get(
                        extensionFolderPath.get(),
                        REGISTRY_NAME
                ).toFile());
                JsonReader jsonReader = new JsonReader(fileReader)
        ) {
            return Objects.requireNonNull(gson.fromJson(jsonReader, Registry.class));
        }
    }

    /**
     * Save the provided registry to disk. It can later be retrieved with
     * {@link #getSavedRegistry()}.
     *
     * @param registry the registry to save
     * @throws IOException if an I/O error occurs while writing the registry file
     */
    public synchronized void saveRegistry(Registry registry) throws IOException {
        try (
                FileWriter fileWriter = new FileWriter(Paths.get(
                        extensionFolderPath.get(),
                        REGISTRY_NAME
                ).toFile());
                BufferedWriter writer = new BufferedWriter(fileWriter)
        ) {
            writer.write(gson.toJson(registry));
        }
    }

    /**
     * @return the path to the extension folder. It may be updated from any thread
     */
    public StringProperty getExtensionFolderPath() {
        return extensionFolderPath;
    }

    /**
     * Delete all extensions belonging to the provided index.
     *
     * @param savedIndex the index to delete
     * @throws IOException if an I/O error occur while deleting the files
     * @throws SecurityException if the user doesn't have sufficient rights to delete
     * the index
     * @throws java.nio.file.InvalidPathException if a Path object cannot be constructed from the index
     * path, for example because the extensions folder path contain invalid characters
     */
    public synchronized void deleteIndex(SavedIndex savedIndex) throws IOException {
        deleteDirectory(Paths.get(
                extensionFolderPath.get(),
                savedIndex.name()
        ).toFile());
    }

    /**
     * Indicate whether an extension belonging to an index is installed. If that's the
     * case, installation information are returned.
     *
     * @param savedIndex the index owning the extension to search
     * @param extension the extension to search
     * @return an empty Optional if the provided extension is not installed, or information
     * on the installed extension
     * @throws IOException if an I/O error occurs when searching for the extension
     * @throws java.nio.file.InvalidPathException if the Path object of the extension cannot be created, for example
     * because the extensions folder path contain invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to search for extension files
     */
    public synchronized Optional<InstalledExtension> getInstalledExtension(SavedIndex savedIndex, Extension extension) throws IOException {
        Path extensionPath = getExtensionFolder(savedIndex, extension);

        Path versionPath = null;
        if (Files.isDirectory(extensionPath)) {
            try (Stream<Path> stream = Files.list(extensionPath)) {
                versionPath = stream
                        .filter(Files::isDirectory)
                        .findAny()
                        .orElse(null);
            }
        }
        if (versionPath == null) {
            return Optional.empty();
        }

        Path mainJarFolderPath = Paths.get(
                versionPath.toString(),
                FileType.MAIN_JAR.name
        );
        if (!isDirectoryNotEmpty(mainJarFolderPath)) {
            return Optional.empty();
        }

        Path optionalDependenciesFolderPath = Paths.get(
                versionPath.toString(),
                FileType.OPTIONAL_DEPENDENCIES.name
        );
        boolean optionalDependenciesInstalled = isDirectoryNotEmpty(optionalDependenciesFolderPath);

        return Optional.of(new InstalledExtension(versionPath.toFile().getName(), optionalDependenciesInstalled));
    }

    /**
     * Get (and create if it doesn't already exist) the path to the folder
     * containing the specified files of the provided extension at the specified
     * version belonging to the provided index.
     *
     * @param savedIndex the index owning the extension
     * @param extension the extension to find the folder to
     * @param releaseName the version name of the extension to retrieve
     * @param fileType the type of files to retrieve
     * @return the path to the folder containing the specified files of the provided extension
     * @throws IOException if an I/O error occurs while creating the folder
     * @throws java.nio.file.InvalidPathException if the Path object cannot be created, for example
     * because the extensions folder path contain invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to create the folder
     */
    public synchronized Path createAndGetExtensionPath(
            SavedIndex savedIndex,
            Extension extension,
            String releaseName,
            FileType fileType
    ) throws IOException {
        Path folderPath = Paths.get(
                getExtensionFolder(savedIndex, extension).toString(),
                releaseName,
                fileType.name
        );

        if (Files.isRegularFile(folderPath)) {
            Files.deleteIfExists(folderPath);
        }
        Files.createDirectories(folderPath);

        return folderPath;
    }

    /**
     * Delete all files of an extension belonging to an index.
     *
     * @param savedIndex the index owning the extension to delete
     * @param extension the extension to delete
     * @throws IOException if an I/O error occurs while deleting the folder
     * @throws java.nio.file.InvalidPathException if the Path object of the extension folder cannot be created, for example
     * because the extensions folder path contain invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to delete the folder
     */
    public synchronized void deleteExtension(SavedIndex savedIndex, Extension extension) throws IOException {
        deleteDirectory(getExtensionFolder(savedIndex, extension).toFile());
    }

    private void setExtensionDirectoryWatcher() {
        synchronized (this) {
            if (extensionDirectoryWatcher != null) {
                try {
                    extensionDirectoryWatcher.close();
                } catch (Exception e) {
                    logger.debug("Error when closing extension directory watcher", e);
                }
            }
        }

        try {
            synchronized (this) {
                extensionDirectoryWatcher = new DirectoryWatcher(
                        Paths.get(extensionFolderPath.get()),
                        addedFile -> {},
                        removedFile -> {}
                );
            }
        } catch (IOException | InvalidPathException | UnsupportedOperationException | SecurityException e) {
            logger.warn(String.format(
                    "Error when creating extension directory watcher for %s. Extensions manually installed won't be detected."
                    , extensionFolderPath.get()
            ), e);
        }
    }

    private Path getExtensionFolder(SavedIndex savedIndex, Extension extension) {
        return Paths.get(
                extensionFolderPath.get(),
                stripInvalidFilenameChars(savedIndex.name()),
                stripInvalidFilenameChars(extension.name())
        );
    }

    private static boolean isDirectoryNotEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return entries.findAny().isPresent();
            }
        } else {
            return false;
        }
    }

    private static void deleteDirectory(File directoryToBeDeleted) throws IOException {
        File[] childFiles = directoryToBeDeleted.listFiles();
        if (childFiles != null) {
            for (File file : childFiles) {
                deleteDirectory(file);
            }
        }

        Files.deleteIfExists(directoryToBeDeleted.toPath());
    }

    private static String stripInvalidFilenameChars(String name) {
        return name.replaceAll("[\\\\/:\"*?<>|\\n\\r]+", "");
    }
}
