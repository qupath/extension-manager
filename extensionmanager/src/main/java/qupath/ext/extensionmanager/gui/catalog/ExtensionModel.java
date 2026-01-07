package qupath.ext.extensionmanager.gui.catalog;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import qupath.ext.extensionmanager.core.catalog.Extension;
import qupath.ext.extensionmanager.core.catalog.Release;
import qupath.ext.extensionmanager.gui.UiUtils;

import java.util.Optional;

/**
 * The model of UI elements representing an {@link Extension}. This is basically a wrapper around an {@link Extension}
 * where listenable properties are propagated to the JavaFX Application Thread.
 * <p>
 * An instance of this class must be {@link #close() closed} once no longer used.
 */
class ExtensionModel implements AutoCloseable {

    private final ObjectProperty<Optional<Release>> installedRelease = new SimpleObjectProperty<>();
    private final Extension extension;
    private final ChangeListener<? super Optional<Release>> installedReleaseListener;

    public ExtensionModel(Extension extension) {
        this.extension = extension;
        this.installedReleaseListener = UiUtils.bindPropertyInUIThread(installedRelease, extension.getInstalledRelease());
    }

    @Override
    public void close() {
        extension.getInstalledRelease().removeListener(installedReleaseListener);
    }

    public ObservableValue<Optional<Release>> getInstalledRelease() {
        return installedRelease;
    }
}
