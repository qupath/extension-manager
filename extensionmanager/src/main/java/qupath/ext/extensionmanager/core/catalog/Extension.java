package qupath.ext.extensionmanager.core.catalog;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.Version;
import qupath.ext.extensionmanager.core.model.ExtensionModel;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An optionally installed extension.
 */
public class Extension {

    private static final Logger logger = LoggerFactory.getLogger(Extension.class);
    private final String name;
    private final String description;
    private final List<Release> releases;
    private final URI homepage;
    private final boolean starred;
    private final ObjectProperty<Optional<Release>> installedRelease;
    private final BooleanProperty optionalDependenciesInstalled;

    /**
     * Create an extension from a {@link ExtensionModel}.
     *
     * @param extensionModel information on the extension
     * @param installedRelease the release of the extension that is currently installed. Can be null to indicate that the
     *                         extension is not installed
     * @param optionalDependenciesInstalled whether optional dependencies of the extension are currently installed
     * @throws NullPointerException if the provided extension model is null
     */
    public Extension(ExtensionModel extensionModel, Release installedRelease, boolean optionalDependenciesInstalled) {
        this.name = extensionModel.name();
        this.description = extensionModel.description();
        this.releases = extensionModel.releases().stream().map(Release::new).toList();
        this.homepage = extensionModel.homepage();
        this.starred = extensionModel.starred();
        this.installedRelease = new SimpleObjectProperty<>(Optional.ofNullable(installedRelease));
        this.optionalDependenciesInstalled = new SimpleBooleanProperty(optionalDependenciesInstalled);
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * @return the name of the extension
     */
    public String getName() {
        return name;
    }

    /**
     * @return a short (one sentence or so) description of what the extension is and what it does
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return a list of available releases of the extension. This list is immutable
     */
    public List<Release> getReleases() {
        return releases;
    }

    /**
     * @return a link to the GitHub repository associated with the extension
     */
    public URI getHomepage() {
        return homepage;
    }

    /**
     * @return whether the extension is generally useful or recommended for most users
     */
    public boolean isStarred() {
        return starred;
    }

    /**
     * Get an observable value showing the currently installed release of this extension. An empty Optional indicates
     * that the extension is not installed.
     * <p>
     * This observable may be updated from any thread.
     *
     * @return an observable value showing the currently installed release of this extension
     */
    public ObservableValue<Optional<Release>> getInstalledRelease() {
        return installedRelease;
    }

    /**
     * Get an observable boolean value showing whether optional dependencies are currently installed.
     * <p>
     * This observable may be updated from any thread.
     *
     * @return observable boolean value showing whether optional dependencies are currently installed
     */
    public ObservableBooleanValue areOptionalDependenciesInstalled() {
        return optionalDependenciesInstalled;
    }

    /**
     * Indicate that this extension is now installed with the provided release.
     *
     * @param release the installed release
     * @param optionalDependenciesInstalled whether optional dependencies have been installed
     * @throws NullPointerException if the provided release is null
     */
    public synchronized void installRelease(Release release, boolean optionalDependenciesInstalled) {
        this.installedRelease.set(Optional.of(Objects.requireNonNull(release)));
        this.optionalDependenciesInstalled.set(optionalDependenciesInstalled);
    }

    /**
     * Indicate that this extension is now uninstalled.
     */
    public synchronized void uninstallRelease() {
        installedRelease.set(Optional.empty());
        optionalDependenciesInstalled.set(false);
    }

    /**
     * Indicate whether this extension is installed, and if the installed version can be updated to a newer release that
     * is compatible with the provided version.
     *
     * @param version the version that the new release should be compatible to
     * @return a more up-to-date version of this extension, or an empty Optional if this extension is not installed or
     * is already installed with the latest compatible release
     */
    public Optional<UpdateAvailable> getUpdateAvailable(Version version) {
        Optional<Release> installedRelease = getInstalledRelease().getValue();

        if (installedRelease.isEmpty()) {
            logger.debug("{} not installed, so no update available", this);
            return Optional.empty();
        }

        Optional<Release> maxCompatibleRelease = getMaxCompatibleRelease(version);
        if (maxCompatibleRelease.isEmpty()) {
            logger.debug("{} installed but no compatible release with {} found", this, version);
            return Optional.empty();
        }

        if (maxCompatibleRelease.get().getVersion().compareTo(installedRelease.get().getVersion()) <= 0) {
            logger.debug("{} installed but corresponds to the latest compatible version", this);
            return Optional.empty();
        }

        logger.debug("{} installed and updatable to {}", this, maxCompatibleRelease.get());
        return Optional.of(new UpdateAvailable(
                name,
                installedRelease.get().getVersion(),
                maxCompatibleRelease.get().getVersion()
        ));
    }

    private Optional<Release> getMaxCompatibleRelease(Version version) {
        Release maxCompatibleRelease = null;

        for (Release release: releases) {
            if (
                    release.isCompatible(version) &&
                    (maxCompatibleRelease == null || release.getVersion().compareTo(maxCompatibleRelease.getVersion()) > 0)
            ) {
                maxCompatibleRelease = release;
            }
        }

        return Optional.ofNullable(maxCompatibleRelease);
    }
}
