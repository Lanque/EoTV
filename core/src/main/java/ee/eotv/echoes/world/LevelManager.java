package ee.eotv.echoes.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import box2dLight.PointLight;
import box2dLight.RayHandler;
import ee.eotv.echoes.entities.Item;
import ee.eotv.echoes.entities.Player;
import ee.eotv.echoes.entities.Enemy;
import ee.eotv.echoes.entities.Door;
import ee.eotv.echoes.entities.ExitZone; // SEE IMPORT OLI PUUDU

import java.util.ArrayList;
import java.util.Iterator;

public class LevelManager {
    public World world;
    public RayHandler rayHandler;
    public WorldContactListener contactListener;
    private ShapeRenderer shapeRenderer;
    private ArrayList<PointLight> activeEchoes = new ArrayList<>();

    // Mänguobjektide nimekirjad
    private ArrayList<Item> items = new ArrayList<>();
    private ArrayList<Door> doors = new ArrayList<>();

    // --- UUS: VÕIDUTSOON ---
    private ExitZone exitZone;

    private String[] levelLayout = {
        "########################################",
        "#......................................#",
        "#..##########..........##########......#",
        "#..#........#..........#........#......#",
        "#..#........#..........#........#......#",
        "#..#...######..........######...#......#",
        "#..#...#....................#...#......#",
        "#..#...#.......######.......#...#......#",
        "#..#...#............#.......#...#......#",
        "#..#...#.......######.......#...#......#",
        "#..#...#....................#...#......#",
        "#..#...######..........######...#......#",
        "#..#........#..........#........#......#",
        "#..#........#..........#...............#",
        "#..##########..........##########......#",
        "#......................................#",
        "########################################"
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

        createProceduralLevel();

        // --- LISAME ASJAD MAAILMA ---
        spawnItem(Item.Type.KEYCARD, 9, 8); // Võti
        spawnDoor(32, 4); // Uks

        // --- LISAME VÕIDUTSOONI ---
        // Asub ukse taga (x=34, y=4), suurus 2x2 meetrit
        exitZone = new ExitZone(30, 8, 2, 2);
    }

    // --- SEE MEETOD OLI PUUDU ---
    public ExitZone getExitZone() {
        return exitZone;
    }

    public void spawnItem(Item.Type type, float x, float y) {
        items.add(new Item(type, x, y));
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    public void spawnDoor(float x, float y) {
        doors.add(new Door(world, x, y));
    }

    public ArrayList<Door> getDoors() {
        return doors;
    }

    public void drawWorld(OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // 1. Seinad ja põrand
        float tileSize = 1.0f;
        for (int y = 0; y < levelLayout.length; y++) {
            String row = levelLayout[y];
            for (int x = 0; x < row.length(); x++) {
                float worldX = x * tileSize;
                float worldY = (levelLayout.length - y) * tileSize;

                if (row.charAt(x) == '#') {
                    shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
                    shapeRenderer.rect(worldX, worldY, tileSize, tileSize);
                } else {
                    shapeRenderer.setColor(0.05f, 0.05f, 0.05f, 1f);
                    shapeRenderer.rect(worldX, worldY, tileSize, tileSize);
                }
            }
        }

        // 2. Joonistame võidutsooni põrandale
        if (exitZone != null) {
            exitZone.render(shapeRenderer);
        }

        shapeRenderer.end();
    }

    public void drawItems(OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Item item : items) {
            item.render(shapeRenderer);
        }
        shapeRenderer.end();
    }

    public void drawDoors(OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Door door : doors) {
            door.render(shapeRenderer);
        }
        shapeRenderer.end();
    }

    public void drawCharacters(Player player, Enemy enemy, OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(Color.CYAN);
        shapeRenderer.circle(player.getPosition().x, player.getPosition().y, 0.4f, 16);

        if (enemy != null) {
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.circle(enemy.getPosition().x, enemy.getPosition().y, 0.4f, 16);
        }

        shapeRenderer.end();
    }

    private void createProceduralLevel() {
        float tileSize = 1.0f;
        for (int y = 0; y < levelLayout.length; y++) {
            String row = levelLayout[y];
            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) == '#') {
                    createWall(x * tileSize, (levelLayout.length - y) * tileSize, tileSize);
                }
            }
        }
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
