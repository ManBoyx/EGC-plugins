// Règles communes à tous les modules Java du dépôt.

plugins {
    `java-library`
}

group = providers.gradleProperty("hub.group").get()
version = providers.gradleProperty("hub.version").get()

repositories {
    // Les dépôts sont déclarés une seule fois dans settings.gradle.kts.
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Java 8 : les serveurs 1.8 à 1.16 tournent souvent sur Java 8. Compiler avec « release »
    // (et non « target ») interdit aussi d'utiliser une API Java plus récente que la 8.
    options.release.set(8)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Xlint:-options"))
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}
