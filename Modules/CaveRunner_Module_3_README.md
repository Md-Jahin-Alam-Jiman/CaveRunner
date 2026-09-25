# CaveRunner — Module 3: Camera & World Rendering

## Goal

Module 2 created the `300 × 120` tile world, but the generated `World` is only stored in memory. Running the game after Module 2 can therefore look almost unchanged because the old Module 1 renderer is still being used.

Module 3 connects the generated `World` to the screen.

By the end of this module:

- the generated `World` is visible on screen;
- an `OrthographicCamera` follows a temporary focus point;
- WASD / arrow keys move the camera focus;
- `+` / `-` control zoom;
- the camera is clamped to the world bounds;
- only visible tiles are rendered;
- the sky and underground cave space have different colors;
- all seven Module 2 tile types can be displayed;
- no large collection of external texture files is required;
- the code uses the actual `com.badlogic.CaveRunner` package from this project.

> **Important:** Module 3 does not add player physics, gravity, jumping, mining, enemies, inventory, or combat. Those belong to later modules.

---

# 1. Why the screen did not change after Module 2

Module 2 generated the world with:

```text
World.java
    ↓
300 × 120 byte tile grid
    ↓
terrain
caves
ores
spawn protection
```

But Module 1's `Main.java` was still drawing the old hardcoded scene.

Module 3 changes the flow to:

```text
World.java
    ↓
getTile(x, y)
    ↓
Camera calculates visible area
    ↓
Renderer loops only over visible tiles
    ↓
Screen
```

This is the main purpose of this module.

---

# 2. Current Module 2 tile IDs

Module 3 uses the tile IDs already defined in `Tiles.java`.

```text
AIR   = 0
GRASS = 1
DIRT  = 2
STONE = 3
COAL  = 4
IRON  = 5
GOLD  = 6
```

The renderer does not change these IDs.

---

# 3. Important correction from the previous Module 3 draft

The previous draft was written for a different project structure and contained names such as:

```java
com.badlogic.Terraria
World.TILE_SIZE
World.WIDTH
World.HEIGHT
world.get(...)
world.surfaceY(...)
world.theme
new World(12345L, 1)
```

Those APIs do **not** match the current CaveRunner Module 2 implementation.

The current project uses:

```java
package com.badlogic.CaveRunner;
```

and the current `World` API includes:

```java
World()
World(long seed)

getTile(x, y)
setTile(x, y, tile)
isSolid(x, y)
inBounds(x, y)
getSeed()
getSurfaceHeight(x)
countTiles(tile)
```

Therefore this README intentionally uses the actual CaveRunner Module 2 API instead of copying incompatible code.

---

# 4. Module 3 architecture

The renderer will use four main classes:

```text
Main.java
   │
   ├── World
   │     └── generated tile data
   │
   ├── Assets
   │     └── simple tile textures
   │
   ├── Theme
   │     └── sky / cave / tile colors
   │
   └── OrthographicCamera
         └── visible world area
```

The responsibilities are separated:

### World

Stores and generates the world.

### Assets

Creates/holds the textures used by the renderer.

### Theme

Stores visual colors.

### Main

Handles:

- camera;
- temporary camera movement;
- zoom;
- rendering;
- visible-range culling.

---

# 5. Tile size

Module 2 stores tiles as grid cells. Module 3 needs a screen/world-space size for each cell.

Use:

```java
public static final int TILE_SIZE = 32;
```

Add this to `Config.java` if it is not already there.

Example:

```java
package com.badlogic.CaveRunner;

public final class Config {

    public static final int WORLD_W = 300;
    public static final int WORLD_H = 120;

    public static final int TILE_SIZE = 32;

    public static final int SURFACE_BASE = 55;

    public static final int SPAWN_X = WORLD_W / 2;

    public static final float MOVE_SPEED = 120f;

    private Config() {
    }
}
```

If your existing `Config.java` already contains these values, **do not duplicate them**. Only add the missing `TILE_SIZE`.

---

# 6. `Theme.java`

Create or replace:

```text
core/src/main/java/com/badlogic/CaveRunner/Theme.java
```

with:

```java
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
```

---

# 7. `Assets.java`

For Module 3 we do not need to create seven separate PNG files just to test the world renderer.

Instead, `Assets` creates a tiny white texture using `Pixmap`.

The renderer then changes its color with `SpriteBatch.setColor()`.

This keeps Module 3 focused on:

- camera;
- world rendering;
- culling.

Create or replace:

```text
core/src/main/java/com/badlogic/CaveRunner/Assets.java
```

with:

```java
package com.badlogic.CaveRunner;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Module 3 rendering assets.
 *
 * A single white texture is enough because SpriteBatch
 * can tint it into different tile colors.
 */
public class Assets implements Disposable {

    private final Texture pixelTexture;
    private final TextureRegion pixel;

    public Assets() {

        Pixmap pixmap = new Pixmap(
                1,
                1,
                Pixmap.Format.RGBA8888
        );

        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();

        pixelTexture = new Texture(pixmap);

        pixmap.dispose();

        pixel = new TextureRegion(pixelTexture);
    }

    /**
     * Returns the 1x1 white texture region.
     */
    public TextureRegion pixel() {
        return pixel;
    }

    @Override
    public void dispose() {
        pixelTexture.dispose();
    }
}
```

---

# 8. `Main.java`

This is the most important part of Module 3.

It replaces the Module 1 renderer.

Create or replace:

```text
core/src/main/java/com/badlogic/CaveRunner/Main.java
```

with:

```java
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
```

---

# 9. Why this renderer is different from Module 1

Module 1 had a hardcoded dirt strip.

Module 3 does this:

```java
byte tile = world.getTile(x, y);
```

That means the renderer reads the actual generated world.

