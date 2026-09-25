package com.badlogic.CaveRunner;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Module 1:
 * Proves the game loop, window, asset loading,
 * delta-time movement, and cleanup.
 */
public class Main extends ApplicationAdapter {

    private SpriteBatch batch;

    private OrthographicCamera camera;

    private Assets assets;

    // Simple game state.
    private float playerX = 100f;

    private float elapsed = 0f;

    // 32 tiles horizontally.
    private static final int VIEW_W =
            32 * Config.TILE;

    // 18 tiles vertically.
    private static final int VIEW_H =
            18 * Config.TILE;

    @Override
    public void create() {

        // Create rendering system.
        batch = new SpriteBatch();

        // Create 2D camera.
        camera = new OrthographicCamera();

        // Configure the camera.
        camera.setToOrtho(
                false,
                VIEW_W,
                VIEW_H
        );

        // Load textures.
        assets = new Assets();

        // System.out.println("AIR: " + Tiles.getName(Tiles.AIR));
        // System.out.println("GRASS: " + Tiles.getName(Tiles.GRASS));
        // System.out.println("DIRT: " + Tiles.getName(Tiles.DIRT));
        // System.out.println("STONE: " + Tiles.getName(Tiles.STONE));
        // System.out.println("COAL: " + Tiles.getName(Tiles.COAL));
        // System.out.println("IRON: " + Tiles.getName(Tiles.IRON));
        // System.out.println("GOLD: " + Tiles.getName(Tiles.GOLD));
        
        // System.out.println("Stone solid: " + Tiles.isSolid(Tiles.STONE));
        // System.out.println("Air solid: " + Tiles.isSolid(Tiles.AIR));
        
        // System.out.println("Gold ore: " + Tiles.isOre(Tiles.GOLD));
        // System.out.println("Dirt ore: " + Tiles.isOre(Tiles.DIRT));
    }

    @Override
    public void resize(int width, int height) {

        // Camera/window resizing will be handled
        // properly in a later module.
    }

    @Override
    public void render() {

        // ========================================
        // 1. UPDATE
        // ========================================

        float delta = Gdx.graphics.getDeltaTime();

        elapsed += delta;

        // Move player based on real elapsed time.
        playerX += Config.MOVE_SPEED * delta;

        // Wrap player around when it leaves the screen.
        if (playerX > VIEW_W) {
            playerX = -Config.PLAYER_W;
        }

        // ========================================
        // 2. CLEAR SCREEN
        // ========================================

        Gdx.gl.glClearColor(
                0.53f,
                0.75f,
                0.92f,
                1f
        );

        Gdx.gl.glClear(
                GL20.GL_COLOR_BUFFER_BIT
        );

        // ========================================
        // 3. DRAW
        // ========================================

        camera.update();

        batch.setProjectionMatrix(
                camera.combined
        );

        batch.begin();

        // Ground.
        batch.draw(
                assets.get("dirt"),
                0,
                0,
                VIEW_W,
                Config.TILE
        );

        // Player.
        batch.draw(
                assets.get("player"),
                playerX,
                Config.TILE,
                Config.PLAYER_W,
                Config.PLAYER_H
        );

        batch.end();
    }

    @Override
    public void dispose() {

        // Release GPU resources.
        batch.dispose();

        assets.dispose();
    }
}