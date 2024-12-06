# Extension manager

A manager for [indexes](https://github.com/qupath/extension-index-model) and extensions.

## Installing

The repository contains two subprojects:

* One subproject (`extensionmanager`) containing the implementation of the extension manager.
* One subproject (`extensionmanager-app`) to run the project as a standalone application. It is mainly used for development.

To use the extension manager:

```groovy
// build.gradle

dependencies {
  implementation "io.github.qupath:extensionmanager:1.0.0-SNAPSHOT"
}
```

If you don't use Java modules in your application, you also have to import the `javafx.controls` and `javafx.fxml` modules:

```groovy
// build.gradle

javafx {
    version = ...
    modules = [ 'javafx.controls', 'javafx.fxml' ]
}
```

Then, take a look at the `ExtensionManagerApp` class of `extensionmanager-app` to see
an example on how to use the extension manager.

## Building

You can build every module of the extension manager from source with:

```bash
./gradlew clean build
```

The outputs will be under each subproject's `build/libs`.