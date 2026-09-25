package com.badlogic.CaveRunner;

// =============================================================
// World
// =============================================================
// Responsible for:
// - Storing the tile grid
// - Generating terrain
// - Generating caves
// - Generating ores
// - Protecting the spawn area
// - Providing world queries
//
// World does NOT handle:
// - Rendering
// - Player movement
// - Physics
// - Camera
// - Input
// =============================================================

public class World {

    // =========================================================
    // World generation settings
    // =========================================================

    private static final long WORLD_SEED = 20260924L;

    // Terrain
    private static final int TERRAIN_MIN_HEIGHT = 8;
    private static final int TERRAIN_MAX_HEIGHT = Config.WORLD_H - 10;

    private static final int DIRT_DEPTH = 5;

    // Terrain noise
    private static final float TERRAIN_SCALE_1 = 0.025f;
    private static final float TERRAIN_SCALE_2 = 0.06f;
    private static final float TERRAIN_SCALE_3 = 0.12f;

    // Caves
    private static final float CAVE_SCALE = 0.055f;
    private static final float CAVE_THRESHOLD = 0.67f;

    // Ore generation
    private static final float COAL_SCALE = 0.12f;
    private static final float IRON_SCALE = 0.10f;
    private static final float GOLD_SCALE = 0.08f;

    // =========================================================
    // World storage
    // =========================================================

    private final byte[][] tiles;

    private final long seed;

    // =========================================================
    // Constructor
    // =========================================================

    /**
     * Creates a world using the default development seed.
     */
    public World() {
        this(WORLD_SEED);
    }

    /**
     * Creates a world using a specific seed.
     *
     * Same seed -> same world.
     * Different seed -> different world.
     */
    public World(long seed) {

        this.seed = seed;

        // Create 300 x 120 world grid.
        //
        // tiles[x][y]
        //
        // x = horizontal position
        // y = vertical position
        tiles = new byte[Config.WORLD_W][Config.WORLD_H];

        // -----------------------------------------------------
        // Module 2 generation pipeline
        // -----------------------------------------------------

        generateTerrain();
        generateCaves();
        generateOres();
        protectSpawn();
    }

    // =========================================================
    // Terrain generation
    // =========================================================

    /**
     * Generates the terrain surface and underground layers.
     *
     * Above surface  -> AIR
     * Surface        -> GRASS
     * Below surface  -> DIRT
     * Deep underground -> STONE
     */
    private void generateTerrain() {

        for (int x = 0; x < Config.WORLD_W; x++) {

            int surfaceHeight = calculateSurfaceHeight(x);

            for (int y = 0; y < Config.WORLD_H; y++) {

                // -------------------------------------------------
                // Above the surface
                // -------------------------------------------------
                if (y > surfaceHeight) {
                    tiles[x][y] = Tiles.AIR;
                    continue;
                }

                // -------------------------------------------------
                // Surface layer
                // -------------------------------------------------
                if (y == surfaceHeight) {
                    tiles[x][y] = Tiles.GRASS;
                    continue;
                }

                // -------------------------------------------------
                // Dirt layer
                // -------------------------------------------------
                if (y >= surfaceHeight - DIRT_DEPTH) {
                    tiles[x][y] = Tiles.DIRT;
                    continue;
                }

                // -------------------------------------------------
                // Deep underground
                // -------------------------------------------------
                tiles[x][y] = Tiles.STONE;
            }
        }
    }

    /**
     * Calculates the terrain height at a specific X coordinate.
     *
     * Several noise frequencies are combined to create
     * smoother terrain instead of completely random hills.
     */
    private int calculateSurfaceHeight(int x) {

        // Large terrain features
        float large = valueNoise1D(
                x * TERRAIN_SCALE_1,
                seed
        );

        // Medium terrain features
        float medium = valueNoise1D(
                x * TERRAIN_SCALE_2,
                seed + 1000L
        );

        // Small terrain details
        float small = valueNoise1D(
                x * TERRAIN_SCALE_3,
                seed + 2000L
        );

        // Combine the three frequencies.
        float combined =
                large * 0.60f
                + medium * 0.30f
                + small * 0.10f;

        // Convert noise value into terrain height.
        int height = Config.SURFACE_BASE
                + Math.round((combined - 0.5f) * 24f);

        // Keep terrain inside the world.
        return clamp(
                height,
                TERRAIN_MIN_HEIGHT,
                TERRAIN_MAX_HEIGHT
        );
    }

