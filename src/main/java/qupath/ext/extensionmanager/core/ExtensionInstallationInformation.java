package qupath.ext.extensionmanager.core;

/**
 * A class to store information regarding how an extension is installed.
 *
 * @param version a text describing the version of the extension
 * @param optionalDependenciesInstalled whether the optional dependencies of this extension are installed
 */
public record ExtensionInstallationInformation(String version, boolean optionalDependenciesInstalled) {}
