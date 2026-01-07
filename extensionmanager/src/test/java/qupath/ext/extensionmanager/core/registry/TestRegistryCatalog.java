package qupath.ext.extensionmanager.core.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

public class TestRegistryCatalog {

    @Test
    void Check_Null_Name() {
        String name = null;
        String description = "description";
        URI uri = URI.create("http://uri.com");
        URI rawUri = URI.create("http://raw.com");
        boolean deletable = true;
        List<RegistryExtension> extensions = List.of(new RegistryExtension(
                "name",
                "installed version",
                false
        ));

        Assertions.assertThrows(
                RuntimeException.class,
                () -> new RegistryCatalog(name, description, uri, rawUri, deletable, extensions)
        );
    }

    @Test
    void Check_Null_Description() {
        String name = "name";
        String description = null;
        URI uri = URI.create("http://uri.com");
        URI rawUri = URI.create("http://raw.com");
        boolean deletable = true;
        List<RegistryExtension> extensions = List.of(new RegistryExtension(
                "name",
                "installed version",
                false
        ));

        Assertions.assertThrows(
                RuntimeException.class,
                () -> new RegistryCatalog(name, description, uri, rawUri, deletable, extensions)
        );
    }

    @Test
    void Check_Null_Uri() {
        String name = "name";
        String description = "description";
        URI uri = null;
        URI rawUri = URI.create("http://raw.com");
        boolean deletable = true;
        List<RegistryExtension> extensions = List.of(new RegistryExtension(
                "name",
                "installed version",
                false
        ));

        Assertions.assertThrows(
                RuntimeException.class,
                () -> new RegistryCatalog(name, description, uri, rawUri, deletable, extensions)
        );
    }

    @Test
    void Check_Null_Raw_Uri() {
        String name = "name";
        String description = "description";
        URI uri = URI.create("http://uri.com");
        URI rawUri = null;
        boolean deletable = true;
        List<RegistryExtension> extensions = List.of(new RegistryExtension(
                "name",
                "installed version",
                false
        ));

        Assertions.assertThrows(
                RuntimeException.class,
                () -> new RegistryCatalog(name, description, uri, rawUri, deletable, extensions)
        );
    }

    @Test
    void Check_Null_Extensions() {
        String name = "name";
        String description = "description";
        URI uri = URI.create("http://uri.com");
        URI rawUri = URI.create("http://raw.com");
        boolean deletable = true;
        List<RegistryExtension> extensions = null;

        Assertions.assertThrows(
                RuntimeException.class,
                () -> new RegistryCatalog(name, description, uri, rawUri, deletable, extensions)
        );
    }
}
