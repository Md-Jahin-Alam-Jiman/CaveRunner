# Module 2 — World Generation & Tiles

## Goal

Module 1 proved that the libGDX application, assets, desktop launcher, and delta-time loop work.

Module 2 replaces the hardcoded dirt strip with an actual **tile-based world model**.

By the end of this module, CaveRunner should have:

- a clear tile-ID system,
- tile properties such as solid/air,
- a `300 × 120` world grid,
- deterministic terrain generation,
- a grass/soil/stone underground structure,
- caves carved from the underground,
- several ore veins,
- a safe spawn area,
- world-query methods that later physics and rendering can use.

**Important:** Module 2 is primarily a **world-data and generation module**. Camera-following and efficient visible-tile rendering belong to Module 3. Player gravity/collision belongs to Module 4.

---

# 1. Module 2 architecture

The core responsibility is divided into two classes:

```text
core/src/main/java/com/badlogic/CaveRunner/
│
├── Main.java
├── Config.java
├── Assets.java
│
├── Tiles.java       ← What each tile means
└── World.java       ← Where each tile exists
```

Think of the relationship as:

```text
              Tiles
                │
       defines tile meaning
                │
                ▼
        ┌───────────────┐
        │     World     │
        │               │
        │ 300 × 120 grid│
        │               │
        └───────────────┘
                │
        terrain generation
        caves + ores
                │
                ▼
       Module 3 rendering
                │
                ▼
        Module 4 collision
```

### First-principles rule

Do not make `World` responsible for drawing.

Do not make `Tiles` responsible for generating the world.

Keep the responsibilities separate:

```text
Tiles.java
    = tile definitions

World.java
    = tile storage + generation + world queries
```

This separation will make later collision, mining, rendering, saving, and editing much easier.

---

# 2. Coordinate system

We use integer tile coordinates:

```text
(x, y)
```

where:

```text
x = 0 ... WORLD_W - 1
y = 0 ... WORLD_H - 1
```

For this project:

```text
WORLD_W = 300
WORLD_H = 120
TILE    = 16 pixels
```

Therefore the world represents:

```text
300 × 120 tiles
```

or:

```text
4,800 × 1,920 pixels
```

### Coordinate convention

For this module:

```text
y = 0
│
│
│        underground
│
│
└────────────────────── x
```

So higher `y` values represent higher terrain.

This is convenient because libGDX's 2D coordinate system also uses positive Y upward.

---

# 3. Tile IDs

Do not use strings such as:

```java
"grass"
"stone"
"iron"
```

for every cell in the world.

The world grid should store compact integer IDs:

```text
AIR
GRASS
DIRT
STONE
COAL
IRON
GOLD
```

The basic idea is:

```java
world[x][y] = Tiles.STONE;
```

instead of:

```java
world[x][y] = "stone";
```

Integer IDs make the grid simple and efficient.

---

# 4. `Tiles.java`

Create:

```text
core/src/main/java/com/badlogic/CaveRunner/Tiles.java
```

Use this structure:

```java
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
```

## Why `byte`?

The current tile set has only a few IDs.

A `byte` is enough to store values from:

```text
-128 to 127
```

So it is more than enough for our tile IDs.

Later, if the game needs hundreds of tile types, we can reconsider the storage type.

---

# 5. Tile properties

At this stage we only need a few properties.

| Tile | ID | Solid | Ore |
|---|---:|---|---|
| Air | 0 | No | No |
| Grass | 1 | Yes | No |
| Dirt | 2 | Yes | No |
| Stone | 3 | Yes | No |
| Coal | 4 | Yes | Yes |
| Iron | 5 | Yes | Yes |
| Gold | 6 | Yes | Yes |

Later modules can expand this system with:

- hardness,
- mining time,
- light emission,
- damage,
- friction,
- tool requirement,
- drop item,
- texture region.

**Do not add those properties yet.**

Module 2 should establish the simplest useful tile model.

---

# 6. World storage

Create:

```text
core/src/main/java/com/badlogic/CaveRunner/World.java
```

The basic storage is:

```java
private final byte[][] tiles;
```

with:

```java
tiles[x][y]
```

representing the tile at coordinate `(x, y)`.

The constructor should allocate:

```java
tiles = new byte[Config.WORLD_W][Config.WORLD_H];
```

For this project:

