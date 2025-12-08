package qupath.ext.extensionmanager.core.tmp;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;

//TODO: make thread-safe
public class RegistryManager implements AutoCloseable {

    private static final String REGISTRY_NAME = "registry.json";
    private static final Logger logger = LoggerFactory.getLogger(RegistryManager.class);
    private static final Gson gson = new Gson();
    private final ChangeListener<? super Path> parentDirectoryListener = (p, o, n) -> {
        if (n == null) {
            this.registry = null;
        } else {
            try {
                Path registryPath = n.resolve(REGISTRY_NAME);

                //TODO: save current registry to new path, or update current registry to new json registry?

                if (registryPath.toFile().exists()) {
                    try(
                            FileReader fileReader = new FileReader(registryPath.toFile());
                            JsonReader jsonReader = new JsonReader(fileReader)
                    ) {
                        this.registry = gson.fromJson(jsonReader, Registry2.class);
                    }
                }
            } catch (Exception e) {

            }
        }
    };
    private final ObservableValue<Path> parentDirectoryObservable;
    private Registry2 registry = null;

    public RegistryManager(ObservableValue<Path> parentDirectoryObservable) {
        this.parentDirectoryObservable = parentDirectoryObservable;

        parentDirectoryObservable.addListener(parentDirectoryListener);
        parentDirectoryListener.changed(parentDirectoryObservable, null, parentDirectoryObservable.getValue());
    }

    @Override
    public void close() throws Exception {
        parentDirectoryObservable.removeListener(parentDirectoryListener);
    }

    public Registry2 getSavedRegistry() {
        return registry;
    }

    public void addCatalog(Catalog2 catalog) {

    }

    public void removeCatalog(Catalog2 catalog) {

    }

    public void addOrUpdateExtension(Catalog2 catalog, Extension2 extension) {

    }

    public void removeExtension(Catalog2 catalog, Extension2 extension) {

    }
}
