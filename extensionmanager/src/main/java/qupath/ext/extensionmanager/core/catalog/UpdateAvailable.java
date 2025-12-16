package qupath.ext.extensionmanager.core.catalog;

import qupath.ext.extensionmanager.core.Version;

import java.util.Objects;

/**
 * An object indicating an update available.
 * <p>
 * A {@link RuntimeException} is thrown if one parameter is null.
 *
 * @param extensionName the name of the updatable extension
 * @param currentVersion the current version of the updatable extension
 * @param newVersion the most recent and compatible version of the updatable extension
 */
public record UpdateAvailable(String extensionName, Version currentVersion, Version newVersion) {

    public UpdateAvailable {
        Objects.requireNonNull(extensionName);
        Objects.requireNonNull(currentVersion);
        Objects.requireNonNull(newVersion);
    }
}
