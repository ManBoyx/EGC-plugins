package fr.minebed.hub.ultimate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Base des commandes : vérifie la permission avec un message traduit, attrape toute erreur pour ne jamais afficher une trace à
 * un joueur, et sait compléter les pseudos. Toutes les commandes doivent aussi marcher depuis la console : les récompenses
 * du site (Azuriom) les lancent ainsi, avec un pseudo en argument.
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final Ctx ctx;
    private final String permission;

    protected BaseCommand(Ctx ctx, String permission) {
        this.ctx = ctx;
        this.permission = permission;
    }

    /** @return {@code false} pour afficher l'usage de la commande */
    protected abstract boolean execute(CommandSender sender, String label, String[] args);

    protected List<String> complete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            ctx.messages.send(sender, Msg.NO_PERMISSION);
            return true;
        }
        try {
            if (!execute(sender, label, args)) {
                String usage = command.getUsage() == null ? "/" + label : command.getUsage().replace("<command>", label);
                ctx.messages.send(sender, Msg.USAGE, "usage", usage);
            }
        } catch (RuntimeException e) {
            ctx.plugin.getLogger().log(Level.SEVERE, "Erreur dans la commande /" + label + " " + String.join(" ", args), e);
            ctx.messages.send(sender, Msg.INTERNAL_ERROR);
        }
        return true;
    }

    @Override
    public final List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }
        try {
            return complete(sender, args);
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }

    /** Le joueur qui a lancé la commande, ou un message d'erreur et {@code null} si c'est la console. */
    protected Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        ctx.messages.send(sender, Msg.PLAYER_ONLY);
        return null;
    }

    /** Le joueur en ligne nommé, ou un message d'erreur et {@code null}. */
    protected Player requireOnline(CommandSender sender, String name) {
        Player target = ctx.findOnline(name);
        if (target == null) {
            ctx.messages.send(sender, Msg.PLAYER_NOT_FOUND, "name", name);
        }
        return target;
    }

    protected static List<String> onlineNames(String prefix) {
        List<String> out = new ArrayList<String>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(p.getName());
            }
        }
        return out;
    }

    protected static List<String> filter(Iterable<String> options, String prefix) {
        List<String> out = new ArrayList<String>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}
