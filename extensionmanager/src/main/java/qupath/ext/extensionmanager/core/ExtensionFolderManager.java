package qupath.ext.extensionmanager.core;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.catalog.Extension;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.Registry;
import qupath.ext.extensionmanager.core.savedentities.SavedCatalog;
import qupath.ext.extensionmanager.core.tools.FilesWatcher;
import qupath.ext.extensionmanager.core.tools.FileTools;

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
 *     <li>catalogs
 *         <ul><li><em>Catalog name</em>
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
    private static final String CATALOGS_FOLDER = "catalogs";
    private static final String REGISTRY_NAME = "registry.json";
    private static final Predicate<Path> isJar = path -> path.toString().toLowerCase().endsWith(".jar");
    private static final Gson gson = new Gson();
    private final ReadOnlyObjectProperty<Path> extensionDirectoryPath;
    private final FilesWatcher manuallyInstalledExtensionsWatcher;
    private final FilesWatcher catalogManagedInstalledExtensionsWatcher;
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
     * @param extensionDirectoryPath a read-only property pointing to the path the extension directory should have. The
     *                               path can be null or invalid (but not the property). If this property is changed,
     *                               catalogs and extensions will be set to the content of the new value of the property
     *                               (so will be reset if the new path is empty)
     * @throws NullPointerException if the parameter is null
     */
    public ExtensionFolderManager(ReadOnlyObjectProperty<Path> extensionDirectoryPath) {
        this.extensionDirectoryPath = extensionDirectoryPath;

        this.manuallyInstalledExtensionsWatcher = new FilesWatcher(
                extensionDirectoryPath,
                isJar,
                path -> {
                    try {
                        return path.equals(extensionDirectoryPath.get().resolve(CATALOGS_FOLDER));
                    } catch (InvalidPathException | NullPointerException e) {
                        logger.debug("Error when trying to assess if {} should be watched", path, e);
                        return true;
                    }
                }
        );

        this.catalogManagedInstalledExtensionsWatcher = new FilesWatcher(
                extensionDirectoryPath.map(path -> {
                    if (path == null) {
                        return null;
                    } else {
                        try {
                            return getAndCreateCatalogsDirectory();
                        } catch (IOException | InvalidPathException | SecurityException | NullPointerException e) {
                            logger.debug("Error when getting catalog path from {}", path, e);
                            return null;
                        }
                    }
                }),
                isJar,
                path -> false
        );
    }

    @Override
    public void close() throws Exception {
        manuallyInstalledExtensionsWatcher.close();
        catalogManagedInstalledExtensionsWatcher.close();
    }

    /**
     * @return a read only property containing the path to the extension folder.
     * It may be updated from any thread and the path (but not the property) can be null
     * or invalid
     */
    public ReadOnlyObjectProperty<Path> getExtensionDirectoryPath() {
        return extensionDirectoryPath;
    }

    /**
     * Save the provided registry to disk. It can later be retrieved with
     * {@link #getSavedRegistry()}.
     *
     * @param registry the registry to save
     * @throws IOException if an I/O error occurs while writing the registry file
     * @throws SecurityException if the user doesn't have sufficient rights to save the file
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null
     * or if the provided registry is null
     */
    public synchronized void saveRegistry(Registry registry) throws IOException {
        try (
                FileWriter fileWriter = new FileWriter(getRegistryPath().toFile());
                BufferedWriter writer = new BufferedWriter(fileWriter)
        ) {
            writer.write(gson.toJson(Objects.requireNonNull(registry)));
            logger.debug("Registry containing {} saved", registry.catalogs().stream().map(SavedCatalog::name).toList());
        }
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
     * Get the path to the directory containing the provided catalog. This will create the
     * "catalogs" directory (see the class description) if it doesn't already exist.
     *
     * @param savedCatalog the catalog to retrieve
     * @return the path to the directory containing the provided catalog
     * @throws IOException if an I/O error occurs while creating the directory
     * @throws InvalidPathException if the path cannot be created
     * @throws SecurityException if the user doesn't have enough rights to create the directory
     * @throws NullPointerException if the provided catalog is null or if the path contained in
     * {@link #getExtensionDirectoryPath()} is null
     */
    public synchronized Path getCatalogDirectoryPath(SavedCatalog savedCatalog) throws IOException {
        return getAndCreateCatalogsDirectory().resolve(FileTools.stripInvalidFilenameCharacters(savedCatalog.name()));
    }

    /**
     * Delete all extensions belonging to the provided catalog. This will move the directory
     * returned by {@link #getCatalogDirectoryPath(SavedCatalog)} to trash or recursively delete
     * it if moving to trash is not supported by this platform.
     *
     * @param savedCatalog the catalog owning the extensions to delete
     * @throws IOException if an I/O error occur while deleting the files
     * @throws SecurityException if the user doesn't have sufficient rights to delete
     * the catalog
     * @throws java.nio.file.InvalidPathException if the path to the catalog directory cannot be created
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null
     * or if the provided catalog is null
     */
    public synchronized void deleteExtensionsFromCatalog(SavedCatalog savedCatalog) throws IOException {
        File catalogDirectory = getCatalogDirectoryPath(savedCatalog).toFile();
        FileTools.moveDirectoryToTrashOrDeleteRecursively(catalogDirectory);
        logger.debug("The extension files of {} located in {} have been deleted", savedCatalog.name(), catalogDirectory);
    }

    /**
     * Get the path to the directory containing the provided extension of the provided
     * catalog. This will create the directory containing all extensions of the provided
     * catalog if it doesn't already exist.
     *
     * @param savedCatalog the catalog owning the extension
     * @param extension the extension to retrieve
     * @return the path to the folder containing the provided extension
     * @throws IOException if an I/O error occurs while creating the directory
     * @throws InvalidPathException if the path cannot be created
     * @throws SecurityException if the user doesn't have enough rights to create the directory
     * @throws NullPointerException if one of the provided parameter is null or if the path contained in
     * {@link #getExtensionDirectoryPath()} is null
     */
    public synchronized Path getExtensionDirectoryPath(SavedCatalog savedCatalog, Extension extension) throws IOException {
        return getCatalogDirectoryPath(savedCatalog).resolve(FileTools.stripInvalidFilenameCharacters(extension.name()));
    }

    /**
     * Indicate whether an extension belonging to a catalog is installed. If that's the
     * case, installation information are returned.
     *
     * @param savedCatalog the catalog owning the extension to search
     * @param extension the extension to search
     * @return an empty Optional if the provided extension is not installed, or information
     * on the installed extension
     * @throws IOException if an I/O error occurs when searching for the extension
     * @throws java.nio.file.InvalidPathException if the Path object of the extension cannot be created, for example
     * because the extensions folder path contain invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to search for extension files
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null or
     * if one of the parameters is null
     */
    public synchronized Optional<InstalledExtension> getInstalledExtension(SavedCatalog savedCatalog, Extension extension) throws IOException {
        Path extensionPath = getExtensionDirectoryPath(savedCatalog, extension);

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
            logger.debug("No folder found in {}. Guessing {} is not installed", extensionPath, extension.name());
            return Optional.empty();
        }

        Path mainJarFolderPath = Paths.get(
                versionPath.toString(),
                FileType.MAIN_JAR.name
        );
        if (!FileTools.isDirectoryNotEmpty(mainJarFolderPath)) {
            logger.debug(
                    "The folder at {} is not a non-empty directory. Guessing {} is not installed",
                    mainJarFolderPath,
                    extension.name()
            );
            return Optional.empty();
        }
        logger.debug("{} detected at {}", extension.name(), mainJarFolderPath);

        Path optionalDependenciesFolderPath = Paths.get(
                versionPath.toString(),
                FileType.OPTIONAL_DEPENDENCIES.name
        );
        boolean optionalDependenciesInstalled = FileTools.isDirectoryNotEmpty(optionalDependenciesFolderPath);
        if (optionalDependenciesInstalled) {
            logger.debug(
                    "Optional dependencies of {} detected because {} is a non-empty directory",
                    extension.name(),
                    optionalDependenciesFolderPath
            );
        } else {
            logger.debug(
                    "Optional dependencies of {} not detected because {} is not a non-empty directory",
                    extension.name(),
                    optionalDependenciesFolderPath
            );
        }

        return Optional.of(new InstalledExtension(versionPath.toFile().getName(), optionalDependenciesInstalled));
    }

    /**
     * Delete all files of an extension belonging to a catalog. This will move the
     * {@link #getExtensionDirectoryPath(SavedCatalog, Extension)} directory to trash or
     * recursively delete it the platform doesn't support moving files to trash.
     *
     * @param savedCatalog the catalog owning the extension to delete
     * @param extension the extension to delete
     * @throws IOException if an I/O error occurs while deleting the folder
     * @throws java.nio.file.InvalidPathException if the Path object of the extension folder cannot be created, for example
     * because the extensions folder path contain invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to delete the folder
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null
     * or if one of the provided parameter is null
     */
    public synchronized void deleteExtension(SavedCatalog savedCatalog, Extension extension) throws IOException {
        FileTools.moveDirectoryToTrashOrDeleteRecursively(getExtensionDirectoryPath(savedCatalog, extension).toFile());
        logger.debug("The extension files of {} belonging to {} have been deleted", extension.name(), savedCatalog.name());
    }

    /**
     * Get (and create if asked and if it doesn't already exist) the path to the folder
     * containing the specified files of the provided extension at the specified
     * version belonging to the provided catalog.
     *
     * @param savedCatalog the catalog owning the extension
     * @param extension the extension to find the folder to
     * @param releaseName the version name of the extension to retrieve
     * @param fileType the type of files to retrieve
     * @param createDirectory whether to create a folder on the returned path
     * @return the path to the folder containing the specified files of the provided extension
     * @throws IOException if an I/O error occurs while creating the folder
     * @throws java.nio.file.InvalidPathException if the Path object cannot be created, for example
     * because the extensions folder path contain invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to create the folder
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null
     * or if one of the provided parameters is null
     */
    public synchronized Path getExtensionPath(
            SavedCatalog savedCatalog,
            Extension extension,
            String releaseName,
            FileType fileType,
            boolean createDirectory
    ) throws IOException {
        Path folderPath = Paths.get(
                getExtensionDirectoryPath(savedCatalog, extension).toString(),
                releaseName,
                fileType.name
        );

        if (createDirectory) {
            if (Files.isRegularFile(folderPath)) {
                logger.debug("Deleting {} because it should be a directory", folderPath);
                Files.deleteIfExists(folderPath);
            }
            Files.createDirectories(folderPath);
        }

        return folderPath;
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were
     * manually added (i.e. not with a catalog) to the extension directory
     */
    public ObservableList<Path> getManuallyInstalledJars() {
        return manuallyInstalledExtensionsWatcher.getFiles();
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were
     * added with catalogs to the extension directory
     */
    public ObservableList<Path> getCatalogManagedInstalledJars() {
        return catalogManagedInstalledExtensionsWatcher.getFiles();
    }

    private Path getRegistryPath() throws IOException {
        return getAndCreateCatalogsDirectory().resolve(REGISTRY_NAME);
    }

    private Path getAndCreateCatalogsDirectory() throws IOException {
        Path catalogsFolder = extensionDirectoryPath.get().resolve(CATALOGS_FOLDER);

        if (Files.isRegularFile(catalogsFolder)) {
            logger.debug("Deleting {} because it should be a directory", catalogsFolder);
            Files.deleteIfExists(catalogsFolder);
        }
        Files.createDirectories(catalogsFolder);

        return catalogsFolder;
    }
}
