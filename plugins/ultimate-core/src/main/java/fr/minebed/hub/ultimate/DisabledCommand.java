package fr.minebed.hub.ultimate;

import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/** Exécuteur des commandes dont le module est coupé : dit clairement pourquoi rien ne se passe. */
final class DisabledCommand implements CommandExecutor, TabCompleter {

    static final DisabledCommand INSTANCE = new DisabledCommand();
    static volatile Messages messages;

    private DisabledCommand() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages m = messages;
        sender.sendMessage(m == null ? "Cette fonction est désactivée." : m.raw(Msg.PREFIX) + m.raw(Msg.MODULE_DISABLED));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