```text
tiles.length       = 300
tiles[x].length    = 120
```

---

# 7. Deterministic generation

World generation should be deterministic.

That means:

```text
same seed
     ↓
same terrain
     ↓
same caves
     ↓
same ore distribution
```

Use a fixed seed during development.

For example:

```java
private static final long WORLD_SEED = 20260924L;
```

Later, the seed can be generated randomly and stored with the save file.

### Why deterministic generation matters

If something goes wrong:

```text
World seed = 20260924
```

lets us reproduce the exact same world.

That is extremely useful when debugging:

- bad cave generation,
- broken spawn areas,
- missing ores,
- collision bugs,
- world corruption.

---

# 8. Terrain generation model

The first generation pass creates the surface.

We want something conceptually like:

```text
                 surface
       ~~~~~~~ grass ~~~~~~~
       D D D D D D D D D D
       D D D D D D D D D D
       S S S S S S S S S S
       S S S S S S S S S S
       S S S S S S S S S S
```

The surface should not be perfectly flat.

Instead, calculate a surface height for each X coordinate:

```text
surface[x]
```

For example:

```text
       ___
   ___/   \__
__/          \___
```

The terrain should remain playable rather than becoming extreme cliffs.

---

# 9. Value-noise approach

We do not need an external noise library for Module 2.

Implement a small deterministic value-noise helper inside `World.java`.

The idea is:

1. Generate deterministic pseudo-random values at integer points.
2. Interpolate between them.
3. Sample at a lower frequency for broad terrain.
4. Combine multiple frequencies for detail.

Conceptually:

```text
large noise
     +
medium noise
     +
small noise
     =
terrain height
```

This is often called **layered noise** or **fractal/value noise**.

### Why not pure random?

This:

```java
random.nextInt()
```

for every X coordinate creates jagged terrain.

Noise creates smoothly changing values:

```text
      __
  ___/  \____
_/            \__
```

instead of:

```text
_/\/\__/\/\_/\/\_
```

---

# 10. Surface generation

A practical starting formula is:

```text
surfaceY =
    SURFACE_BASE
    + broadNoise * 12
    + detailNoise * 4
```

The exact constants are tunable.

The important design goal is:

```text
broad noise  → large hills
detail noise → small variation
```

Do not try to make the terrain look perfect in this first pass.

The first goal is a **stable and believable playable terrain**.

---

# 11. Filling the terrain

For each column:

```text
if y > surface:
    AIR

if y == surface:
    GRASS

if surface - 4 <= y < surface:
    DIRT

if y < surface - 4:
    STONE
```

Conceptually:

```text
AIR AIR AIR AIR AIR
       GRASS
       DIRT
       DIRT
       DIRT
       STONE
       STONE
       STONE
```

This gives us a basic surface structure before caves and ores are applied.

---

# 12. Cave generation

Caves are generated **after** the basic terrain.

Do not generate caves before the terrain exists.

The sequence should be:

```text
1. Fill terrain
       ↓
2. Carve caves
       ↓
3. Add ores
       ↓
4. Protect spawn
```

This ordering is important.

If ores are placed before caves, the cave pass may accidentally remove them.

---

# 13. Cave noise

Use another deterministic noise field for caves.

Conceptually:

```text
if underground
and caveNoise > threshold:
    tile = AIR
```

But do not carve the entire underground.

The cave condition should only apply below the surface:

```text
y < surface[x] - minimumDepth
```

This protects the surface layer.

A useful conceptual result is:

```text
     GRASS
  D D D D D D
  S S   S S S
  S   C     S
  S C     C S
  S   C C   S
  S S S S S S
```

where `C` represents empty cave space.

---

# 14. Avoiding ugly caves

A single threshold can produce:

- isolated one-tile holes,
- huge empty regions,
- disconnected noise specks.

Use a combination of:

```text
cave noise
+
depth restriction
+
threshold
```

Then inspect the generated world visually.

Do not tune cave generation only by looking at numbers.

---

# 15. Ore generation

Ores should be generated **inside solid underground material**.

Basic rules:

### Coal

Common and relatively shallow.

```text
STONE → COAL
```

### Iron

Less common and deeper.

```text
STONE → IRON
```

### Gold

Rare and deeper.

```text
STONE → GOLD
```

A simple first-pass strategy is:

