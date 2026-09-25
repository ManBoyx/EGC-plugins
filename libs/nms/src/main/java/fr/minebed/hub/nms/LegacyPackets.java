package fr.minebed.hub.nms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Titre, barre d'action et ping pour les vieux serveurs (1.8 à 1.11) dont l'API ne les offre pas.
 * Tout passe par la réflexion et chaque opération dit si elle a réussi : l'appelant prévoit un repli.
 */
public final class LegacyPackets {

    private static volatile Resolved resolved;

    private LegacyPackets() {
    }

    /** Vrai si les paquets historiques sont utilisables sur ce serveur. */
    public static boolean available() {
        return resolve().ok;
    }

    public static boolean sendActionBar(Player player, String message) {
        Resolved r = resolve();
        if (!r.ok || r.actionBarPacket == null) {
            return false;
        }
        try {
            Object component = r.chatComponentText.newInstance(message);
            Object packet = r.actionBarPacket.newInstance(component, (byte) 2);
            return send(r, player, packet);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    public static boolean sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Resolved r = resolve();
        if (!r.ok || r.titlePacket == null || r.titlePacketTimes == null) {
            return false;
        }
        try {
            Object times = r.titlePacketTimes.newInstance(r.titleTimes, null, fadeIn, stay, fadeOut);
            if (!send(r, player, times)) {
                return false;
            }
            if (subtitle != null && !subtitle.isEmpty()) {
                send(r, player, r.titlePacket.newInstance(r.titleSub, r.chatComponentText.newInstance(subtitle)));
            }
            return send(r, player, r.titlePacket.newInstance(r.titleMain, r.chatComponentText.newInstance(title == null ? "" : title)));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** Ping en millisecondes, ou -1 si inconnu. */
    public static int ping(Player player) {
        Resolved r = resolve();
        if (r.craftGetHandle == null || r.pingField == null) {
            return -1;
        }
        try {
            Object handle = r.craftGetHandle.invoke(player);
            return r.pingField.getInt(handle);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return -1;
        }
    }

    private static boolean send(Resolved r, Player player, Object packet) {
        try {
            Object handle = r.craftGetHandle.invoke(player);
            Object connection = r.playerConnection.get(handle);
            r.sendPacket.invoke(connection, packet);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static Resolved resolve() {
        Resolved local = resolved;
        if (local == null) {
            synchronized (LegacyPackets.class) {
                if (resolved == null) {
                    resolved = doResolve();
                }
                local = resolved;
            }
        }
        return local;
    }

    private static Resolved doResolve() {
        Resolved r = new Resolved();
        try {
            ServerPackages packages = ServerPackages.fromCraftServerClass(Bukkit.getServer().getClass().getName());
            if (!packages.hasLegacyNms()) {
                return r;
            }
            Class<?> craftPlayer = packages.craft("entity.CraftPlayer");
            Class<?> entityPlayer = packages.nms("EntityPlayer");
            Class<?> connection = packages.nms("PlayerConnection");
            Class<?> packet = packages.nms("Packet");
            Class<?> component = packages.nms("IChatBaseComponent");
            Class<?> componentText = packages.nms("ChatComponentText");
            if (craftPlayer == null || entityPlayer == null || connection == null || packet == null || component == null || componentText == null) {
                return r;
            }
            r.craftGetHandle = craftPlayer.getMethod("getHandle");
            r.playerConnection = Reflect.findField(entityPlayer, "playerConnection");
            r.sendPacket = connection.getMethod("sendPacket", packet);
            r.chatComponentText = componentText.getConstructor(String.class);
            r.pingField = Reflect.findField(entityPlayer, "ping");
            if (r.playerConnection == null) {
                return r;
            }
            Class<?> chatPacket = packages.nms("PacketPlayOutChat");
            if (chatPacket != null) {
                try {
                    r.actionBarPacket = chatPacket.getConstructor(component, byte.class);
                } catch (NoSuchMethodException e) {
                    r.actionBarPacket = null; // 1.12 : autre signature, l'API sait déjà faire
                }
            }
            Class<?> titlePacket = packages.nms("PacketPlayOutTitle");
            Class<?> titleAction = packages.nms("PacketPlayOutTitle$EnumTitleAction");
            if (titlePacket != null && titleAction != null) {
                r.titlePacket = titlePacket.getConstructor(titleAction, component);
                r.titlePacketTimes = titlePacket.getConstructor(titleAction, component, int.class, int.class, int.class);
                r.titleMain = enumConstant(titleAction, "TITLE");
                r.titleSub = enumConstant(titleAction, "SUBTITLE");
                r.titleTimes = enumConstant(titleAction, "TIMES");
            }
            r.ok = true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            r.ok = false;
        }
        return r;
    }

    private static Object enumConstant(Class<?> type, String name) {
        for (Object constant : type.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(name)) {
                return constant;
            }
        }
        return null;
    }

    private static final class Resolved {
        boolean ok;
        Method craftGetHandle;
        Field playerConnection;
        Field pingField;
        Method sendPacket;
        Constructor<?> chatComponentText;
        Constructor<?> actionBarPacket;
        Constructor<?> titlePacket;
        Constructor<?> titlePacketTimes;
        Object titleMain;
        Object titleSub;
        Object titleTimes;
    }
}
