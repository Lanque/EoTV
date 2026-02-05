package ee.eotv.echoes.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import box2dLight.PointLight;
import box2dLight.RayHandler;
import ee.eotv.echoes.entities.Item;
import ee.eotv.echoes.entities.Player;
import ee.eotv.echoes.entities.Enemy;
import ee.eotv.echoes.entities.Door;
import ee.eotv.echoes.entities.ExitZone;

import java.util.ArrayList;
import java.util.Iterator;

public class LevelManager {
    public World world;
    public RayHandler rayHandler;
    public WorldContactListener contactListener;
    private ShapeRenderer shapeRenderer;
    private ArrayList<PointLight> activeEchoes = new ArrayList<>();

    // Nimekirjad objektidest
    private ArrayList<Item> items = new ArrayList<>();
    private ArrayList<Door> doors = new ArrayList<>();
    private ArrayList<Enemy> enemies = new ArrayList<>(); // UUS: Nimekiri vaenlastest
    private ExitZone exitZone;

    // --- SUUR KAART (60x30) ---
    // Saad siin joonistada leveli kasutades sümboleid.

    //  # = Sein
    //
    //  . = Põrand (tühi)
    //
    //  K = Võtmekaart (Keycard)
    //
    //  S = Kivi (Stone)
    //
    //  D = Uks (Door)
    //
    //  Z = Vaenlane (Zombie)
    //
    //  E = Võidutsoon (Exit)
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
        "#..........#...#.................#.........................#",
        "#...########...#....Z....#.......#........##.#######.......#",
        "#...#......#...#.........#.......#.................#.......#",
        "#...#..S...#...###########.......#........#...Z....#.......#",
        "#...#......#.....................D........#........#.......#",
        "#...##.#####...###########.......#........##########.......#",
        "#..............#.........#.......#.........................#",
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
        world = new World(new Vector2(0, 0), true);
        contactListener = new WorldContactListener();
        world.setContactListener(contactListener);
        shapeRenderer = new ShapeRenderer();

        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0f, 0f, 0f, 0f);
        rayHandler.setShadows(true);
        rayHandler.setBlurNum(1);

        // Parsime kaardi ja loome maailma
        createLevelFromMap();
    }

    // --- PARSIMISE LOOGIKA ---
    private void createLevelFromMap() {
        float tileSize = 1.0f;

        // Pöörame y-telje ümber, et stringi ülemine rida oleks maailma ülemine osa
        for (int y = 0; y < levelLayout.length; y++) {
            String row = levelLayout[y];
            for (int x = 0; x < row.length(); x++) {
                // Arvutame maailma koordinaadid
                float worldX = x * tileSize;
                float worldY = (levelLayout.length - 1 - y) * tileSize;

                char symbol = row.charAt(x);

                switch (symbol) {
                    case '#': // Sein
                        createWall(worldX, worldY, tileSize);
                        break;
                    case 'K': // Keycard
                        spawnItem(Item.Type.KEYCARD, worldX + 0.5f, worldY + 0.5f);
                        break;
                    case 'S': // Stone
                        spawnItem(Item.Type.STONE, worldX + 0.5f, worldY + 0.5f);
                        break;
                    case 'D': // Door
                        spawnDoor(worldX, worldY); // Uks on 1x1 plokk
                        break;
                    case 'Z': // Zombie
                        spawnEnemy(worldX + 0.5f, worldY + 0.5f);
                        break;
                    case 'E': // Exit Zone
                        exitZone = new ExitZone(worldX, worldY, 2, 2);
                        break;
                    default:
                        // '.' on põrand (tühi), midagi ei tee
                        break;
                }
            }
        }
    }

    public ExitZone getExitZone() { return exitZone; }

    public void spawnItem(Item.Type type, float x, float y) { items.add(new Item(type, x, y)); }
    public ArrayList<Item> getItems() { return items; }

    public void spawnDoor(float x, float y) { doors.add(new Door(world, x, y)); }
    public ArrayList<Door> getDoors() { return doors; }

    // UUS: Vaenlaste lisamine ja küsimine
    public void spawnEnemy(float x, float y) {
        Enemy enemy = new Enemy(world, x, y);
        enemy.setPatrolPoints(buildPatrolRoute(x, y));
        enemies.add(enemy);
    }
    public ArrayList<Enemy> getEnemies() { return enemies; }

    public void drawWorld(OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float tileSize = 1.0f;
        for (int y = 0; y < levelLayout.length; y++) {
            String row = levelLayout[y];
            for (int x = 0; x < row.length(); x++) {
                float worldX = x * tileSize;
                float worldY = (levelLayout.length - 1 - y) * tileSize;

                if (row.charAt(x) == '#') {
                    shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
                    shapeRenderer.rect(worldX, worldY, tileSize, tileSize);
                } else {
                    // Põranda värv
                    shapeRenderer.setColor(0.05f, 0.05f, 0.05f, 1f);
                    shapeRenderer.rect(worldX, worldY, tileSize, tileSize);
                }
            }
        }
        if (exitZone != null) exitZone.render(shapeRenderer);
        shapeRenderer.end();
    }

    public void drawItems(OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Item item : items) item.render(shapeRenderer);
        shapeRenderer.end();
    }

    public void drawDoors(OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Door door : doors) door.render(shapeRenderer);
        shapeRenderer.end();
    }

    // UUS: Joonistab nüüd kõiki vaenlasi nimekirjast
    public void drawEnemies(OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);

        for (Enemy enemy : enemies) {
            shapeRenderer.circle(enemy.getPosition().x, enemy.getPosition().y, 0.4f, 16);
        }

        shapeRenderer.end();
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

        if (points.size == 0) {
            points.add(new Vector2(worldX, worldY));
        }

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
        PointLight echo = new PointLight(rayHandler, 64, color, radius, x, y);
        echo.setSoft(true);
        activeEchoes.add(echo);
    }

    public void addEcho(float x, float y) {
        addEcho(x, y, 15f, new Color(0.4f, 0.7f, 1f, 1f));
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

    public void renderLights(OrthographicCamera camera) {
        rayHandler.setCombinedMatrix(camera.combined, camera.position.x, camera.position.y, camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom);
        rayHandler.updateAndRender();
    }

    public void dispose() {
        world.dispose();
        rayHandler.dispose();
        shapeRenderer.dispose();
    }
}
