package qupath.ext.extensionmanager.core.catalog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.extensionmanager.core.Version;

public class TestUpdateAvailable {

    @Test
    void Check_Null_Extension_Name() {
        String extensionName = null;
        Version currentVersion = new Version("v0.1.2");
        Version newVersion = new Version("v1.2.3");

        Assertions.assertThrows(
                RuntimeException.class,
                () -> new UpdateAvailable(extensionName, currentVersion, newVersion)
        );
    }

    @Test
    void Check_Null_Current_Version() {
        String extensionName = "extension name";
        Version currentVersion = null;
        Version newVersion = new Version("v1.2.3");

        Assertions.assertThrows(
                RuntimeException.class,
                () -> new UpdateAvailable(extensionName, currentVersion, newVersion)
        );
    }

    @Test
    void Check_Null_New_Version() {
        String extensionName = "extension name";
        Version currentVersion = new Version("v0.1.2");
        Version newVersion = null;

        Assertions.assertThrows(
                RuntimeException.class,
                () -> new UpdateAvailable(extensionName, currentVersion, newVersion)
        );
    }
}
