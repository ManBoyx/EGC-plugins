package fr.minebed.hub.ultimate.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class RandomSpotTest {

    @Test
    void pickedPointsStayInsideTheRing() {
        Random random = new Random(42);
        for (int i = 0; i < 5000; i++) {
            int[] p = RandomSpot.pick(random, 100, -50, 200, 1000);
            double d = Math.hypot(p[0] - 100, p[1] + 50);
            assertTrue(d >= 199 && d <= 1001, "distance hors couronne : " + d);
        }
    }

    @Test
    void pointsAreSpreadOverTheSurfaceNotTheRadius() {
        // Sur une surface uniforme, la moitié des points est au-delà du rayon r tel que r² = (min² + max²) / 2.
        Random random = new Random(7);
        double median = Math.sqrt((0 + 1000.0 * 1000.0) / 2);
        int beyond = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            int[] p = RandomSpot.pick(random, 0, 0, 0, 1000);
            if (Math.hypot(p[0], p[1]) > median) {
                beyond++;
            }
        }
        double share = beyond / (double) n;
        assertTrue(share > 0.47 && share < 0.53, "part au-delà de la médiane : " + share);
    }

    @Test
    void allFourQuadrantsAreUsed() {
        Random random = new Random(1);
        boolean[] seen = new boolean[4];
        for (int i = 0; i < 200; i++) {
            int[] p = RandomSpot.pick(random, 0, 0, 100, 500);
            seen[(p[0] >= 0 ? 0 : 1) + (p[1] >= 0 ? 0 : 2)] = true;
        }
        for (boolean s : seen) {
            assertTrue(s);
        }
    }

    @Test
    void nonsenseRadiiAreRepaired() {
        Random random = new Random(3);
        for (int i = 0; i < 200; i++) {
            int[] negative = RandomSpot.pick(random, 0, 0, -50, 100);
            assertTrue(Math.hypot(negative[0], negative[1]) <= 101);
            int[] inverted = RandomSpot.pick(random, 0, 0, 500, 100); // maximum inférieur au minimum
            assertTrue(Math.hypot(inverted[0], inverted[1]) >= 499);
        }
    }

    @Test
    void dangerousGroundIsRefusedByNameOldAndNew() {
        for (String bad : new String[] {"LAVA", "STATIONARY_LAVA", "WATER", "STATIONARY_WATER", "CACTUS", "FIRE", "MAGMA", "MAGMA_BLOCK",
            "OAK_LEAVES", "LEAVES", "LEAVES_2", "COBWEB", "WEB", "SWEET_BERRY_BUSH", "POWDER_SNOW", "CAMPFIRE", "BUBBLE_COLUMN", "KELP", "POINTED_DRIPSTONE"}) {
            assertTrue(RandomSpot.isUnsafeGroundName(bad), bad);
        }
        for (String good : new String[] {"GRASS", "GRASS_BLOCK", "DIRT", "STONE", "SAND", "SNOW_BLOCK", "ICE", "OAK_PLANKS", "WOOD", "NETHERRACK"}) {
            assertFalse(RandomSpot.isUnsafeGroundName(good), good);
        }
    }

    @Test
    void borderTest() {
        assertTrue(RandomSpot.insideBorder(0, 0, 0, 0, 1000, 8));
        assertTrue(RandomSpot.insideBorder(491, -491, 0, 0, 1000, 8));
        assertFalse(RandomSpot.insideBorder(493, 0, 0, 0, 1000, 8));
        assertFalse(RandomSpot.insideBorder(0, 600, 0, 0, 1000, 8));
        assertTrue(RandomSpot.insideBorder(1200, 1200, 1000, 1000, 1000, 8)); // bordure centrée ailleurs
    }
}
