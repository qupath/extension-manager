package qupath.ext.extensionmanager.core.registry;

import java.util.Objects;

/**
 * An installed extension.
 * <p>
 * A {@link RuntimeException} is thrown if one parameter is null.
 *
 * @param name the name of the extension
 * @param installedVersion the name of the version of this extension that is currently installed
 * @param optionalDependenciesInstalled whether optional dependencies are currently installed
 */
public record RegistryExtension(String name, String installedVersion, boolean optionalDependenciesInstalled) {

    public RegistryExtension {
        Objects.requireNonNull(name);
        Objects.requireNonNull(installedVersion);
    }
}
