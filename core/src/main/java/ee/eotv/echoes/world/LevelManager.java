package ee.eotv.echoes.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import box2dLight.PointLight;
import box2dLight.RayHandler;
import ee.eotv.echoes.entities.Item;
import ee.eotv.echoes.entities.Enemy;
import ee.eotv.echoes.entities.Door;
import ee.eotv.echoes.entities.ExitZone;
import ee.eotv.echoes.entities.Generator;

import java.util.ArrayList;
import java.util.Iterator;

public class LevelManager {
    public World world;
    public RayHandler rayHandler;
    public WorldContactListener contactListener;
    private ArrayList<PointLight> activeEchoes = new ArrayList<>();

    // Nimekirjad objektidest
    private ArrayList<Item> items = new ArrayList<>();
    private ArrayList<Door> doors = new ArrayList<>();
    private ArrayList<Enemy> enemies = new ArrayList<>();
    private ArrayList<Generator> generators = new ArrayList<>();
    private ExitZone exitZone;
    private PointLight exitLight;
    private boolean exitUnlocked = false;

    // --- SUUR KAART (60x30) ---
    private String[] levelLayout = {
        "############################################################",
        "#..........................................................#",
        "#..#######...###########...#################...##########..#",
        "#..#.....#...#.........#...#.......#.......#...#........#..#",
        "#..#..S..#...#....Z....#...#...K...#...Z...#...#........#..#",
        "#..#.....#...#.........#...#.......#.......#...#........#..#",
        "#..###.###...#####.#####...###.#####...#####...###.######..#",
        "#..........................................................#",
        "#######.####...###########.......###########################",
        "#..........#...#.........#.......#.........................#",
        "#..........#...#G................#.........................#",
        "#...########...#....Z....#.......#........##.#######.......#",
        "#...#......#...#.........#.......#.................#.......#",
        "#...#..S...#...###########.......#........#...Z....#.......#",
        "#...#......#.....................D........#.......G#.......#",
        "#...##.#####...###########.......#........##########.......#",
        "#..............#....G....#.......#.........................#",
        "################.........#.......###########################",
        "#........................#.......#.........................#",
        "#..###########...#########.......#.........................#",
        "#..#.........#...#.......#.......#.......###########.......#",
        "#..#....Z....#...#...S...........#.......#.........#.......#",
        "#..#.........#...#.......#.......#.......#....E....#.......#",
        "#..#####.#####...#########.......#.......#.........#.......#",
        "#........................................#.........#.......#",
        "##########################################.........#.......#",
        "#..................................................#.......#",
        "#..#################################################.......#",
        "#..........................................................#",
        "############################################################"
    };

    public LevelManager() {
        this(true);
    }

    public LevelManager(boolean enableLighting) {
        world = new World(new Vector2(0, 0), true);
        contactListener = new WorldContactListener();
        world.setContactListener(contactListener);

        if (enableLighting) {
            rayHandler = new RayHandler(world);
            rayHandler.setAmbientLight(0f, 0f, 0f, 0f);
            rayHandler.setShadows(true);
            rayHandler.setBlurNum(1);
        } else {
            rayHandler = null;
        }

        createLevelFromMap();
        setExitUnlocked(generators.isEmpty());
    }

    private void createLevelFromMap() {
        float tileSize = 1.0f;
        for (int y = 0; y < levelLayout.length; y++) {
            String row = levelLayout[y];
            for (int x = 0; x < row.length(); x++) {
                float worldX = x * tileSize;
                float worldY = (levelLayout.length - 1 - y) * tileSize;
                char symbol = row.charAt(x);

                switch (symbol) {
                    case '#': createWall(worldX, worldY, tileSize); break;
                    case 'K': spawnItem(Item.Type.KEYCARD, worldX + 0.5f, worldY + 0.5f); break;
                    case 'S': spawnItem(Item.Type.STONE, worldX + 0.5f, worldY + 0.5f); break;
                    case 'D': spawnDoor(worldX, worldY); break;
                    case 'Z': spawnEnemy(worldX + 0.5f, worldY + 0.5f); break;
                    case 'G': spawnGenerator(worldX + 0.5f, worldY + 0.5f); break;
                    case 'E': exitZone = new ExitZone(worldX, worldY, 2, 2); break;
                }
            }
        }
    }

    public String[] getLevelLayout() { return levelLayout; }

    public ExitZone getExitZone() { return exitZone; }
    public boolean isExitUnlocked() { return exitUnlocked; }

