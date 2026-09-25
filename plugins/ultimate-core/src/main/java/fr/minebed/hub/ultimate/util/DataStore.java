package fr.minebed.hub.ultimate.util;

import fr.minebed.hub.compat.scheduler.Scheduler;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Un fichier de données YAML. Les écritures sont regroupées (au plus une toutes les deux secondes), faites hors du fil
 * principal, et atomiques : on écrit un fichier temporaire puis on le déplace, pour ne jamais laisser un fichier à moitié écrit.
 * Un fichier illisible n'est jamais écrasé en silence : il est mis de côté sous un autre nom.
 */
public final class DataStore {

    private final Plugin plugin;
    private final Scheduler scheduler;
    private final Path file;
    private final YamlConfiguration yaml = new YamlConfiguration();
    private final Object writeLock = new Object();
    private boolean dirty;
    private boolean scheduled;

    public DataStore(Plugin plugin, Scheduler scheduler, String fileName) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.file = plugin.getDataFolder().toPath().resolve(fileName);
        load();
    }

    private void load() {
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                return;
            }
            try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
                yaml.load(reader);
            }
        } catch (IOException | InvalidConfigurationException e) {
            Path aside = file.resolveSibling(file.getFileName() + ".illisible-" + System.currentTimeMillis());
            try {
                Files.move(file, aside);
                plugin.getLogger().log(Level.SEVERE, "Fichier de données illisible, mis de côté sous " + aside.getFileName() + " : on repart d'un fichier vide.", e);
            } catch (IOException moveError) {
                plugin.getLogger().log(Level.SEVERE, "Fichier de données illisible et impossible à mettre de côté : " + file.getFileName(), moveError);
            }
        }
    }

    public synchronized Object get(String path) {
        return yaml.get(path);
    }

    public synchronized String getString(String path) {
        return yaml.getString(path);
    }

    public synchronized long getLong(String path, long fallback) {
        return yaml.getLong(path, fallback);
    }

    public synchronized boolean contains(String path) {
        return yaml.contains(path);
    }

    /** Les clés directement sous {@code section} (jamais {@code null}). */
    public synchronized Set<String> keys(String section) {
        if (section.isEmpty()) {
            return yaml.getKeys(false);
        }
        org.bukkit.configuration.ConfigurationSection s = yaml.getConfigurationSection(section);
        return s == null ? Collections.<String>emptySet() : new java.util.LinkedHashSet<String>(s.getKeys(false));
    }

    /** Écrit une valeur ({@code null} l'efface) et programme la sauvegarde. */
    public void set(String path, Object value) {
        synchronized (this) {
            yaml.set(path, value);
        }
        markDirty();
    }

    private void markDirty() {
        boolean schedule;
        synchronized (this) {
            dirty = true;
            schedule = !scheduled;
            scheduled = true;
        }
        if (schedule) {
            try {
                scheduler.runGlobalLater(new Runnable() {
                    @Override
                    public void run() {
                        writeLater();
                    }
                }, 40);
            } catch (RuntimeException e) {
                // Plugin en cours d'arrêt : le flush() de la désactivation écrira.
                synchronized (this) {
                    scheduled = false;
                }
            }
        }
    }

    private void writeLater() {
        final String text;
        synchronized (this) {
            scheduled = false;
            if (!dirty) {
                return;
            }
            dirty = false;
            text = yaml.saveToString();
        }
        scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                write(text);
            }
        });
    }

    /** Écrit tout de suite (à l'arrêt du plugin). */
    public void flush() {
        String text;
        synchronized (this) {
            dirty = false;
            text = yaml.saveToString();
        }
        write(text);
    }

    private void write(String text) {
        synchronized (writeLock) {
            try {
                Path temp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.write(temp, text.getBytes(StandardCharsets.UTF_8));
                try {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Impossible d'écrire " + file.getFileName(), e);
            }
        }
    }
}
