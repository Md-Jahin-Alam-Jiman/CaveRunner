package com.badlogic.CaveRunner;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

/**
 * Module 3:
 *
 * - Creates the world
 * - Creates an orthographic camera
 * - Moves a temporary focus point
 * - Renders only visible tiles
 * - Supports zoom
 *
 * Player physics will replace the temporary focus point
 * in Module 4.
 */
public class Main extends ApplicationAdapter {

    // ---------------------------------------------------------
    // Camera settings
    // ---------------------------------------------------------

    private static final int VIEW_W_TILES = 32;
    private static final int VIEW_H_TILES = 18;

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.0f;

    // ---------------------------------------------------------
    // Game objects
    // ---------------------------------------------------------

    private Assets assets;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private World world;
    private Theme theme;

    // ---------------------------------------------------------
    // Camera focus
    // ---------------------------------------------------------

    private float focusX;
    private float focusY;

    private static final float FOCUS_SPEED = 220f;

    // Zoom
    private float zoom = 1f;

    // ---------------------------------------------------------
    // Temporary sky color
    // ---------------------------------------------------------

    private final float[] skyColor = new float[3];

    // ---------------------------------------------------------
    // Create
    // ---------------------------------------------------------

    @Override
    public void create() {

        // Create rendering resources.
        assets = new Assets();

        batch = new SpriteBatch();

        // Create world using Module 2.
        world = new World();

        // Create theme.
        theme = Theme.greenHills();

        // -----------------------------------------------------
        // Camera
        // -----------------------------------------------------

        camera = new OrthographicCamera();

        float width =
                VIEW_W_TILES * Config.TILE_SIZE;

        float height =
                VIEW_H_TILES * Config.TILE_SIZE;

        camera.setToOrtho(
                false,
                width,
                height
        );

        applyZoom();

        // -----------------------------------------------------
        // Start camera at spawn
        // -----------------------------------------------------

        int spawnX = Config.SPAWN_X;

        int spawnY =
                world.getSurfaceHeight(spawnX);

        focusX =
                spawnX * Config.TILE_SIZE
                + Config.TILE_SIZE / 2f;

        focusY =
                (spawnY + 3)
                * Config.TILE_SIZE;

        camera.position.set(
                focusX,
                focusY,
                0f
        );

        camera.update();
    }

    // ---------------------------------------------------------
    // Resize
    // ---------------------------------------------------------

    @Override
    public void resize(
            int width,
            int height) {

        applyZoom();
    }

    // ---------------------------------------------------------
    // Render
    // ---------------------------------------------------------

    @Override
    public void render() {

        float delta =
                Gdx.graphics.getDeltaTime();

        handleInput(delta);

        updateCamera();

        draw();
    }

    // ---------------------------------------------------------
    // Input
    // ---------------------------------------------------------

