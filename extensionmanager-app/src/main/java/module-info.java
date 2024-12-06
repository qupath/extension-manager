/**
 * This module contains an application to start the extension manager.
 */
module qupath.ext.extensionmanager.app {
    requires javafx.graphics;
    requires qupath.ext.extensionmanager;

    opens qupath.ext.extensionmanager.app to javafx.graphics;
}