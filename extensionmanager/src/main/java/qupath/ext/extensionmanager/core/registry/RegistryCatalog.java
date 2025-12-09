package qupath.ext.extensionmanager.core.registry;

import java.net.URI;
import java.util.List;

//TODO: enforce not null, also in others records
public record RegistryCatalog(String name, String description, URI uri, URI rawUri, boolean deletable, List<RegistryExtension> extensions) {

    public RegistryCatalog(String name, String description, URI uri, URI rawUri, boolean deletable) {
        this(name, description, uri, rawUri, deletable, List.of());
    }
}
