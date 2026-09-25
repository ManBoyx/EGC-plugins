// Projet racine : il ne compile rien lui-même, il rassemble les livrables.

val plugins = listOf(":plugins:ultimate-core")

tasks.register<Sync>("dist") {
    group = "distribution"
    description = "Rassemble les jars de plugins prêts à déposer dans le dossier plugins/ d'un serveur."
    plugins.forEach { path ->
        dependsOn("$path:shadowJar")
        from(project(path).tasks.named("shadowJar"))
    }
    into(layout.buildDirectory.dir("dist"))
    // Somme de contrôle à publier avec les jars (voir docs/PUBLISHING.md).
    doLast {
        val dir = layout.buildDirectory.dir("dist").get().asFile
        val lines = dir.listFiles { f -> f.extension == "jar" }!!.sortedBy { it.name }.map { f ->
            val hex = java.security.MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") { "%02x".format(it) }
            "$hex  ${f.name}"
        }
        File(dir, "SHA256SUMS").writeText(lines.joinToString("\n") + "\n")
    }
}
