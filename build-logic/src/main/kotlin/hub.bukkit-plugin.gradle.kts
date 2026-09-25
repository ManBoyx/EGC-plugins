// Règles d'un plugin Bukkit : jar unique (les bibliothèques internes y sont incluses) et plugin.yml complété.

plugins {
    id("hub.java-conventions")
    id("com.gradleup.shadow")
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("plugin.yml") {
        expand("version" to version)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set(project.name)
    // Réduit la taille sans casser la réflexion : on ne minimise pas, on laisse tout.
    mergeServiceFiles()
}

tasks.jar {
    // Le jar « nu » ne sert à rien sur un serveur : seul le shadowJar est livré.
    archiveClassifier.set("thin")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
