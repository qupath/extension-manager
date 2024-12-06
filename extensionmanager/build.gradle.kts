plugins {
    id("extensionmanager.java-conventions")
    id("extensionmanager.publishing-conventions")
    alias(libs.plugins.javafx)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.slf4j)
    implementation(libs.gson)
    implementation(libs.controlsfx)
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