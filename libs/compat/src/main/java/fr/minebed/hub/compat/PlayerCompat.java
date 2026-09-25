package fr.minebed.hub.compat;

import fr.minebed.hub.nms.LegacyPackets;
import fr.minebed.hub.nms.Reflect;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Ce que l'API du joueur offre selon les versions : titre, barre d'action, ping. Chaque méthode a un repli et ne lève jamais d'erreur. */
public final class PlayerCompat {

    private static final Method GET_PING = Reflect.findMethod(Player.class, "getPing");
    private static final Method SEND_TITLE = Reflect.findMethod(Player.class, "sendTitle", String.class, String.class, int.class, int.class, int.class);
    private static final Method SPIGOT = Reflect.findMethod(Player.class, "spigot");
    // hidePlayer(Player) est remplacée par hidePlayer(Plugin, Player) à partir de la 1.12.2 : on prend la nouvelle si elle existe.
    private static final Method HIDE_WITH_PLUGIN = Reflect.findMethod(Player.class, "hidePlayer", Plugin.class, Player.class);
    private static final Method SHOW_WITH_PLUGIN = Reflect.findMethod(Player.class, "showPlayer", Plugin.class, Player.class);
    private static final Method HIDE_LEGACY = Reflect.findMethod(Player.class, "hidePlayer", Player.class);
    private static final Method SHOW_LEGACY = Reflect.findMethod(Player.class, "showPlayer", Player.class);

    private PlayerCompat() {
    }

    /** Ping en millisecondes, ou -1 si le serveur ne sait pas le donner. */
    public static int ping(Player player) {
        Object viaApi = Reflect.invoke(GET_PING, player);
        if (viaApi instanceof Integer) {
            return (Integer) viaApi;
        }
        return LegacyPackets.ping(player);
    }

    /** Affiche un titre ; à défaut de mieux, envoie le texte dans le chat. Renvoie {@code true} si un vrai titre a été affiché. */
    public static boolean sendTitle(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (SEND_TITLE != null) {
            try {
                SEND_TITLE.invoke(player, title == null ? "" : title, subtitle == null ? "" : subtitle, fadeInTicks, stayTicks, fadeOutTicks);
                return true;
            } catch (ReflectiveOperationException | RuntimeException e) {
                // repli ci-dessous
            }
        }
        if (LegacyPackets.sendTitle(player, title, subtitle, fadeInTicks, stayTicks, fadeOutTicks)) {
            return true;
        }
        if (title != null && !title.isEmpty()) {
            player.sendMessage(title);
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            player.sendMessage(subtitle);
        }
        return false;
    }

    /** Affiche un message au-dessus de la barre d'objets ; à défaut, dans le chat. Renvoie {@code true} si la barre d'action a servi. */
    public static boolean sendActionBar(Player player, String message) {
        if (sendActionBarViaSpigot(player, message) || LegacyPackets.sendActionBar(player, message)) {
            return true;
        }
        player.sendMessage(message);
        return false;
    }

    /** {@code player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message))} par réflexion (Spigot/Paper 1.9+). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean sendActionBarViaSpigot(Player player, String message) {
        try {
            Object spigot = Reflect.invoke(SPIGOT, player);
            Class<?> typeClass = Reflect.findClass("net.md_5.bungee.api.ChatMessageType");
            Class<?> componentClass = Reflect.findClass("net.md_5.bungee.api.chat.BaseComponent");
            Class<?> textClass = Reflect.findClass("net.md_5.bungee.api.chat.TextComponent");
            if (spigot == null || typeClass == null || componentClass == null || textClass == null) {
                return false;
            }
            Object actionBar = Enum.valueOf((Class<Enum>) typeClass, "ACTION_BAR");
            Object arrayOfComponents = java.lang.reflect.Array.newInstance(componentClass, 1);
            java.lang.reflect.Array.set(arrayOfComponents, 0, textClass.getConstructor(String.class).newInstance(message));
            Class<?> spigotType = Reflect.findClass("org.bukkit.entity.Player$Spigot");
            Method send = Reflect.findMethod(spigotType, "sendMessage", typeClass, arrayOfComponents.getClass());
            if (send == null) {
                return false;
            }
            send.invoke(spigot, actionBar, arrayOfComponents);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** Vrai si ce serveur permet de cacher un joueur à un autre. */
    public static boolean canHidePlayers() {
        return HIDE_WITH_PLUGIN != null || HIDE_LEGACY != null;
    }

    /**
     * Cache {@code target} aux yeux de {@code viewer} (à appeler dans la région de {@code viewer} sur Folia).
     * @return {@code true} si l'appel a réussi
     */
    public static boolean hide(Player viewer, Player target, Plugin plugin) {
        return call(HIDE_WITH_PLUGIN, HIDE_LEGACY, viewer, target, plugin);
    }

    /** Montre de nouveau {@code target} à {@code viewer}. */
    public static boolean show(Player viewer, Player target, Plugin plugin) {
        return call(SHOW_WITH_PLUGIN, SHOW_LEGACY, viewer, target, plugin);
    }

    private static boolean call(Method withPlugin, Method legacy, Player viewer, Player target, Plugin plugin) {
        try {
            if (withPlugin != null) {
                withPlugin.invoke(viewer, plugin, target);
                return true;
            }
            if (legacy != null) {
                legacy.invoke(viewer, target);
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
        return false;
    }
}
