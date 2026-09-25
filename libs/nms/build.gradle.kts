plugins {
    id("hub.java-conventions")
}

description = "Accès aux détails internes du serveur (NMS) par réflexion uniquement : un seul jar pour toutes les versions."

dependencies {
    compileOnly(libs.spigot.api)
    testImplementation(libs.spigot.api)
}
