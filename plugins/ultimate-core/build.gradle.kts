plugins {
    id("hub.java-conventions")
    id("hub.bukkit-plugin")
}

description = "EGC-plugins : socle de serveur (téléportations, kits, économie, chat, utilitaires) pour Bukkit, Spigot, Paper et Folia, de la 1.8 à aujourd'hui."

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:compat"))
    implementation(project(":libs:nms"))
    compileOnly(libs.spigot.api)
    compileOnly(libs.vault.api)
    testImplementation(libs.spigot.api)
}

// Le jar livré porte le nom du produit, pas celui du module.
tasks.shadowJar { archiveBaseName.set("EGC-plugins") }
tasks.jar { archiveBaseName.set("EGC-plugins") }
