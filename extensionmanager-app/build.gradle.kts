plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":extensionmanager"))
}

application {
    mainClass = "qupath.ext.extensionmanager.app.Main"
    mainModule = "qupath.ext.extensionmanager.app"
}

javafx {
    version = "23.0.1"
    modules = listOf("javafx.controls", "javafx.fxml")
}