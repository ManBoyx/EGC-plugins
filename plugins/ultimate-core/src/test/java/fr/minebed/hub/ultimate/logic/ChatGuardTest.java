package fr.minebed.hub.ultimate.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ChatGuardTest {

    private final UUID player = UUID.randomUUID();
    private final AtomicLong now = new AtomicLong(0);

    private ChatGuard guard(ChatGuard.Settings settings) {
        return new ChatGuard(settings, now::get);
    }

    private ChatGuard.Settings settings() {
        ChatGuard.Settings s = new ChatGuard.Settings();
        s.forbiddenWords = Arrays.asList("connard");
        s.allowedDomains = Arrays.asList("minebed.fr");
        return s;
    }

    @Test
    void normalMessagesPass() {
        ChatGuard g = guard(settings());
        ChatGuard.Verdict v = g.check(player, "Bonjour tout le monde");
        assertTrue(v.allowed());
        assertEquals("Bonjour tout le monde", v.message());
    }

    @Test
    void forbiddenWordsAreBlockedEvenDisguised() {
        ChatGuard g = guard(settings());
        assertEquals(ChatGuard.Reason.WORD, g.check(player, "espèce de c0nn4rd").reason());
        assertEquals("connard", g.check(player, "CONNARD!").detail());
    }

    @Test
    void spamIsBlockedThenReleased() {
        ChatGuard.Settings s = settings();
        s.maxMessages = 2;
        s.windowMillis = 1_000;
        ChatGuard g = guard(s);
        assertTrue(g.check(player, "premier message").allowed());
        assertTrue(g.check(player, "deuxième truc différent").allowed());
        assertEquals(ChatGuard.Reason.SPAM, g.check(player, "troisième histoire").reason());
        now.set(1_500);
        assertTrue(g.check(player, "quatrième blabla").allowed());
    }

    @Test
    void repeatsAreBlocked() {
        ChatGuard g = guard(settings());
        assertTrue(g.check(player, "achetez mon grade").allowed());
        assertEquals(ChatGuard.Reason.REPEAT, g.check(player, "Achetez mon grade !").reason());
        assertTrue(g.check(player, "autre chose complètement").allowed());
    }

    @Test
    void shoutingIsLowercasedOrBlocked() {
        ChatGuard g = guard(settings());
        ChatGuard.Verdict lowered = g.check(player, "ARRETEZ DE FAIRE CA");
        assertTrue(lowered.allowed());
        assertEquals("arretez de faire ca", lowered.message());

        ChatGuard.Settings s = settings();
        s.capsLowercase = false;
        assertEquals(ChatGuard.Reason.CAPS, guard(s).check(UUID.randomUUID(), "ARRETEZ DE FAIRE CA").reason());
    }

    @Test
    void linksAreBlockedExceptAllowedDomains() {
        ChatGuard g = guard(settings());
        assertEquals(ChatGuard.Reason.LINK, g.check(player, "venez sur https://serveur-pirate.xyz").reason());
        assertEquals("serveur-pirate.xyz", g.check(player, "www.serveur-pirate.xyz c'est bien").detail());
        assertTrue(g.check(player, "notre site : minebed.fr").allowed());
        assertTrue(g.check(player, "et le forum sur forum.minebed.fr").allowed());
        assertTrue(g.check(player, "bonjour. ça va ? j'arrive.").allowed());
    }

    @Test
    void linkFilterCanBeDisabled() {
        ChatGuard.Settings s = settings();
        s.blockLinks = false;
        assertTrue(guard(s).check(player, "va sur exemple.com").allowed());
    }

    @Test
    void emptyMessageIsAllowedAndForgetResets() {
        ChatGuard g = guard(settings());
        assertTrue(g.check(player, "   ").allowed());
        g.check(player, "un message assez long");
        g.forget(player);
        assertTrue(g.check(player, "un message assez long").allowed());
    }
}
