package qupath.ext.extensionmanager.core.savedentities;

import qupath.ext.extensionmanager.core.Version;

/**
 * An object indicating an update available.
 *
 * @param extensionName the name of the updatable extension
 * @param currentVersion the current version of the updatable extension
 * @param newVersion the most recent and compatible version of the updatable extension
 */
public record UpdateAvailable(String extensionName, Version currentVersion, Version newVersion) {}
