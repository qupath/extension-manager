package qupath.ext.extensionmanager.core;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.index.model.Extension;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.Registry;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.core.tools.FilesWatcher;
import qupath.ext.extensionmanager.core.tools.FileTools;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A class to manage the extension folder (containing extension installation
 * files).
 * <p>
 * This class is thread-safe.
 * <p>
 * This manager must be {@link #close() closed} once no longer used.
 * <p>
 * The extension folder is organized this way:
 * <ul>
 *     <li>indexes
 *         <ul><li><em>Index name</em>
 *             <ul><li><em>Extension name</em>
 *                 <ul><li><em>Extension version</em>
 *                     <ul>
 *                         <li>main-jar
 *                         <ul>
 *                             <li><em>Extensions main jar file</em></li>
 *                         </ul>
 *                         </li>
 *                         <li>javadocs-dependencies
 *                         <ul>
 *                             <li><em>Extension javadocs</em></li>
 *                         </ul>
 *                         </li>
 *                         <li>required-dependencies
 *                         <ul>
 *                             <li><em>Required dependencies</em></li>
 *                         </ul>
 *                         </li>
 *                         <li>optional-dependencies
 *                         <ul>
 *                             <li><em>Optional dependencies</em></li>
 *                         </ul>
 *                         </li></ul></li></ul></li></ul></li>
 *             <li>registry.json</li>
 *         </ul>
 *     </li>
 *     <li><em>Extension JARs manually installed</em></li>
 * </ul>
 */
class ExtensionFolderManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionFolderManager.class);
    private static final String INDEXES_FOLDER = "indexes";
    private static final String REGISTRY_NAME = "registry.json";
    private static final Predicate<Path> isJar = path -> path.toString().toLowerCase().endsWith(".jar");
    private static final Gson gson = new Gson();
    private final StringProperty extensionDirectoryPath;
    private final FilesWatcher manuallyInstalledExtensionsWatcher;
    private final FilesWatcher indexManagedInstalledExtensionsWatcher;
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
     * @param extensionDirectoryPath a string property pointing to the path the extension folder
     *                               should have. If it changes, the content of the old path will
     *                               NOT be copied to the new path. The path (but not the property)
     *                               can be null or invalid
     */
    public ExtensionFolderManager(StringProperty extensionDirectoryPath) {
        this.extensionDirectoryPath = extensionDirectoryPath;

        this.manuallyInstalledExtensionsWatcher = new FilesWatcher(
                extensionDirectoryPath,
                isJar,
                path -> {
                    try {
                        return path.equals(Paths.get(
                                extensionDirectoryPath.get(),
                                INDEXES_FOLDER
                        ));
                    } catch (InvalidPathException | NullPointerException e) {
                        logger.debug(String.format("Error when trying to assess if %s should be watched", path), e);
                        return true;
                    }
                }
        );

        this.indexManagedInstalledExtensionsWatcher = new FilesWatcher(
                extensionDirectoryPath.map(path -> {
                    if (path == null) {
                        return null;
                    } else {
                        try {
                            return getAndCreateIndexesFolder().toString();
                        } catch (IOException | InvalidPathException | SecurityException e) {
                            logger.debug(String.format("Error when getting index path from %s", path), e);
                            return null;
                        }
                    }
                }),
                isJar,
                path -> true
        );
    }

    @Override
    public void close() throws Exception {
        manuallyInstalledExtensionsWatcher.close();
        indexManagedInstalledExtensionsWatcher.close();
    }

    /**
     * @return the registry that was last saved with {@link #saveRegistry(Registry)}
     * @throws IOException if an I/O error occurs while reading the registry file
     * @throws java.io.FileNotFoundException if the registry file does not exist
     * @throws java.nio.file.InvalidPathException if the path to the registry cannot be created
     * @throws SecurityException if the user doesn't have enough rights to read the registry
     * @throws NullPointerException if the registry file exists but is empty or if the path contained
     * in {@link #getExtensionDirectoryPath()} is null
     * @throws JsonSyntaxException if the registry file exists but contain a malformed JSON element
     * @throws JsonIOException if there was a problem reading from the registry file
     */
    public synchronized Registry getSavedRegistry() throws IOException {
        try(
                FileReader fileReader = new FileReader(getRegistryPath().toFile());
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
     * @throws SecurityException if the user doesn't have sufficient rights to save the file
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null
     */
    public synchronized void saveRegistry(Registry registry) throws IOException {
        try (
                FileWriter fileWriter = new FileWriter(getRegistryPath().toFile());
                BufferedWriter writer = new BufferedWriter(fileWriter)
        ) {
            writer.write(gson.toJson(registry));
        }
    }

    /**
     * @return a read only property containing the path to the extension folder.
     * It may be updated from any thread and the path (but not the property) can be null
     * or invalid
     */
    public ReadOnlyStringProperty getExtensionDirectoryPath() {
        return extensionDirectoryPath;
    }

    /**
     * Delete all extensions belonging to the provided index.
     *
     * @param savedIndex the index to delete
     * @throws IOException if an I/O error occur while deleting the files
     * @throws SecurityException if the user doesn't have sufficient rights to delete
     * the index
     * @throws java.nio.file.InvalidPathException if a Path object cannot be constructed from the index
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null
     * path, for example because the extensions folder path contain invalid characters
     */
    public synchronized void deleteIndex(SavedIndex savedIndex) throws IOException {
        FileTools.deleteDirectoryRecursively(Paths.get(
                getAndCreateIndexesFolder().toString(),
                savedIndex.name()
        ).toFile());
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were
     * manually added (i.e. not with an index) to the extension directory
     */
    public ObservableList<Path> getManuallyInstalledJars() {
        return manuallyInstalledExtensionsWatcher.getFiles();
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were
     * added with indexes to the extension directory
     */
    public ObservableList<Path> getIndexedManagedInstalledJars() {
        return indexManagedInstalledExtensionsWatcher.getFiles();
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
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null
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
        if (!FileTools.isDirectoryNotEmpty(mainJarFolderPath)) {
            return Optional.empty();
        }

        Path optionalDependenciesFolderPath = Paths.get(
                versionPath.toString(),
                FileType.OPTIONAL_DEPENDENCIES.name
        );
        boolean optionalDependenciesInstalled = FileTools.isDirectoryNotEmpty(optionalDependenciesFolderPath);

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
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null
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
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null
     */
    public synchronized void deleteExtension(SavedIndex savedIndex, Extension extension) throws IOException {
        FileTools.deleteDirectoryRecursively(getExtensionFolder(savedIndex, extension).toFile());
    }

    private Path getRegistryPath() throws IOException {
        return getAndCreateIndexesFolder().resolve(REGISTRY_NAME);
    }

    private Path getAndCreateIndexesFolder() throws IOException {
        Path indexesFolder = Paths.get(
                extensionDirectoryPath.get(),
                INDEXES_FOLDER
        );

        if (Files.isRegularFile(indexesFolder)) {
            Files.deleteIfExists(indexesFolder);
        }
        Files.createDirectories(indexesFolder);

        return indexesFolder;
    }

    private Path getExtensionFolder(SavedIndex savedIndex, Extension extension) throws IOException {
        return Paths.get(
                getAndCreateIndexesFolder().toString(),
                FileTools.stripInvalidFilenameCharacters(savedIndex.name()),
                FileTools.stripInvalidFilenameCharacters(extension.name())
        );
    }
}
