package fr.minebed.hub.ultimate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Les fichiers fournis dans le jar doivent rester cohérents entre eux et avec le code. */
class ResourcesTest {

    private static final Pattern TOKEN = Pattern.compile("\\{[a-z_]+}");

    private static YamlConfiguration load(String path) throws Exception {
        InputStream in = ResourcesTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(in, "ressource absente : " + path);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        return yaml;
    }

    private static Set<String> leaves(YamlConfiguration yaml) {
        Set<String> out = new TreeSet<String>();
        for (String key : yaml.getKeys(true)) {
            if (!yaml.isConfigurationSection(key)) {
                out.add(key);
            }
        }
        return out;
    }

    private static Set<String> tokens(String text) {
        Set<String> out = new HashSet<String>();
        Matcher m = TOKEN.matcher(text);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }

    @Test
    void everyMessageKeyIsInEveryLanguageAndNothingElse() throws Exception {
        Set<String> expected = new TreeSet<String>();
        for (Msg m : Msg.values()) {
            assertTrue(expected.add(m.key()), "clé en double : " + m.key());
        }
        assertEquals(expected, leaves(load("lang/fr.yml")), "fr.yml");
        assertEquals(expected, leaves(load("lang/en.yml")), "en.yml");
    }

    @Test
    void translationsUseTheSameTokensAsTheFrench() throws Exception {
        YamlConfiguration fr = load("lang/fr.yml");
        YamlConfiguration en = load("lang/en.yml");
        for (Msg m : Msg.values()) {
            assertEquals(tokens(fr.getString(m.key())), tokens(en.getString(m.key())), "jetons différents pour " + m.key());
        }
    }

    @Test
    void everyModuleHasAToggleInTheConfig() throws Exception {
        YamlConfiguration config = load("config.yml");
        List<String> ids = Arrays.asList("spawn", "homes", "warps", "tpa", "back", "kits", "economy", "chat", "utility", "join", "announcer", "info", "rtp", "enderchest", "freeze", "staff", "vanish");
        for (String id : ids) {
            assertTrue(config.isBoolean("modules." + id), "modules." + id + " manquant");
        }
        assertEquals(new TreeSet<String>(ids), new TreeSet<String>(config.getConfigurationSection("modules").getKeys(false)));
    }

    @Test
    void pluginYmlDeclaresFoliaSupportAndAllCommandsHaveUsage() throws Exception {
        YamlConfiguration plugin = load("plugin.yml");
        assertTrue(plugin.getBoolean("folia-supported"), "folia-supported doit être vrai");
        assertEquals("EGC-plugins", plugin.getString("name"));
        assertEquals("fr.minebed.hub.ultimate.UltimatePlugin", plugin.getString("main"));
        for (String name : plugin.getConfigurationSection("commands").getKeys(false)) {
            assertNotNull(plugin.getString("commands." + name + ".usage"), "usage manquant : " + name);
            assertNotNull(plugin.getString("commands." + name + ".description"), "description manquante : " + name);
        }
    }

    @Test
    void mainClassExists() throws Exception {
        assertNotNull(Class.forName("fr.minebed.hub.ultimate.UltimatePlugin"));
    }
}
