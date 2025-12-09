package qupath.ext.extensionmanager.core.registry;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.catalog.Catalog;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

//TODO: make thread-safe, more simple
public class RegistryManager implements AutoCloseable {

    private static final String REGISTRY_NAME = "registry.json";
    private static final Logger logger = LoggerFactory.getLogger(RegistryManager.class);
    private static final Gson gson = new Gson();
    private final ChangeListener<? super Path> parentDirectoryListener = (p, o, n) ->
            this.catalogs.setAll(getSavedRegistry(n).catalogs());
    private final ObservableValue<Path> parentDirectoryObservable;

    /**
     *
     * @param parentDirectoryObservable
     * @throws NullPointerException
     */
    public RegistryManager(ObservableValue<Path> parentDirectoryObservable) {
        this.parentDirectoryObservable = parentDirectoryObservable;

        parentDirectoryObservable.addListener(parentDirectoryListener);
        parentDirectoryListener.changed(parentDirectoryObservable, null, parentDirectoryObservable.getValue());
    }

    @Override
    public void close() {
        parentDirectoryObservable.removeListener(parentDirectoryListener);
    }

    public List<Catalog> getSavedCatalogs() throws IOException {

    }

    /**
     *
     * @param catalog
     * @throws IllegalArgumentException
     * @throws NullPointerException
     * @throws IOException
     */
    public void addCatalog(RegistryCatalog catalog) throws IOException {
        Objects.requireNonNull(catalog);

        if (catalogs.stream().map(RegistryCatalog::name).anyMatch(catalogName -> catalogName.equals(catalog.name()))) {
            throw new IllegalArgumentException();
        }

        try (
                FileWriter fileWriter = new FileWriter(parentDirectoryObservable.getValue().toFile());
                BufferedWriter writer = new BufferedWriter(fileWriter)
        ) {
            List<RegistryCatalog> newCatalogs = new ArrayList<>(catalogs);
            newCatalogs.add(catalog);
            writer.write(gson.toJson(new Registry(newCatalogs)));

            catalogs.add(catalog);      // add catalog only if write succeeds
        }
    }

    public ObservableList<RegistryCatalog> getCatalogs() {
        return catalogsImmutable;
    }

    /**
     *
     * @param catalog
     * @throws IllegalArgumentException
     * @throws NullPointerException
     * @throws IOException
     */
    public void removeCatalog(RegistryCatalog catalog) throws IOException {
        if (!catalog.deletable()) {
            throw new IllegalArgumentException();
        }

        try (
                FileWriter fileWriter = new FileWriter(parentDirectoryObservable.getValue().toFile());
                BufferedWriter writer = new BufferedWriter(fileWriter)
        ) {
            List<RegistryCatalog> newCatalogs = new ArrayList<>(catalogs);
            newCatalogs.remove(catalog);
            writer.write(gson.toJson(new Registry(newCatalogs)));

            catalogs.remove(catalog);      // remove catalog only if write succeeds
        }
    }

    public void addOrUpdateExtension(RegistryCatalog catalog, RegistryExtension extension) {

    }

    public void removeExtension(RegistryCatalog catalog, RegistryExtension extension) {

    }

    private Registry getSavedRegistry(Path parentDirectory) {
        if (parentDirectory == null) {
            logger.debug(
                    "Current parent directory path null. Resetting registry to default catalogs {}",
                    defaultCatalogs.stream().map(RegistryCatalog::name).toList()
            );
            return new Registry(defaultCatalogs);
        }

        Path registryPath;
        try {
            registryPath = parentDirectory.resolve(REGISTRY_NAME);
        } catch (InvalidPathException e) {
            logger.debug(
                    "Cannot create registry path. Resetting registry to default catalogs {}",
                    defaultCatalogs.stream().map(RegistryCatalog::name).toList(),
                    e
            );
            return new Registry(defaultCatalogs);
        }

        try(
                FileReader fileReader = new FileReader(registryPath.toFile());
                JsonReader jsonReader = new JsonReader(fileReader)
        ) {
            return gson.fromJson(jsonReader, Registry.class);
        } catch (IOException e) {
            logger.debug(
                    "Cannot open registry at {}. Resetting registry to default catalogs {}",
                    registryPath,
                    defaultCatalogs.stream().map(RegistryCatalog::name).toList(),
                    e
            );
            return new Registry(defaultCatalogs);
        }
    }
}
