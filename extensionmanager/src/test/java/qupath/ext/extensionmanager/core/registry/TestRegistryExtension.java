package qupath.ext.extensionmanager.core.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestRegistryExtension {

    @Test
    void Check_Null_Name() {
        String name = null;
        String installedVersion = "installed version";
        boolean optionalDependenciesInstalled = true;

        Assertions.assertThrows(
                RuntimeException.class,
                () -> new RegistryExtension(name, installedVersion, optionalDependenciesInstalled)
        );
    }

    @Test
    void Check_Null_Installed_Version() {
        String name = "name";
        String installedVersion = null;
        boolean optionalDependenciesInstalled = true;

        Assertions.assertThrows(
                RuntimeException.class,
                () -> new RegistryExtension(name, installedVersion, optionalDependenciesInstalled)
        );
    }
}
