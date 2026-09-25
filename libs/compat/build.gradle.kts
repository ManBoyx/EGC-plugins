plugins {
    id("hub.java-conventions")
}

description = "Couche de compatibilité : plateforme, planificateur Bukkit/Folia, matériaux, enchantements, sons, titres."

dependencies {
    api(project(":libs:common"))
    api(project(":libs:nms"))
    compileOnly(libs.spigot.api)
    testImplementation(libs.spigot.api)
}
