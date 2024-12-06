/**
 * This module provides an {@link qupath.ext.extensionmanager.core.ExtensionIndexManager ExtensionIndexManager},
 * which can be controlled by an {@link qupath.ext.extensionmanager.gui.ExtensionManager ExtensionManager}.
 */
module qupath.ext.extensionmanager {
    exports qupath.ext.extensionmanager.core;
    exports qupath.ext.extensionmanager.core.index;
    exports qupath.ext.extensionmanager.core.savedentities;
    exports qupath.ext.extensionmanager.gui;

    requires javafx.base;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.controls;
    requires org.slf4j;
    requires java.desktop;
    requires org.controlsfx.controls;
    requires com.google.gson;
    requires java.net.http;

    opens qupath.ext.extensionmanager.core.index to com.google.gson;
    opens qupath.ext.extensionmanager.core.tools to com.google.gson;
    opens qupath.ext.extensionmanager.core.savedentities to com.google.gson;
    opens qupath.ext.extensionmanager.gui to javafx.fxml;
    opens qupath.ext.extensionmanager.gui.index to javafx.fxml;
}