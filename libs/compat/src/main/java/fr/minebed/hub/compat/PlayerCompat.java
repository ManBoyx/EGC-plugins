package fr.minebed.hub.compat;

import fr.minebed.hub.nms.LegacyPackets;
import fr.minebed.hub.nms.Reflect;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;

/** Ce que l'API du joueur offre selon les versions : titre, barre d'action, ping. Chaque méthode a un repli et ne lève jamais d'erreur. */
public final class PlayerCompat {

    private static final Method GET_PING = Reflect.findMethod(Player.class, "getPing");
    private static final Method SEND_TITLE = Reflect.findMethod(Player.class, "sendTitle", String.class, String.class, int.class, int.class, int.class);
    private static final Method SPIGOT = Reflect.findMethod(Player.class, "spigot");

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
}
