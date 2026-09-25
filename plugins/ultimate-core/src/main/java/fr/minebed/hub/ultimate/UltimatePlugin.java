package fr.minebed.hub.ultimate;

import fr.minebed.hub.compat.Platform;
import fr.minebed.hub.compat.scheduler.Scheduler;
import fr.minebed.hub.compat.scheduler.Schedulers;
import fr.minebed.hub.ultimate.modules.AnnouncerModule;
import fr.minebed.hub.ultimate.modules.BackModule;
import fr.minebed.hub.ultimate.modules.ChatModule;
import fr.minebed.hub.ultimate.modules.EconomyModule;
import fr.minebed.hub.ultimate.modules.EnderChestModule;
import fr.minebed.hub.ultimate.modules.FreezeModule;
import fr.minebed.hub.ultimate.modules.HomesModule;
import fr.minebed.hub.ultimate.modules.InfoModule;
import fr.minebed.hub.ultimate.modules.JoinModule;
import fr.minebed.hub.ultimate.modules.KitsModule;
import fr.minebed.hub.ultimate.modules.RtpModule;
import fr.minebed.hub.ultimate.modules.SpawnModule;
import fr.minebed.hub.ultimate.modules.StaffModule;
import fr.minebed.hub.ultimate.modules.TpaModule;
import fr.minebed.hub.ultimate.modules.UtilityModule;
import fr.minebed.hub.ultimate.modules.VanishModule;
import fr.minebed.hub.ultimate.modules.WarpsModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * EGC-plugins : un seul jar pour Bukkit, Spigot, Paper et Folia, de la 1.8 à aujourd'hui.
 * Chaque fonctionnalité est un module que l'on coupe dans la configuration ; une erreur dans un module n'empêche pas les autres.
 */
public final class UltimatePlugin extends JavaPlugin {

    private static final int CONFIG_VERSION = 1;

    private Ctx ctx;
    private Scheduler scheduler;
    private Messages messages;
    private CoreModule core;
    private final List<Module> active = new ArrayList<Module>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Platform platform = Platform.current();
        scheduler = Schedulers.forPlugin(this);
        messages = new Messages(this, platform);
        messages.load(getConfig().getString("language", "fr"));
        DisabledCommand.messages = messages;
        ctx = new Ctx(this, platform, scheduler, messages);

        // Tant que leur module n'est pas actif, toutes les commandes répondent « fonction désactivée ».
        for (String name : getDescription().getCommands().keySet()) {
            PluginCommand command = getCommand(name);
            if (command != null) {
                command.setExecutor(DisabledCommand.INSTANCE);
                command.setTabCompleter(DisabledCommand.INSTANCE);
            }
        }
        core = new CoreModule(ctx, this);
        core.enable();
        checkConfigVersion();
        enableModules();
        getLogger().info("EGC-plugins " + getDescription().getVersion() + " activé sur " + platform + " (Java " + System.getProperty("java.version")
            + "). Modules : " + (active.isEmpty() ? "aucun" : String.join(", ", activeModuleIds())));
    }

    @Override
    public void onDisable() {
        disableModules();
        if (core != null) {
            core.disable();
        }
        if (scheduler != null) {
            scheduler.cancelAll();
        }
    }

    /** Relit la configuration et les messages, puis relance les modules. */
    void reloadAll() {
        disableModules();
        reloadConfig();
        messages.load(getConfig().getString("language", "fr"));
        checkConfigVersion();
        enableModules();
    }

    List<String> activeModuleIds() {
        List<String> ids = new ArrayList<String>();
        for (Module m : active) {
            ids.add(m.id());
        }
        return Collections.unmodifiableList(ids);
    }

    private void checkConfigVersion() {
        int found = getConfig().getInt("config-version", 0);
        if (found < CONFIG_VERSION) {
            getLogger().warning("Votre config.yml est ancien (version " + found + ", actuelle : " + CONFIG_VERSION
                + "). Les nouvelles options prennent leur valeur par défaut ; comparez avec le fichier d'origine du jar.");
        }
    }

    private void enableModules() {
        TeleportService teleports = new TeleportService(ctx);
        List<Module> wanted = new ArrayList<Module>();
        wanted.add(new SpawnModule(ctx, teleports));
        wanted.add(new HomesModule(ctx, teleports));
        wanted.add(new WarpsModule(ctx, teleports));
        wanted.add(new TpaModule(ctx, teleports));
        wanted.add(new BackModule(ctx, teleports));
        wanted.add(new KitsModule(ctx));
        wanted.add(new EconomyModule(ctx));
        wanted.add(new ChatModule(ctx));
        wanted.add(new UtilityModule(ctx));
        wanted.add(new JoinModule(ctx));
        wanted.add(new AnnouncerModule(ctx));
        wanted.add(new InfoModule(ctx));
        wanted.add(new RtpModule(ctx, teleports));
        wanted.add(new EnderChestModule(ctx));
        wanted.add(new FreezeModule(ctx));
        wanted.add(new StaffModule(ctx, teleports));
        wanted.add(new VanishModule(ctx));
        for (Module module : wanted) {
            if (!ctx.moduleEnabled(module.id())) {
                continue;
            }
            try {
                module.enable();
                active.add(module);
            } catch (Throwable t) {
                getLogger().log(Level.SEVERE, "Le module « " + module.id() + " » n'a pas pu démarrer et reste désactivé.", t);
                try {
                    module.disable();
                } catch (Throwable ignored) {
                    // déjà signalé ci-dessus
                }
            }
        }
    }

    private void disableModules() {
        for (Module module : active) {
            try {
                module.disable();
            } catch (Throwable t) {
                getLogger().log(Level.SEVERE, "Erreur à l'arrêt du module « " + module.id() + " »", t);
            }
        }
        active.clear();
    }
}
