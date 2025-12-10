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
import qupath.ext.extensionmanager.core.savedentities.UpdateAvailable;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Extension {

    private static final Logger logger = LoggerFactory.getLogger(Extension.class);
    private final ObjectProperty<Optional<Release>> installedRelease = new SimpleObjectProperty<>();
    private final BooleanProperty optionalDependenciesInstalled = new SimpleBooleanProperty();
    private final String name;
    private final String description;
    private final List<Release> releases;
    private final URI homepage;
    private final boolean starred;

    public Extension(String name, String description, List<Release> releases, URI homepage, boolean starred) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.releases = Objects.requireNonNull(releases);
        this.homepage = Objects.requireNonNull(homepage);
        this.starred = starred;

        //TODO: init installedRelease and optionalDependenciesInstalled
        //TODO: check equals, also for catalog and release
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Extension extension = (Extension) o;
        return starred == extension.starred && name.equals(extension.name) && description.equals(extension.description) &&
                releases.equals(extension.releases) && homepage.equals(extension.homepage);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + releases.hashCode();
        result = 31 * result + homepage.hashCode();
        result = 31 * result + Boolean.hashCode(starred);
        return result;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Release> getReleases() {
        return releases;
    }

    public URI getHomepage() {
        return homepage;
    }

    public boolean isStarred() {
        return starred;
    }

    public ObservableValue<Optional<Release>> getInstalledRelease() {
        return installedRelease;
    }

    public ObservableBooleanValue areOptionalDependenciesInstalled() {
        return optionalDependenciesInstalled;
    }

    public void installRelease(Release release) {
        installedRelease.set(Optional.of(Objects.requireNonNull(release)));
    }

    public void uninstallRelease() {
        installedRelease.set(Optional.empty());
    }

    public UpdateAvailable getUpdateAvailable(Version version) {
        Optional<Release> installedRelease = getInstalledRelease().getValue();

        if (installedRelease.isEmpty()) {
            logger.debug("{} not installed, so no update available", this);
            return null;
        }

        Optional<Release> maxCompatibleRelease = getMaxCompatibleRelease(version);
        if (maxCompatibleRelease.isEmpty()) {
            logger.debug("{} installed but no compatible release with {} found", this, version);
            return null;
        }

        if (maxCompatibleRelease.get().getVersion().compareTo(installedRelease.get().getVersion()) <= 0) {
            logger.debug("{} installed but corresponds to the latest compatible version", this);
            return null;
        }

        logger.debug("{} installed and updatable to {}", this, maxCompatibleRelease.get());
        return new UpdateAvailable(
                name,
                installedRelease.get().getVersion(),
                maxCompatibleRelease.get().getVersion()
        );
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
