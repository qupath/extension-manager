package qupath.ext.extensionmanager.core.savedentities;

/**
 * An object indicating an update available.
 *
 * @param extensionName the name of the updatable extension
 * @param currentVersion a text describing the current version of the updatable extension
 * @param newVersion a text describing the most recent and compatible version of the updatable extension
 */
public record UpdateAvailable(String extensionName, String currentVersion, String newVersion) {}
