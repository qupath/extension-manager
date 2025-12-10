package qupath.ext.extensionmanager.core.registry;

import qupath.ext.extensionmanager.core.catalog.Catalog;

import java.util.List;

public record Registry(List<RegistryCatalog> catalogs) {

    public static Registry createFromCatalogs(List<Catalog> catalogs) {

    }
}
