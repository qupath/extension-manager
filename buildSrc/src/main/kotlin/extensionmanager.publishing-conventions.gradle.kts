plugins {
    java
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "SciJava"
            val releasesRepoUrl = uri("https://maven.scijava.org/content/repositories/releases")
            val snapshotsRepoUrl = uri("https://maven.scijava.org/content/repositories/snapshots")
            // Use gradle -Prelease publish
            url = if (project.hasProperty("release")) releasesRepoUrl else snapshotsRepoUrl
            credentials {
                username = System.getenv("MAVEN_USER")
                password = System.getenv("MAVEN_PASS")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "io.github.qupath"
            from(components["java"])

                    pom {
                        licenses {
                            license {
                                name = "Apache License v2.0"
                                url = "http://www.apache.org/licenses/LICENSE-2.0"
                            }
                        }
                    }
        }
    }
}