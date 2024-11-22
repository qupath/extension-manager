package qupath.ext.extensionmanager.core;

import java.net.URI;

/**
 * A class to store basic information regarding an index.
 *
 * @param name the index name
 * @param description an index description
 * @param url the URL pointing to the index
 */
public record IndexMetadata(String name, String description, URI url) {}