    // =========================================================
    // Cave generation
    // =========================================================

    /**
     * Carves caves into the underground.
     *
     * Caves are generated only below the surface.
     */
    private void generateCaves() {

        // Leave the outermost X columns untouched.
        for (int x = 1; x < Config.WORLD_W - 1; x++) {

            int surfaceHeight = calculateSurfaceHeight(x);

            /*
             * Start a few blocks below the surface.
             *
             * Stop before reaching the surface.
             */
            for (int y = 4; y < surfaceHeight - 6; y++) {

                // Do not carve air.
                if (tiles[x][y] == Tiles.AIR) {
                    continue;
                }

                // -------------------------------------------------
                // Primary cave noise
                // -------------------------------------------------

                float cave1 = valueNoise2D(
                        x * CAVE_SCALE,
                        y * CAVE_SCALE,
                        seed + 3000L
                );

                // -------------------------------------------------
                // Secondary cave noise
                // -------------------------------------------------

                float cave2 = valueNoise2D(
                        x * CAVE_SCALE * 2.0f,
                        y * CAVE_SCALE * 2.0f,
                        seed + 4000L
                );

                // Combine both noise layers.
                float caveValue =
                        cave1 * 0.75f
                        + cave2 * 0.25f;

                // Carve cave.
                if (caveValue > CAVE_THRESHOLD) {
                    tiles[x][y] = Tiles.AIR;
                }
            }
        }
    }

    // =========================================================
    // Ore generation
    // =========================================================

    /**
     * Generates all ores.
     *
     * Order:
     *
     * Coal
     * Iron
     * Gold
     */
    private void generateOres() {

        generateCoal();
        generateIron();
        generateGold();
    }

    // =========================================================
    // Coal
    // =========================================================

    /**
     * Generates coal in the upper underground.
     *
     * Coal only replaces STONE.
     */
    private void generateCoal() {

        for (int x = 1; x < Config.WORLD_W - 1; x++) {

            for (int y = 8; y < Config.WORLD_H - 5; y++) {

                // Ores can only replace stone.
                if (tiles[x][y] != Tiles.STONE) {
                    continue;
                }

                float noise = valueNoise2D(
                        x * COAL_SCALE,
                        y * COAL_SCALE,
                        seed + 5000L
                );

                // Coal is more common in shallower areas.
                if (y < 65 && noise > 0.70f) {
                    tiles[x][y] = Tiles.COAL;
                }
            }
        }
    }

    // =========================================================
    // Iron
    // =========================================================

    /**
     * Generates iron deeper underground.
     *
     * Iron only replaces STONE.
     */
    private void generateIron() {

        for (int x = 1; x < Config.WORLD_W - 1; x++) {

            for (int y = 20; y < Config.WORLD_H - 5; y++) {

                // Ores can only replace stone.
                if (tiles[x][y] != Tiles.STONE) {
                    continue;
                }

                float noise = valueNoise2D(
                        x * IRON_SCALE,
                        y * IRON_SCALE,
                        seed + 6000L
                );

                // Iron appears deeper than coal.
                if (y < 85 && y >= 25 && noise > 0.73f) {
                    tiles[x][y] = Tiles.IRON;
                }
            }
        }
    }

    // =========================================================
    // Gold
    // =========================================================

