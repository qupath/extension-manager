plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "qupath.ext.extensionmanager"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.controlsfx:controlsfx:11.2.1")
}

application {
    mainClass = "qupath.ext.extensionmanager.Main"
}

javafx {
    version = "23.0.1"
    modules = listOf("javafx.controls", "javafx.fxml")
}