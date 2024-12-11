plugins {
    id("java-library")
}

repositories {
    mavenCentral()

    maven {
        name = "SciJava"
        url = uri("https://maven.scijava.org/content/repositories/releases")
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

group = "io.github.qupath"
version = rootProject.version

base {
    group = "io.github.qupath"
}