    public void setExitUnlocked(boolean unlocked) {
        if (exitUnlocked == unlocked) return;
        exitUnlocked = unlocked;
        if (rayHandler != null && exitUnlocked && exitZone != null && exitLight == null) {
            float centerX = exitZone.getBounds().x + exitZone.getBounds().width * 0.5f;
            float centerY = exitZone.getBounds().y + exitZone.getBounds().height * 0.5f;
            exitLight = new PointLight(rayHandler, 64, new Color(1f, 0.95f, 0.8f, 1f), 7f, centerX, centerY);
            exitLight.setSoft(true);
        }
    }

    public void spawnItem(Item.Type type, float x, float y) { items.add(new Item(type, x, y)); }
    public ArrayList<Item> getItems() { return items; }

    public void spawnDoor(float x, float y) { doors.add(new Door(world, x, y)); }
    public ArrayList<Door> getDoors() { return doors; }

    public void spawnEnemy(float x, float y) {
        Enemy enemy = new Enemy(world, x, y);
        enemy.setPatrolPoints(buildPatrolRoute(x, y));
        enemies.add(enemy);
    }
    public ArrayList<Enemy> getEnemies() { return enemies; }

    public void spawnGenerator(float x, float y) { generators.add(new Generator(x, y)); }
    public ArrayList<Generator> getGenerators() { return generators; }

    public boolean areAllGeneratorsRepaired() {
        if (generators.isEmpty()) return true;
        for (Generator generator : generators) {
            if (!generator.isRepaired()) return false;
        }
        return true;
    }

    private void createWall(float x, float y, float size) {
        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.StaticBody;
        bdef.position.set(x + size / 2, y + size / 2);
        Body body = world.createBody(bdef);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(size / 2, size / 2);
        body.createFixture(shape, 0.0f);
        shape.dispose();
    }

    private Array<Vector2> buildPatrolRoute(float worldX, float worldY) {
        Array<Vector2> points = new Array<>();
        int tileX = (int) worldX;
        int tileY = (int) worldY;
        int row = levelLayout.length - 1 - tileY;

        addPatrolPoint(points, tileX, row);
        int maxSteps = 6;
        int left = findOpenInDirection(tileX, row, -1, 0, maxSteps);
        int right = findOpenInDirection(tileX, row, 1, 0, maxSteps);
        int up = findOpenInDirection(tileX, row, 0, -1, maxSteps);
        int down = findOpenInDirection(tileX, row, 0, 1, maxSteps);

        addPatrolPoint(points, left, row);
        addPatrolPoint(points, right, row);
        addPatrolPoint(points, tileX, up);
        addPatrolPoint(points, tileX, down);

        if (points.size == 0) points.add(new Vector2(worldX, worldY));
        return points;
    }

    private void addPatrolPoint(Array<Vector2> points, int col, int row) {
        if (!isWalkableTile(col, row)) return;
        float worldX = col + 0.5f;
        float worldY = (levelLayout.length - 1 - row) + 0.5f;
        for (Vector2 p : points) {
            if (p.epsilonEquals(worldX, worldY, 0.01f)) return;
        }
        points.add(new Vector2(worldX, worldY));
    }

    private int findOpenInDirection(int startCol, int startRow, int dx, int dy, int maxSteps) {
        int col = startCol;
        int row = startRow;
        int lastOpenCol = startCol;
        int lastOpenRow = startRow;
        for (int i = 0; i < maxSteps; i++) {
            col += dx;
            row += dy;
            if (!isWalkableTile(col, row)) break;
            lastOpenCol = col;
            lastOpenRow = row;
        }
        return (dx != 0) ? lastOpenCol : lastOpenRow;
    }

    private boolean isWalkableTile(int col, int row) {
        if (row < 0 || row >= levelLayout.length) return false;
        String line = levelLayout[row];
        if (col < 0 || col >= line.length()) return false;
        char tile = line.charAt(col);
        return tile != '#' && tile != 'D';
    }

    public void addEcho(float x, float y, float radius, Color color) {
        if (rayHandler == null) return;
        PointLight echo = new PointLight(rayHandler, 64, color, radius, x, y);
        echo.setSoft(true);
        activeEchoes.add(echo);
    }

    public void update(float delta) {
        world.step(1/60f, 6, 2);
        updateEchoes(delta);
    }

    public void updateClient(float delta) {
        updateEchoes(delta);
    }

    private void updateEchoes(float delta) {
        Iterator<PointLight> iter = activeEchoes.iterator();
        while (iter.hasNext()) {
            PointLight light = iter.next();
            light.setDistance(light.getDistance() - 15f * delta);
            if (light.getDistance() <= 0) {
                light.remove();
                iter.remove();
            }
        }
    }

    public void dispose() {
        if (exitLight != null) exitLight.remove();
        world.dispose();
        if (rayHandler != null) {
            rayHandler.dispose();
        }
    }
}
