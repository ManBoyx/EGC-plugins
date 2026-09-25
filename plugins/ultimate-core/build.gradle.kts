plugins {
    id("hub.java-conventions")
    id("hub.bukkit-plugin")
}

description = "UltimateCore : socle de serveur (téléportations, kits, économie, chat, utilitaires) pour Bukkit, Spigot, Paper et Folia, de la 1.8 à aujourd'hui."

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:compat"))
    implementation(project(":libs:nms"))
    compileOnly(libs.spigot.api)
    compileOnly(libs.vault.api)
    testImplementation(libs.spigot.api)
}
