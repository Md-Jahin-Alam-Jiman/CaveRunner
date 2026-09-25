package com.badlogic.CaveRunner;

public final class Tiles {

    private Tiles() {
    }

    // -----------------------------
    // Tile IDs
    // -----------------------------

    public static final byte AIR   = 0;
    public static final byte GRASS = 1;
    public static final byte DIRT  = 2;
    public static final byte STONE = 3;
    public static final byte COAL  = 4;
    public static final byte IRON  = 5;
    public static final byte GOLD  = 6;

    // -----------------------------
    // Tile properties
    // -----------------------------

    public static boolean isSolid(byte tile) {

        return tile != AIR;
    }

    public static boolean isOre(byte tile) {

        return tile == COAL
                || tile == IRON
                || tile == GOLD;
    }

    public static String getName(byte tile) {

        return switch (tile) {

            case AIR   -> "Air";
            case GRASS -> "Grass";
            case DIRT  -> "Dirt";
            case STONE -> "Stone";
            case COAL  -> "Coal";
            case IRON  -> "Iron";
            case GOLD  -> "Gold";

            default -> "Unknown";
        };
    }
}