package qupath.ext.extensionmanager.core.savedentities;

import qupath.ext.extensionmanager.core.index.Index;

import java.net.URI;

/**
 * Basic metadata on an index.
 *
 * @param name see {@link Index#name()}
 * @param description see {@link Index#description()}
 * @param uri a URI pointing to the raw content of the index, or to a GitHub repository where the
 *            index can be found
 * @param rawUri the URI pointing to the raw content of the index (can be same as {@link #uri})
 */
public record SavedIndex(String name, String description, URI uri, URI rawUri) {}