For example:

```text
World
---------------------------------
AIR AIR AIR AIR AIR AIR
AIR AIR AIR AIR AIR AIR
        GRASS
        DIRT
        DIRT
        STONE
        STONE COAL
        STONE IRON
        STONE GOLD
        AIR   <- cave
---------------------------------
              ↓
           Renderer
              ↓
           Screen
```

---

# 10. Visible-range culling

The world contains:

```text
300 × 120
= 36,000 tiles
```

The camera only sees a small portion.

The renderer first calculates:

```java
x0
x1
y0
y1
```

Then it renders:

```java
for (int x = x0; x <= x1; x++) {
    for (int y = y0; y <= y1; y++) {
        ...
    }
}
```

It does **not** loop through all 36,000 tiles every frame.

This is the main performance technique introduced in Module 3.

---

# 11. Camera controls

## Move

```text
W / ↑
    Move focus upward

S / ↓
    Move focus downward

A / ←
    Move focus left

D / →
    Move focus right
```

## Zoom

```text
=
+
    Zoom in

-
    Zoom out

0
    Reset zoom
```

---

# 12. Why `camera.update()` matters

Whenever the camera changes:

```java
camera.position.x = ...;
camera.position.y = ...;
```

or:

```java
camera.viewportWidth = ...;
camera.viewportHeight = ...;
```

we call:

```java
camera.update();
```

Without this, `camera.combined` still contains the old projection matrix.

The screen can therefore appear not to respond even though the Java variables changed.

---

# 13. Camera clamping

The camera must not show outside the world.

The world dimensions are:

```text
Width:
300 × 32 = 9600 pixels

Height:
120 × 32 = 3840 pixels
```

The camera is therefore constrained between:

```java
halfWidth
```

and:

```java
worldWidth - halfWidth
```

and similarly for Y.

This prevents the camera from showing invalid space outside the generated world.

---

# 14. Temporary focus point

Module 3 does NOT have a real player yet.

Instead:

```java
private float focusX;
private float focusY;
```

acts as a temporary player position.

This is intentional.

Module 4 will replace this with the actual `Player` object.

The camera logic itself should not need to be rewritten.

---

# 15. Run the project

From the project root:

### Windows PowerShell

```powershell
.\gradlew lwjgl3:run
```

or:

```powershell
.\gradlew.bat lwjgl3:run
```

---

# 16. Expected result

You should now see something substantially different from Module 1.

The screen should contain:

```text
                 BLUE SKY
────────────────────────────────────

       GREEN GRASS SURFACE
████████████████████████████████████
████████████████████████████████████
████████████████████████████████████
▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
▓▓▓▓▓▓▓▓▓▓ CAVE ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
▓▓▓▓▓▓▓▓▓▓      ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
▓▓▓▓▓▓▓▓ COAL  ▓▓▓▓▓▓▓▓ IRON ▓▓▓▓▓
▓▓▓▓▓▓▓▓▓▓ GOLD ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
```

The actual appearance will be colored rectangles in this module. Real art assets can be introduced later.

---

# 17. Module 3 verification checklist

Before moving to Module 4, verify all of these:

- [ ] `Config.TILE_SIZE` exists
- [ ] `Theme.java` compiles
- [ ] `Assets.java` compiles
- [ ] `Main.java` compiles
- [ ] `World.java` is the fixed Module 2 version
- [ ] package is `com.badlogic.CaveRunner`
- [ ] game starts without exceptions
- [ ] terrain is visible
- [ ] grass is visible
- [ ] dirt is visible
- [ ] stone is visible
- [ ] coal is visible when generated
- [ ] iron is visible when generated
- [ ] gold is visible when generated
- [ ] underground caves appear darker
- [ ] WASD moves the camera focus
- [ ] arrow keys move the camera focus
- [ ] `=` zooms in
- [ ] `-` zooms out
- [ ] `0` resets zoom
- [ ] camera stays inside world boundaries
- [ ] window resizing does not break the camera
- [ ] renderer uses `world.getTile()`
- [ ] renderer does not draw all 36,000 cells every frame

---

# 18. Important testing note about ores and caves

Module 2 generation is deterministic because the world uses a fixed seed.

Therefore a particular seed can produce fewer visible caves or ores than another seed.

Do not change `World.java` just because one run appears to have fewer ore blocks.

First verify:

```text
world.countTiles(Tiles.COAL)
world.countTiles(Tiles.IRON)
world.countTiles(Tiles.GOLD)
```

If a count is zero, that is a generation issue to investigate separately from the renderer.

---

# 19. What Module 3 does NOT implement

Do not add these yet:

```text
❌ Gravity
❌ Jumping
❌ Player collision
❌ Mining
❌ Breaking blocks
❌ Placing blocks
❌ Inventory
❌ Enemies
❌ Combat
❌ Crafting
❌ Save/load
❌ Day/night cycle
❌ Lighting system
❌ Real player animation
```

These belong to later modules.

---

# 20. Module 3 success condition

Module 3 is complete when the generated Module 2 world is actually visible and navigable through the camera.

The important transition is:

```text
MODULE 1
Hardcoded scene
      ↓
MODULE 2
Generated world data
      ↓
MODULE 3
Generated world rendered on screen
      ↓
MODULE 4
Real player + physics
```

The key achievement is that `Main.java` is now consuming the real `World` data instead of drawing a hardcoded dirt strip.

---

# 21. Next module

After Module 3 is verified:

## Module 4 — Player Physics & Movement

Module 4 will replace the temporary camera focus with a real player and introduce:

```text
Player
  ↓
position
velocity
gravity
ground detection
AABB collision
movement
jumping
camera follows player
```

The Module 3 camera and renderer should remain largely unchanged.