    /**
     * Generates gold deeper underground.
     *
     * Gold only replaces STONE.
     */
    private void generateGold() {

        for (int x = 1; x < Config.WORLD_W - 1; x++) {

            for (int y = 15; y < Config.WORLD_H - 5; y++) {

                // Ores can only replace stone.
                if (tiles[x][y] != Tiles.STONE) {
                    continue;
                }

                float noise = valueNoise2D(
                        x * GOLD_SCALE,
                        y * GOLD_SCALE,
                        seed + 7000L
                );

                // Gold is rarer.
                if (y < 65 && y >= 15 && noise > 0.79f) {
                    tiles[x][y] = Tiles.GOLD;
                }
            }
        }
    }

    // =========================================================
    // Spawn protection
    // =========================================================

    /**
     * Creates a safe area around the player spawn.
     *
     * This method runs after caves and ores so that
     * generation cannot leave the player inside a cave
     * or ore block.
     */
    private void protectSpawn() {

        int spawnX = Config.SPAWN_X;

        // Find the surface at the spawn X coordinate.
        int surfaceHeight = calculateSurfaceHeight(spawnX);

        // -----------------------------------------------------
        // Make spawn surface safe
        // -----------------------------------------------------

        for (int x = spawnX - Config.SPAWN_SAFE_RADIUS;
             x <= spawnX + Config.SPAWN_SAFE_RADIUS;
             x++) {

            if (!inBounds(x, surfaceHeight)) {
                continue;
            }

            // Grass on the surface.
            tiles[x][surfaceHeight] = Tiles.GRASS;

            // Dirt below the surface.
            for (int depth = 1;
                 depth <= DIRT_DEPTH;
                 depth++) {

                int y = surfaceHeight - depth;

                if (inBounds(x, y)) {
                    tiles[x][y] = Tiles.DIRT;
                }
            }
        }

        // -----------------------------------------------------
        // Clear space above spawn
        // -----------------------------------------------------

        for (int x = spawnX - 2;
             x <= spawnX + 2;
             x++) {

            for (int y = surfaceHeight + 1;
                 y <= surfaceHeight + 5;
                 y++) {

                if (inBounds(x, y)) {
                    tiles[x][y] = Tiles.AIR;
                }
            }
        }

        // -----------------------------------------------------
        // Ensure ground below spawn is solid
        // -----------------------------------------------------

        for (int x = spawnX - 2;
             x <= spawnX + 2;
             x++) {

            for (int y = surfaceHeight - DIRT_DEPTH;
                 y < surfaceHeight;
                 y++) {

                if (inBounds(x, y)) {
                    tiles[x][y] = Tiles.DIRT;
                }
            }
        }
    }

    // =========================================================
    // World queries
    // =========================================================

    /**
     * Returns the tile at the specified coordinate.
     *
     * Coordinates outside the world are treated as STONE.
     *
     * This makes the outside of the world solid for
     * future collision detection.
     */
    public byte getTile(int x, int y) {

        if (!inBounds(x, y)) {
            return Tiles.STONE;
        }

        return tiles[x][y];
    }

    /**
     * Changes the tile at the specified coordinate.
     *
     * Invalid coordinates are ignored.
     */
    public void setTile(int x, int y, byte tile) {

        if (!inBounds(x, y)) {
            return;
        }

        tiles[x][y] = tile;
    }

    /**
     * Checks whether a tile is solid.
     *
     * Outside the world is also considered solid.
     */
    public boolean isSolid(int x, int y) {

        return Tiles.isSolid(getTile(x, y));
    }

    /**
     * Checks whether a coordinate is inside the world.
     */
    public boolean inBounds(int x, int y) {

        return x >= 0
                && x < Config.WORLD_W
                && y >= 0
                && y < Config.WORLD_H;
    }

    // =========================================================
    // Utility methods
    // =========================================================

    /**
     * Returns the seed used to generate this world.
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Returns the generated surface height at X.
     *
     * This method is public so later systems can use it
     * for player spawning and debugging.
     */
    public int getSurfaceHeight(int x) {

        if (x < 0 || x >= Config.WORLD_W) {
            return Config.SURFACE_BASE;
        }

        return calculateSurfaceHeight(x);
    }

