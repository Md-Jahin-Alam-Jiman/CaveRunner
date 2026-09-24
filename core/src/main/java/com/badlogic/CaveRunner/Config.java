package com.badlogic.CaveRunner;


public final class Config {

    private Config() {
        // Prevent object creation.
    }

    // ---- General ----

    public static final String TITLE = "Cave Runner";

    // ---- World ----

    public static final int TILE = 16;

    public static final int WORLD_W = 300;

    public static final int WORLD_H = 120;

    public static final int SURFACE_BASE = 70;

    public static final int SPAWN_X = WORLD_W / 2;

    public static final int SPAWN_SAFE_RADIUS = 8;

    // ---- Player ----

    public static final float PLAYER_W = TILE * 0.75f;

    public static final float PLAYER_H = TILE * 1.75f;

    public static final float MOVE_SPEED = 120f;

    public static final float JUMP_VEL = 240f;

    public static final float GRAVITY = -600f;

    public static final float MAX_FALL = -400f;

    public static final int MAX_HEALTH = 100;

    public static final int START_LIVES = 3;

    // ---- Time ----

    public static final float DAY_LENGTH = 400f;

    public static final float AUTOSAVE_INTERVAL = 60f;

    // ---- Levels ----

    public static final int MAX_LEVEL = 5;
}