package fr.minebed.hub.ultimate.logic;

import java.util.Locale;
import java.util.Random;

/** Le calcul du « téléport aléatoire » qui n'a pas besoin de Minecraft : où tirer un point, et quels sols refuser. */
public final class RandomSpot {

    private RandomSpot() {
    }

    /**
     * Tire un point {x, z} dans une couronne autour de ({@code centerX}, {@code centerZ}), uniformément sur la surface
     * (et non sur le rayon, qui favoriserait le centre). Un rayon négatif vaut 0 ; un maximum trop petit est relevé.
     */
    public static int[] pick(Random random, int centerX, int centerZ, int minRadius, int maxRadius) {
        double min = Math.max(0, minRadius);
        double max = Math.max(min + 1, maxRadius);
        double radius = Math.sqrt(random.nextDouble() * (max * max - min * min) + min * min);
        double angle = random.nextDouble() * 2 * Math.PI;
        return new int[] {centerX + (int) Math.round(radius * Math.cos(angle)), centerZ + (int) Math.round(radius * Math.sin(angle))};
    }

    /**
     * Vrai si on ne doit pas poser un joueur sur ce matériau (par son nom, valable avant comme après la 1.13) :
     * liquides, feu, cactus, feuilles (cime d'arbre), toiles, baies, magma, stalactites, neige poudreuse…
     */
    public static boolean isUnsafeGroundName(String materialName) {
        String n = materialName.toUpperCase(Locale.ROOT);
        String[] bad = {"LAVA", "WATER", "CACTUS", "FIRE", "MAGMA", "CAMPFIRE", "POWDER_SNOW", "BERRY", "BERRIES", "WITHER_ROSE",
            "LEAVES", "WEB", "BUBBLE", "KELP", "SEAGRASS", "DRIPSTONE", "SCAFFOLDING", "HONEY", "SLIME", "TNT"};
        for (String part : bad) {
            if (n.contains(part)) {
                return true;
            }
        }
        return false;
    }

    /** Vrai si ({@code x}, {@code z}) est dans la bordure carrée du monde, avec une marge. */
    public static boolean insideBorder(double x, double z, double borderCenterX, double borderCenterZ, double borderSize, double margin) {
        double half = borderSize / 2.0 - margin;
        return Math.abs(x - borderCenterX) <= half && Math.abs(z - borderCenterZ) <= half;
    }
}