    /**
     * Counts how many tiles of a specific type exist.
     *
     * Useful for testing world generation.
     */
    public int countTiles(byte tileType) {

        int count = 0;

        for (int x = 0; x < Config.WORLD_W; x++) {

            for (int y = 0; y < Config.WORLD_H; y++) {

                if (tiles[x][y] == tileType) {
                    count++;
                }
            }
        }

        return count;
    }

    // =========================================================
    // Deterministic 1D Value Noise
    // =========================================================

    /**
     * Generates deterministic 1D value noise.
     *
     * Result:
     *
     * 0.0 -> 1.0
     */
    private float valueNoise1D(
            float x,
            long noiseSeed) {

        int x0 = (int) Math.floor(x);
        int x1 = x0 + 1;

        float t = x - x0;

        // Smooth interpolation.
        t = smoothStep(t);

        // Random values at the two points.
        float v0 = randomValue1D(
                x0,
                noiseSeed
        );

        float v1 = randomValue1D(
                x1,
                noiseSeed
        );

        // Interpolate between them.
        return lerp(v0, v1, t);
    }

    /**
     * Generates a deterministic pseudo-random value
     * for a 1D integer coordinate.
     */
    private float randomValue1D(
            int x,
            long noiseSeed) {

        long value = x * 374761393L;

        value += noiseSeed * 668265263L;

        value = (value ^ (value >> 13))
                * 1274126177L;

        value ^= value >> 16;

        // Make value positive.
        long positive = value
                & 0x7fffffffffffffffL;

        // Return value between 0 and 1.
        return (positive % 100000L)
                / 100000f;
    }

    // =========================================================
    // Deterministic 2D Value Noise
    // =========================================================

    /**
     * Generates deterministic 2D value noise.
     *
     * Result:
     *
     * 0.0 -> 1.0
     */
    private float valueNoise2D(
            float x,
            float y,
            long noiseSeed) {

        int x0 = (int) Math.floor(x);
        int x1 = x0 + 1;

        int y0 = (int) Math.floor(y);
        int y1 = y0 + 1;

        float tx = x - x0;
        float ty = y - y0;

        // Smooth interpolation.
        tx = smoothStep(tx);
        ty = smoothStep(ty);

        // Four corner values.
        float v00 = randomValue2D(
                x0,
                y0,
                noiseSeed
        );

        float v10 = randomValue2D(
                x1,
                y0,
                noiseSeed
        );

        float v01 = randomValue2D(
                x0,
                y1,
                noiseSeed
        );

        float v11 = randomValue2D(
                x1,
                y1,
                noiseSeed
        );

        // Interpolate horizontally.
        float bottom = lerp(
                v00,
                v10,
                tx
        );

        float top = lerp(
                v01,
                v11,
                tx
        );

        // Interpolate vertically.
        return lerp(
                bottom,
                top,
                ty
        );
    }

    /**
     * Generates a deterministic pseudo-random value
     * for an X/Y coordinate.
     */
    private float randomValue2D(
            int x,
            int y,
            long noiseSeed) {

        long value = x * 374761393L;

        value += y * 668265263L;

        value += noiseSeed
                * 1442695040888963407L;

        value = (value ^ (value >> 13))
                * 1274126177L;

        value ^= value >> 16;

        // Make value positive.
        long positive = value
                & 0x7fffffffffffffffL;

        // Return value between 0 and 1.
        return (positive % 100000L)
                / 100000f;
    }

    // =========================================================
    // Math helpers
    // =========================================================

    /**
     * Linear interpolation.
     *
     * Moves from a to b using t.
     */
    private float lerp(
            float a,
            float b,
            float t) {

        return a + (b - a) * t;
    }

    /**
     * Smooth interpolation curve.
     */
    private float smoothStep(float t) {

        return t * t * (3f - 2f * t);
    }

    /**
     * Keeps an integer inside a specified range.
     */
    private int clamp(
            int value,
            int min,
            int max) {

        return Math.max(
                min,
                Math.min(max, value)
        );
    }
}