```text
if tile == STONE
and depth is appropriate
and ore noise passes threshold:
    replace STONE with ore
```

---

# 16. Why use ore noise instead of only random chance?

Pure random placement:

```java
if (random.nextFloat() < 0.02f)
```

creates isolated single blocks.

We want veins:

```text
    O
   OOO
    OO
```

Noise creates spatial correlation.

That means nearby cells tend to receive similar values, which naturally produces clusters.

---

# 17. Ore depth rules

A simple initial distribution:

```text
Coal:
    medium depth

Iron:
    deeper than coal

Gold:
    deepest
```

For example:

```text
surface
   │
   ├── dirt
   │
   ├── coal
   │
   ├── iron
   │
   └── gold
```

The exact depth ranges should be constants rather than magic numbers.

For example:

```java
private static final int COAL_MIN_DEPTH = 8;
private static final int IRON_MIN_DEPTH = 20;
private static final int GOLD_MIN_DEPTH = 40;
```

These values are starting points, not final balance.

---

# 18. Spawn protection

The spawn area must be safe.

The center X position is:

```java
Config.SPAWN_X
```

Do not place caves or ores in a small protected radius around spawn.

For example:

```text
             spawn
               ↓
        ┌─────────────┐
        │   SAFE AREA │
────────┴─────────────┴────────
```

At minimum, protect the surface and immediate underground region around:

```java
Config.SPAWN_X
```

This prevents the player from spawning inside:

- a cave,
- an unexpected hole,
- an ore pocket,
- later traps/enemies.

---

# 19. World query methods

`World.java` should provide methods instead of exposing the raw array everywhere.

At minimum:

```java
public byte getTile(int x, int y)
```

```java
public void setTile(int x, int y, byte tile)
```

```java
public boolean isSolid(int x, int y)
```

```java
public boolean inBounds(int x, int y)
```

This gives later systems a clean interface.

For example, Module 4 collision code can simply ask:

```java
world.isSolid(tileX, tileY)
```

instead of directly manipulating:

```java
world.tiles[x][y]
```

---

# 20. Boundary behavior

A world query must not crash when given an invalid coordinate.

For example:

```text
x = -1
y = 50
```

or:

```text
x = 300
y = 50
```

should not cause:

```text
ArrayIndexOutOfBoundsException
```

Use:

```java
public boolean inBounds(int x, int y)
```

before accessing the array.

For `getTile`, choose a consistent out-of-bounds policy.

For this project, treating outside the world as solid is useful for collision:

```text
outside world = solid
```

But keep the policy explicit in the code.

---

# 21. Recommended `World.java` responsibilities

`World.java` should contain:

```text
World
├── tile array
├── seed
├── constructor
├── generate()
├── generateTerrain()
├── carveCaves()
├── generateOres()
├── protectSpawn()
├── getTile()
├── setTile()
├── isSolid()
└── inBounds()
```

It should **not** contain:

```text
SpriteBatch
Texture
OrthographicCamera
player movement
keyboard input
combat
inventory
```

Those belong to other systems/modules.

---

# 22. Generation pipeline

The complete Module 2 pipeline should be:

```text
World constructor
       │
       ▼
allocate 300 × 120 grid
       │
       ▼
generate surface heights
       │
       ▼
fill grass / dirt / stone
       │
       ▼
carve caves
       │
       ▼
generate coal
       │
       ▼
generate iron
       │
       ▼
generate gold
       │
       ▼
protect spawn
       │
       ▼
world ready
```

This ordering should remain explicit in the code.

---

# 23. Module 2 test strategy

Do not immediately integrate everything into the full game.

First test the `World` independently.

Useful checks:

```java
System.out.println(
    world.getTile(Config.SPAWN_X, Config.SURFACE_BASE)
);
```

Check that:

- the world has the correct dimensions,
- the spawn area is not empty,
- the surface exists,
- caves contain `AIR`,
- underground contains `STONE`,
- ores exist,
- out-of-bounds queries do not crash.

---

# 24. Add a temporary world-generation debug view

Before Module 3, a simple debug visualization is useful.

For example, map tiles to characters:

```text
AIR   = ' '
GRASS = 'G'
DIRT  = 'D'
STONE = 'S'
COAL  = 'C'
IRON  = 'I'
GOLD  = 'O'
```

Then print a small section of the world:

