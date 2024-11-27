package qupath.ext.extensionmanager.core.savedentities;

/**
 * Installation information on an extension.
 *
 * @param releaseName the name of the installed release that should follow the
 *                   specifications of {@link qupath.ext.extensionmanager.core.Version}
 * @param optionalDependenciesInstalled whether optional dependencies are installed
 */
public record InstalledExtension(String releaseName, boolean optionalDependenciesInstalled) {}
