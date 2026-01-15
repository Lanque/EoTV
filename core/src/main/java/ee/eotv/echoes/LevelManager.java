package ee.eotv.echoes;

import com.badlogic.gdx.graphics.OrthographicCamera; // <--- TÄHTIS IMPORT
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import box2dLight.RayHandler;

public class LevelManager {
    public World world;
    public TiledMap map;
    public RayHandler rayHandler;
    public WorldContactListener contactListener;

    public LevelManager() {
        // 1. Füüsikamaailm
        world = new World(new Vector2(0, 0), true);

        // 2. Kontaktide kuulaja (Meie kohtunik)
        contactListener = new WorldContactListener();
        world.setContactListener(contactListener);

        // 3. Valgus
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0.1f, 0.1f, 0.1f, 0.4f); // Hämar

        // 4. Kaart
        try {
            map = new TmxMapLoader().load("maps/level1.tmx");
            parseWalls(map);
        } catch (Exception e) {
            System.out.println("VIGA: Kaarti ei leitud!");
        }
    }

    private void parseWalls(TiledMap map) {
        MapLayer layer = map.getLayers().get("physics");
        if (layer == null) return;

        for (MapObject object : layer.getObjects()) {
            if (object instanceof RectangleMapObject) {
                Rectangle rect = ((RectangleMapObject) object).getRectangle();
                float PPM = 32.0f;
                float w = rect.width / PPM;
                float h = rect.height / PPM;

                BodyDef bdef = new BodyDef();
                bdef.type = BodyDef.BodyType.StaticBody;
                bdef.position.set((rect.x / PPM) + w / 2, (rect.y / PPM) + h / 2);

                Body body = world.createBody(bdef);
                PolygonShape shape = new PolygonShape();
                shape.setAsBox(w / 2, h / 2);
                body.createFixture(shape, 0.0f);
                shape.dispose();
            }
        }
    }

    public void update(float delta) {
        world.step(1/60f, 6, 2);
    }

    // --- SIIN OLI VIGA ---
    // Muutsime "Camera" -> "OrthographicCamera", sest ainult sellel on "zoom"
    public void renderLights(OrthographicCamera camera) {
        rayHandler.setCombinedMatrix(camera.combined, camera.position.x, camera.position.y, camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom);
        rayHandler.updateAndRender();
    }
    // ---------------------

    public void dispose() {
        world.dispose();
        map.dispose();
        rayHandler.dispose();
    }
}