    private void handleInput(float delta) {

        // Move camera focus horizontally.
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.A)) {

            focusX -= FOCUS_SPEED * delta;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)
                || Gdx.input.isKeyPressed(Input.Keys.D)) {

            focusX += FOCUS_SPEED * delta;
        }

        // Move camera focus vertically.
        if (Gdx.input.isKeyPressed(Input.Keys.UP)
                || Gdx.input.isKeyPressed(Input.Keys.W)) {

            focusY += FOCUS_SPEED * delta;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)
                || Gdx.input.isKeyPressed(Input.Keys.S)) {

            focusY -= FOCUS_SPEED * delta;
        }

        // Zoom out.
        if (Gdx.input.isKeyJustPressed(
                Input.Keys.MINUS)) {

            zoomBy(0.1f);
        }

        // Zoom in.
        if (Gdx.input.isKeyJustPressed(
                Input.Keys.EQUALS)) {

            zoomBy(-0.1f);
        }

        // Reset zoom.
        if (Gdx.input.isKeyJustPressed(
                Input.Keys.NUM_0)) {

            zoom = 1f;

            applyZoom();
        }
    }

    // ---------------------------------------------------------
    // Zoom
    // ---------------------------------------------------------

    private void zoomBy(float amount) {

        zoom = MathUtils.clamp(
                zoom + amount,
                MIN_ZOOM,
                MAX_ZOOM
        );

        applyZoom();
    }

    /**
     * Recalculates the camera viewport.
     *
     * The height is based on the desired number of tiles.
     * The width is calculated from the window aspect ratio.
     */
    private void applyZoom() {

        if (camera == null) {
            return;
        }

        float screenWidth =
                Math.max(1, Gdx.graphics.getWidth());

        float screenHeight =
                Math.max(1, Gdx.graphics.getHeight());

        float aspect =
                screenWidth / screenHeight;

        camera.viewportHeight =
                VIEW_H_TILES
                * Config.TILE_SIZE
                * zoom;

        camera.viewportWidth =
                camera.viewportHeight
                * aspect;

        camera.update();
    }

    // ---------------------------------------------------------
    // Camera update
    // ---------------------------------------------------------

    private void updateCamera() {

        float halfWidth =
                camera.viewportWidth / 2f;

        float halfHeight =
                camera.viewportHeight / 2f;

        float worldWidth =
                Config.WORLD_W
                * Config.TILE_SIZE;

        float worldHeight =
                Config.WORLD_H
                * Config.TILE_SIZE;

        // Smoothly follow focus.
        camera.position.x +=
                (focusX - camera.position.x)
                * 0.15f;

        camera.position.y +=
                (focusY - camera.position.y)
                * 0.15f;

        // -----------------------------------------------------
        // Clamp camera to world bounds.
        // -----------------------------------------------------

        camera.position.x =
                MathUtils.clamp(
                        camera.position.x,
                        halfWidth,
                        Math.max(
                                halfWidth,
                                worldWidth - halfWidth
                        )
                );

        camera.position.y =
                MathUtils.clamp(
                        camera.position.y,
                        halfHeight,
                        Math.max(
                                halfHeight,
                                worldHeight - halfHeight
                        )
                );

        camera.update();
    }

    // ---------------------------------------------------------
    // Draw
    // ---------------------------------------------------------

    private void draw() {

        // Enable transparency.
        Gdx.gl.glEnable(
                GL20.GL_BLEND
        );

        Gdx.gl.glBlendFunc(
                GL20.GL_SRC_ALPHA,
                GL20.GL_ONE_MINUS_SRC_ALPHA
        );

        // -----------------------------------------------------
        // Sky
        // -----------------------------------------------------

        theme.skyDay[0] = MathUtils.clamp(
                theme.skyDay[0],
                0f,
                1f
        );

        theme.skyDay[1] = MathUtils.clamp(
                theme.skyDay[1],
                0f,
                1f
        );

        theme.skyDay[2] = MathUtils.clamp(
                theme.skyDay[2],
                0f,
                1f
        );

        skyColor[0] = theme.skyDay[0];
        skyColor[1] = theme.skyDay[1];
        skyColor[2] = theme.skyDay[2];

        Gdx.gl.glClearColor(
                skyColor[0],
                skyColor[1],
                skyColor[2],
                1f
        );

        Gdx.gl.glClear(
                GL20.GL_COLOR_BUFFER_BIT
        );

        // Draw world.
        drawWorld();
    }

    // ---------------------------------------------------------
    // World rendering
    // ---------------------------------------------------------

    /**
     * Draws ONLY tiles currently visible through the camera.
     *
     * The complete world contains:
     *
     * 300 × 120 = 36,000 cells.
     *
     * We do NOT draw all 36,000 every frame.
     */
    private void drawWorld() {

        batch.setProjectionMatrix(
                camera.combined
        );

        int tileSize =
                Config.TILE_SIZE;

        // -----------------------------------------------------
        // Calculate visible tile range.
        // -----------------------------------------------------

        int x0 = Math.max(
                0,
                MathUtils.floor(
                        (
                                camera.position.x
                                - camera.viewportWidth / 2f
                        ) / tileSize
                ) - 1
        );

        int x1 = Math.min(
                Config.WORLD_W - 1,
                MathUtils.ceil(
                        (
                                camera.position.x
                                + camera.viewportWidth / 2f
                        ) / tileSize
                ) + 1
        );

        int y0 = Math.max(
                0,
                MathUtils.floor(
                        (
                                camera.position.y
                                - camera.viewportHeight / 2f
                        ) / tileSize
                ) - 1
        );

        int y1 = Math.min(
                Config.WORLD_H - 1,
                MathUtils.ceil(
                        (
                                camera.position.y
                                + camera.viewportHeight / 2f
                        ) / tileSize
                ) + 1
        );

        TextureRegion pixel =
                assets.pixel();

        batch.begin();

        // -----------------------------------------------------
        // Visible-range culling
        // -----------------------------------------------------

        for (int x = x0; x <= x1; x++) {

            int surface =
                    world.getSurfaceHeight(x);

            for (int y = y0; y <= y1; y++) {

                byte tile =
                        world.getTile(x, y);

                // -------------------------------------------------
                // AIR
                // -------------------------------------------------

                if (tile == Tiles.AIR) {

                    /*
                     * Air above the surface shows the sky.
                     *
                     * Air underground gets a dark cave color.
                     */
                    if (y < surface - 1) {

                        float depth =
                                MathUtils.clamp(
                                        (
                                                surface
                                                - 1
                                                - y
                                        ) / 5f,
                                        0f,
                                        1f
                                );

                        float r =
                                theme.cave[0]
                                * depth;

                        float g =
                                theme.cave[1]
                                * depth;

                        float b =
                                theme.cave[2]
                                * depth;

                        batch.setColor(
                                r,
                                g,
                                b,
                                depth
                        );

                        batch.draw(
                                pixel,
                                x * tileSize,
                                y * tileSize,
                                tileSize,
                                tileSize
                        );

                        batch.setColor(
                                1f,
                                1f,
                                1f,
                                1f
                        );
                    }

                    continue;
                }

                // -------------------------------------------------
                // Solid tile
                // -------------------------------------------------

                float[] color =
                        theme.colorFor(tile);

                if (color == null) {
                    continue;
                }

                batch.setColor(
                        color[0],
                        color[1],
                        color[2],
                        1f
                );

                batch.draw(
                        pixel,
                        x * tileSize,
                        y * tileSize,
                        tileSize,
                        tileSize
                );

                // Always reset SpriteBatch color.
                batch.setColor(
                        1f,
                        1f,
                        1f,
                        1f
                );
            }
        }

        batch.end();
    }

    // ---------------------------------------------------------
    // Dispose
    // ---------------------------------------------------------

    @Override
    public void dispose() {

        batch.dispose();

        assets.dispose();
    }
}
