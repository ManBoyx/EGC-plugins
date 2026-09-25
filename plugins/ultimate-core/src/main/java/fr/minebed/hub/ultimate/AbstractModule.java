package fr.minebed.hub.ultimate;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/** Facilite l'enregistrement des commandes et des écouteurs d'un module, et leur retrait propre. */
public abstract class AbstractModule implements Module {

    protected final Ctx ctx;
    private final List<Listener> listeners = new ArrayList<Listener>();
    private final List<String> commands = new ArrayList<String>();

    protected AbstractModule(Ctx ctx) {
        this.ctx = ctx;
    }

    protected void listen(Listener listener) {
        listeners.add(listener);
        Bukkit.getPluginManager().registerEvents(listener, ctx.plugin);
    }

    protected void command(String name, BaseCommand executor) {
        PluginCommand command = ctx.plugin.getCommand(name);
        if (command == null) {
            ctx.plugin.getLogger().warning("Commande /" + name + " absente de plugin.yml : ignorée.");
            return;
        }
        command.setExecutor((CommandExecutor) executor);
        command.setTabCompleter((TabCompleter) executor);
        commands.add(name);
    }

    @Override
    public void disable() {
        for (Listener l : listeners) {
            HandlerList.unregisterAll(l);
        }
        listeners.clear();
        for (String name : commands) {
            PluginCommand command = ctx.plugin.getCommand(name);
            if (command != null) {
                command.setExecutor(DisabledCommand.INSTANCE);
                command.setTabCompleter(DisabledCommand.INSTANCE);
            }
        }
        commands.clear();
        onDisable();
    }

    /** À surcharger pour sauvegarder ou annuler des tâches. */
    protected void onDisable() {
    }
}