```text
        GGGGGGGGG
       DDDDDDDDDD
      DDDDDDDDDDDD
      SSSSS  SSSS
      SS C    SSS
      S  CCC  SSS
      SSSSSSSSSSS
```

This is not the final renderer.

It is a debugging tool to prove the generation algorithm is doing what we expect.

---

# 25. Module 2 success criteria

Module 2 is complete only when all of these are true:

- [ ] `Tiles.java` exists.
- [ ] Tile IDs are defined.
- [ ] Solid/air classification works.
- [ ] `World.java` exists.
- [ ] World dimensions are `300 × 120`.
- [ ] World generation is deterministic for a fixed seed.
- [ ] Surface height varies smoothly.
- [ ] Grass is placed at the surface.
- [ ] Dirt exists below grass.
- [ ] Stone forms the deeper underground.
- [ ] Caves are carved below the surface.
- [ ] Cave generation does not destroy the protected spawn area.
- [ ] Coal veins are generated.
- [ ] Iron veins are generated.
- [ ] Gold veins are generated.
- [ ] Ores replace only appropriate solid tiles.
- [ ] `getTile()` works.
- [ ] `setTile()` works.
- [ ] `isSolid()` works.
- [ ] Bounds are handled safely.
- [ ] The same seed produces the same world.
- [ ] A different seed produces a different world.
- [ ] The world can be inspected without relying on the final camera renderer.

---

# 26. What Module 2 does NOT implement

Do not put these into Module 2:

- player gravity,
- jumping,
- player collision,
- camera following,
- mining input,
- block-breaking animation,
- inventory,
- enemies,
- combat,
- crafting,
- save/load,
- lighting,
- day/night rendering.

Those systems depend on the world model but should be implemented separately.

---

# 27. Important correction from Module 1

The original specification describes Module 2 as using layered noise for terrain, caves, and ore veins.

We will keep that goal, but **we will not introduce an unnecessary third-party noise library just for this module**.

Instead:

```text
deterministic value noise
+
interpolation
+
multiple frequencies
```

is enough to establish the generation architecture.

This keeps the project:

- dependency-light,
- understandable,
- reproducible,
- easier to debug.

If the generated terrain later needs more sophisticated noise, the noise implementation can be replaced without changing the `World` API.

---

# 28. Module 2 development order

Build it in this order:

### Step 1

Create `Tiles.java`.

Test tile IDs and properties.

### Step 2

Create `World.java`.

Test the `300 × 120` array and bounds.

### Step 3

Generate a flat terrain.

Confirm:

```text
grass
dirt
stone
```

work before adding noise.

### Step 4

Add smooth terrain variation.

### Step 5

Add caves.

### Step 6

Add coal, iron, and gold veins.

### Step 7

Protect spawn.

### Step 8

Add a debug world inspection.

### Step 9

Connect the generated world to `Main.java`.

### Step 10

Leave camera optimization for Module 3.

---

# 29. Final target

At the end of Module 2, the game should conceptually have:

```text
                 SKY

        /\          __
    ___/  \____  __/  \__
___/            \/        \____
GGGGGGGGGGGGGGGGGGGGGGGGGGGGGG
DDDDDDDDDDDDDDDDDDDDDDDDDDDDDD
DDDDDDDDDDDDDDDDDDDDDDDDDDDDDD
SSSSSSSSS    SSSSSSSSSSSSSSSS
SSSSS   C      SSSSS   I SSSSS
SSSS  CCC     SSSSS  III SSSS
SSSSSSSSS  SSSSSSSSSSSSSSSSSS
SSSSSSSSSSSSS     SSSSSSSSSSS
SSSSSSSSSSS  OOO   SSSSSSSSSS
SSSSSSSSSS   OOO   SSSSSSSSSS
SSSSSSSSSSS   O   SSSSSSSSSSS
```

This is **world data**, not the final visual presentation.

Module 3 will turn this data into efficient on-screen tile rendering and introduce camera behavior.

---

# Module 2 completion rule

Do not consider Module 2 complete just because the game window shows some terrain.

The real milestone is:

```text
Tiles
  ↓
World grid
  ↓
Deterministic terrain
  ↓
Caves
  ↓
Ore veins
  ↓
Safe spawn
  ↓
Clean world-query API
```

Once this pipeline works, the later systems have a reliable world to build on.
