package qupath.ext.extensionmanager.core.savedentities;

import java.net.URI;

/**
 * Basic metadata on an index.
 *
 * @param name see {@link qupath.ext.extensionmanager.core.index.model.Index#name()}
 * @param description see {@link qupath.ext.extensionmanager.core.index.model.Index#description()}
 * @param uri the URI pointing to the raw content of the index
 */
public record SavedIndex(String name, String description, URI uri) {}
