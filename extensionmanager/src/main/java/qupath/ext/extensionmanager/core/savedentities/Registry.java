package qupath.ext.extensionmanager.core.savedentities;

import java.util.Collections;
import java.util.List;

/**
 * A class containing information regarding a list of saved indexes.
 */
public class Registry {

    private final List<SavedIndex> savedIndexes;

    /**
     * Create a registry containing the provided list of indexes.
     *
     * @param savedIndexes the indexes this registry should contain
     */
    public Registry(List<SavedIndex> savedIndexes) {
        this.savedIndexes = Collections.unmodifiableList(savedIndexes);
    }

    /**
     * @return an unmodifiable view of the indexes of this registry
     */
    public List<SavedIndex> getIndexes() {
        return savedIndexes;
    }
}
