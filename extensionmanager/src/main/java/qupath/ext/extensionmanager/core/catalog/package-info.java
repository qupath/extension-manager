/**
 * This package contains the model of a catalog as described on
 * <a href="https://github.com/qupath/extension-catalog-model">this
 * Pydantic model</a>.
 * <p>
 * However, there is a small difference between the Pydantic model and this package:
 * classes of this package use the camel case naming convention while the Pydantic
 * model uses the snake case naming convention.
 * <p>
 * A class ({@link qupath.ext.extensionmanager.core.catalog.CatalogFetcher CatalogFetcher}) is
 * also provided to fetch such catalog.
 */
package qupath.ext.extensionmanager.core.catalog;