package com.badlogic.CaveRunner;

/**
 * Stores visual colors used by the renderer.
 *
 * Module 3 only uses the theme for rendering.
 * World generation remains inside World.java.
 */
public final class Theme {

    public final String name;

    // RGB values from 0 to 1.
    public final float[] skyDay;
    public final float[] cave;

    // Tile colors.
    public final float[] grass;
    public final float[] dirt;
    public final float[] stone;
    public final float[] coal;
    public final float[] iron;
    public final float[] gold;

    private Theme(
            String name,
            float[] skyDay,
            float[] cave,
            float[] grass,
            float[] dirt,
            float[] stone,
            float[] coal,
            float[] iron,
            float[] gold) {

        this.name = name;
        this.skyDay = skyDay;
        this.cave = cave;

        this.grass = grass;
        this.dirt = dirt;
        this.stone = stone;
        this.coal = coal;
        this.iron = iron;
        this.gold = gold;
    }

    private static float[] rgb(
            float r,
            float g,
            float b) {

        return new float[] { r, g, b };
    }

    /**
     * Default CaveRunner theme.
     */
    public static Theme greenHills() {

        return new Theme(
                "Green Hills",

                // Day sky
                rgb(0.45f, 0.75f, 0.95f),

                // Underground cave
                rgb(0.07f, 0.06f, 0.08f),

                // Grass
                rgb(0.25f, 0.75f, 0.25f),

                // Dirt
                rgb(0.55f, 0.32f, 0.16f),

                // Stone
                rgb(0.38f, 0.40f, 0.43f),

                // Coal
                rgb(0.08f, 0.08f, 0.08f),

                // Iron
                rgb(0.65f, 0.45f, 0.32f),

                // Gold
                rgb(1.00f, 0.78f, 0.10f)
        );
    }

    /**
     * Returns the color associated with a tile.
     *
     * AIR returns null because AIR is handled separately.
     */
    public float[] colorFor(byte tile) {

        switch (tile) {

            case Tiles.GRASS:
                return grass;

            case Tiles.DIRT:
                return dirt;

            case Tiles.STONE:
                return stone;

            case Tiles.COAL:
                return coal;

            case Tiles.IRON:
                return iron;

            case Tiles.GOLD:
                return gold;

            default:
                return null;
        }
    }
}
