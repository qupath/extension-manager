package qupath.ext.extensionmanager.core.savedentities;

import java.util.Collections;
import java.util.List;

/**
 * A class containing information regarding a list of saved catalogs.
 *
 * @param catalogs the saved catalogs. This list is immutable and won't be null
 */
public record Registry(List<SavedCatalog> catalogs) {

    /**
     * Create a registry containing the provided list of catalogs.
     *
     * @param catalogs the catalogs this registry should contain
     * @throws NullPointerException if the provided list is null
     */
    public Registry(List<SavedCatalog> catalogs) {
        this.catalogs = Collections.unmodifiableList(catalogs);
    }
}
