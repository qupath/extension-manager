plugins {
    id("extensionmanager.java-conventions")
    application
    alias(libs.plugins.javafx)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":extensionmanager"))
    implementation(libs.logback)
}

application {
    mainClass = "qupath.ext.extensionmanager.app.ExtensionManagerApp"
    mainModule = "qupath.ext.extensionmanager.app"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

javafx {
    version = libs.versions.javafx.get()
    modules = listOf("javafx.controls", "javafx.fxml")
}