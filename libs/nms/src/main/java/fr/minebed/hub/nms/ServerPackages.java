package fr.minebed.hub.nms;

/**
 * Noms des paquets internes du serveur. Jusqu'à la 1.16 ils portent la version (« v1_8_R3 ») ; ensuite les noms changent
 * (cartographie Mojang) : c'est pourquoi les paquets « historiques » ne sont utilisés que là où l'API ne fournit rien.
 */
public final class ServerPackages {

    private final String craftPackage;
    private final String nmsPackage;

    private ServerPackages(String craftPackage, String nmsPackage) {
        this.craftPackage = craftPackage;
        this.nmsPackage = nmsPackage;
    }

    /**
     * @param craftServerClassName nom complet de la classe du serveur, ex. {@code org.bukkit.craftbukkit.v1_8_R3.CraftServer}
     */
    public static ServerPackages fromCraftServerClass(String craftServerClassName) {
        int dot = craftServerClassName.lastIndexOf('.');
        String pkg = dot < 0 ? "" : craftServerClassName.substring(0, dot);
        int lastDot = pkg.lastIndexOf('.');
        String last = lastDot < 0 ? pkg : pkg.substring(lastDot + 1);
        boolean versioned = last.matches("v\\d+_\\d+_R\\d+");
        return new ServerPackages(pkg, versioned ? "net.minecraft.server." + last : null);
    }

    public String craftPackage() {
        return craftPackage;
    }

    /** Vrai seulement avec les serveurs à paquets versionnés (jusqu'à la 1.16 et Spigot classique jusqu'à la 1.20). */
    public boolean hasLegacyNms() {
        return nmsPackage != null;
    }

    public Class<?> nms(String simpleName) {
        return nmsPackage == null ? null : Reflect.findClass(nmsPackage + "." + simpleName);
    }

    public Class<?> craft(String simpleName) {
        return Reflect.findClass(craftPackage + "." + simpleName);
    }
}
