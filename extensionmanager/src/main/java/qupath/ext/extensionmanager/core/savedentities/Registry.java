package qupath.ext.extensionmanager.core.savedentities;

import java.util.Collections;
import java.util.List;

/**
 * A class containing information regarding a list of saved indexes.
 *
 * @param indexes the saved indexes
 */
public record Registry(List<SavedIndex> indexes) {

    /**
     * Create a registry containing the provided list of indexes.
     *
     * @param indexes the indexes this registry should contain
     */
    public Registry(List<SavedIndex> indexes) {
        this.indexes = Collections.unmodifiableList(indexes);
    }

    /**
     * @return an unmodifiable view of the indexes of this registry
     */
    @Override
    public List<SavedIndex> indexes() {
        return indexes;
    }
}
