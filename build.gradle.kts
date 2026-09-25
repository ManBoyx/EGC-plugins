// Projet racine : il ne compile rien lui-même, il rassemble les livrables.

val plugins = listOf(":plugins:ultimate-core")

tasks.register<Copy>("dist") {
    group = "distribution"
    description = "Rassemble les jars de plugins prêts à déposer dans le dossier plugins/ d'un serveur."
    plugins.forEach { path ->
        dependsOn("$path:shadowJar")
        from(project(path).tasks.named("shadowJar"))
    }
    into(layout.buildDirectory.dir("dist"))
}
