package fr.minebed.hub.common.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ColorCodesTest {

    @Test
    void translatesClassicCodes() {
        assertEquals("§aBonjour §lmonde", ColorCodes.translate("&aBonjour &lmonde", true));
    }

    @Test
    void lowercasesCodeLetters() {
        assertEquals("§cRouge", ColorCodes.translate("&CRouge", true));
    }

    @Test
    void leavesAmpersandsThatAreNotCodes() {
        assertEquals("Tom & Jerry &z", ColorCodes.translate("Tom & Jerry &z", true));
    }

    @Test
    void expandsHexWhenSupported() {
        assertEquals("§x§f§f§8§0§0§0", ColorCodes.translate("&#ff8000", true));
    }

    @Test
    void fallsBackToNearestClassicColorWithoutHex() {
        assertEquals("§c", ColorCodes.translate("&#ff4040", false)); // rouge clair
        assertEquals("§0", ColorCodes.translate("&#050505", false)); // presque noir
        assertEquals("§f", ColorCodes.translate("&#fefefe", false)); // presque blanc
    }

    @Test
    void stripRemovesEveryKindOfCode() {
        assertEquals("Salut toi", ColorCodes.strip("&aSalut §l&#ff0000toi"));
        assertEquals("abc", ColorCodes.strip("§x§f§f§8§0§0§0abc"));
    }

    @Test
    void handlesNullAndEmpty() {
        assertEquals("", ColorCodes.translate(null, true));
        assertEquals("", ColorCodes.translate("", true));
        assertEquals("", ColorCodes.strip(null));
    }
}
