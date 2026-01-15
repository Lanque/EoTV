package ee.eotv.echoes;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import box2dLight.RayHandler;
import box2dLight.ConeLight;
import java.util.ArrayList;

public class Main extends ApplicationAdapter {

    // --- KLASSID JA MUUTUJAD ---

    // Abiklass kivi jälgimiseks
    private class Stone {
        Body body;
        float flightTime = 0;       // Kui kaua on lennanud
        float maxFlightTime = 0.6f; // Millal "maha kukub" (sekundites)
        boolean hasLanded = false;  // Kas on juba maandunud

        public Stone(Body body) {
            this.body = body;
        }
    }

    private World world;
    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera camera;
    private Body playerBody;

    // Valgus
    private RayHandler rayHandler;
    private ConeLight flashlight;

    // Viskamine ja Kivid
    private float currentPower = 0f;
    private float maxPower = 1.2f;   // Maksimaalne jõud (väike number on Box2D jaoks suur!)
    private boolean isCharging = false;

    // Nimekiri aktiivsetest kividest
    private ArrayList<Stone> activeStones = new ArrayList<>();

    @Override
    public void create() {
        // 1. MAAILM
        world = new World(new Vector2(0, 0), true);
        debugRenderer = new Box2DDebugRenderer();

        // 2. KAAMERA
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 40, 30);

        // 3. SEIN (TESTIKS)
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(10, 7);
        Body squareBody = world.createBody(bodyDef);
        PolygonShape squareShape = new PolygonShape();
        squareShape.setAsBox(1, 1);
        squareBody.createFixture(squareShape, 0.0f);
        squareShape.dispose();

        // 4. MÄNGIJA
        BodyDef playerDef = new BodyDef();
        playerDef.type = BodyDef.BodyType.DynamicBody;
        playerDef.position.set(5, 5);
        playerDef.fixedRotation = true;
        playerBody = world.createBody(playerDef);

        CircleShape circle = new CircleShape();
        circle.setRadius(0.5f);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 0.0f;
        playerBody.createFixture(fixtureDef);
        circle.dispose();

        // 5. VALGUS
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0.2f, 0.2f, 0.2f, 0.5f); // Hämar tuba

        flashlight = new ConeLight(rayHandler, 100, com.badlogic.gdx.graphics.Color.CORAL, 30, 0, 0, 0, 30);
        flashlight.attachToBody(playerBody, 0, 0, 0); // 0 kraadi = vaatab otse
    }

    @Override
    public void render() {
        // --- 0. KAAMERA UUENDAMINE (KÕIGE ESIMENE!) ---
        // Peame uuendama kaamerat ENNE hiire arvutusi
        camera.position.x = playerBody.getPosition().x;
        camera.position.y = playerBody.getPosition().y;
        camera.update();

        // --- 1. LIIKUMINE JA PÖÖRAMINE ---
        float horizontal = 0;
        float vertical = 0;
        float speed = 5.0f;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) vertical = speed;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) vertical = -speed;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) horizontal = -speed;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) horizontal = speed;

        // Pööramine hiire suunas
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();
        Vector3 mousePos = new Vector3(mouseX, mouseY, 0);
        camera.unproject(mousePos);

        float playerX = playerBody.getPosition().x;
        float playerY = playerBody.getPosition().y;
        float angle = MathUtils.atan2(mousePos.y - playerY, mousePos.x - playerX);

        playerBody.setTransform(playerX, playerY, angle);
        playerBody.setLinearVelocity(horizontal, vertical);

        // --- 2. VISKAMISE LOOGIKA ---

        // LAADIMINE
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            isCharging = true;
            // Laeme aeglaselt (kuna maxPower on väike)
            currentPower += 1.5f * Gdx.graphics.getDeltaTime();

            if (currentPower > maxPower) {
                currentPower = maxPower;
            }

            // Arvutame protsendi näitamiseks
            int percentage = (int)((currentPower / maxPower) * 100);
            System.out.println("Viskejõud: " + percentage + "%");
        }
        // VISKAMINE (kui nupp lahti lasti)
        else if (isCharging) {
            createStone(playerX, playerY, currentPower);
            currentPower = 0f;
            isCharging = false;
        }

        // --- 3. KIVIDE LENNU LOOGIKA ---
        for (int i = 0; i < activeStones.size(); i++) {
            Stone s = activeStones.get(i);

            if (!s.hasLanded) {
                s.flightTime += Gdx.graphics.getDeltaTime();

                // Kui lennuaeg saab täis -> Kivi maandub
                if (s.flightTime >= s.maxFlightTime) {
                    s.hasLanded = true;
                    // Jõhker pidurdus (nagu kukuks maha)
                    s.body.setLinearDamping(10.0f);
                    s.body.setAngularDamping(5.0f);
                    // System.out.println("Kivi maandus!");
                }
            }
        }

        // --- 4. FÜÜSIKA SAMM ---
        world.step(1/60f, 6, 2);

        // --- 5. JOONISTAMINE ---
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_STENCIL_BUFFER_BIT);

        rayHandler.setCombinedMatrix(camera);
        rayHandler.updateAndRender();

        debugRenderer.render(world, camera.combined);
    }

    // UUS MEETOD: Loob kivi, mis lendab ja siis kukub
    private void createStone(float startX, float startY, float power) {
        // 1. Arvutame suuna ja stardikoha
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        Vector2 direction = new Vector2(mousePos.x - startX, mousePos.y - startY);
        direction.nor(); // Pikkus 1

        // Tekitame kivi 0.6m mängija ette (et ei tekiks sisse)
        float spawnX = startX + (direction.x * 0.6f);
        float spawnY = startY + (direction.y * 0.6f);

        // 2. Keha
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(spawnX, spawnY);
        bodyDef.bullet = true; // Täpsem kokkupõrke arvestus
        Body stoneBody = world.createBody(bodyDef);

        // 3. Kuju ja Omadused
        CircleShape circle = new CircleShape();
        circle.setRadius(0.1f);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 2.0f;
        fixtureDef.restitution = 0.3f; // Natuke tönts põrkamine
        stoneBody.createFixture(fixtureDef);
        circle.dispose();

        // 4. Lennu algus (Libiseb vabalt)
        stoneBody.setLinearDamping(0f);
        stoneBody.setAngularDamping(0f);

        // 5. Impulss
        direction.scl(power); // Korrutame jõuga
        stoneBody.applyLinearImpulse(direction, stoneBody.getWorldCenter(), true);

        // 6. Lisame nimekirja
        activeStones.add(new Stone(stoneBody));
    }

    @Override
    public void dispose() {
        rayHandler.dispose();
        debugRenderer.dispose();
        world.dispose();
    }
}
