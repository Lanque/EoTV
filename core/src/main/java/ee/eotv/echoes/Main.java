package ee.eotv.echoes;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.GL20;

public class Main extends ApplicationAdapter {
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private OrthographicCamera camera;

    // UUS: Mängija keha
    private com.badlogic.gdx.physics.box2d.Body playerBody;

    // Valgussüsteem
    private box2dLight.RayHandler rayHandler;
    private box2dLight.ConeLight flashlight;

    @Override
    public void create() {
        // --- 1. Maailma loomine ---
        // Vector2(0, 0) tähendab, et gravitatsiooni pole (sest vaade on pealt).
        // "true" tähendab, et magavad objektid (mis ei liigu) jäetakse rahule (optimeerimine).
        world = new World(new Vector2(0, 0), true);

        // See on tööriist, mis joonistab rohelised jooned ümber füüsikaobjektide
        debugRenderer = new Box2DDebugRenderer();

        // --- 2. Kaamera seadistamine ---
        camera = new OrthographicCamera();
        // Siin on konks: Box2D töötab MEETRITES, mitte pikslites.
        // Me ütleme kaamerale: "Näita meile ala, mis on 20 meetrit lai ja 15 meetrit kõrge".
        camera.setToOrtho(false, 20, 15);

        // --- TEST: Teeme ühe kasti (Keha) ---
        // 1. Defineeri keha omadused
        com.badlogic.gdx.physics.box2d.BodyDef bodyDef = new com.badlogic.gdx.physics.box2d.BodyDef();
        bodyDef.type = com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody; // Staatiline = Sein (ei liigu)
        bodyDef.position.set(10, 7); // Pane see ekraani keskele (10m paremale, 7m üles)

        // 2. Loo keha maailma
        com.badlogic.gdx.physics.box2d.Body squareBody = world.createBody(bodyDef);

        // 3. Defineeri kuju (Kast)
        com.badlogic.gdx.physics.box2d.PolygonShape squareShape = new com.badlogic.gdx.physics.box2d.PolygonShape();
        squareShape.setAsBox(1, 1); // See on 2x2 meetrit suur kast (1m keskelt igas suunas)

        // 4. Liida kuju kehaga
        squareBody.createFixture(squareShape, 0.0f);

        // 5. Viska kuju minema (seda pole enam vaja, koopia on kehas olemas)
        squareShape.dispose();

        // --- MÄNGIJA LOOMINE ---
        // 1. Keha definitsioon (DynamicBody = Liigub ja omab massi)
        com.badlogic.gdx.physics.box2d.BodyDef playerDef = new com.badlogic.gdx.physics.box2d.BodyDef();
        playerDef.type = com.badlogic.gdx.physics.box2d.BodyDef.BodyType.DynamicBody;
        playerDef.position.set(5, 5); // Alustab koordinaatidelt 5, 5
        playerDef.fixedRotation = true; // Tähtis! Muidu hakkab mängija veerema nagu pall

        playerBody = world.createBody(playerDef);

        // 2. Kuju (Ring on parem kui kast, sest ei jää nurkadesse kinni)
        com.badlogic.gdx.physics.box2d.CircleShape circle = new com.badlogic.gdx.physics.box2d.CircleShape();
        circle.setRadius(0.5f); // Poole meetrise raadiusega (1m laiune)

        // 3. Füüsika omadused (Fixture)
        com.badlogic.gdx.physics.box2d.FixtureDef fixtureDef = new com.badlogic.gdx.physics.box2d.FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 1.0f; // Tihedus (annab massi)
        fixtureDef.friction = 0.0f; // Hõõrdumine 0, et ei jääks seina külge kinni

        playerBody.createFixture(fixtureDef);
        circle.dispose(); // Kuju pole enam vaja

        // --- VALGUSE SEADISTAMINE ---

        // 1. Loo valguse haldur ja seondu see füüsikamaailmaga
        rayHandler = new box2dLight.RayHandler(world);

        // 2. Määra "Ambient Light" ehk üldvalgus.
        // 0, 0, 0, 1 = Täiesti must (kottpime).
        // 0.1f, 0.1f, 0.1f, 1f = Väga hämar (nagu kuuvalgus).
        rayHandler.setAmbientLight(0.2f, 0.2f, 0.2f, 1f); // Hallikas valgus


        // 3. Loo taskulamp (ConeLight)
        // Parameetrid: (haldur, kiirte arv, värv, kaugus meetrites, x, y, suund kraadides, koonuse laius kraadides)
        flashlight = new box2dLight.ConeLight(rayHandler, 100, com.badlogic.gdx.graphics.Color.CORAL, 15, 0, 0, 90, 30);

        // 4. Liimi taskulamp mängija külge
        // See tähendab, et valgus liigub automaatselt koos rohelise ringiga.
        // (0, 0) tähendab, et valgus algab täpselt keha keskelt.
        flashlight.attachToBody(playerBody, 0, 0, 0); // 90 kraadi tähendab, et valgus näitab "üles"

        // Optimeerimine (vajalik Desktopi jaoks)
        // box2dLight.RayHandler.useDiffuseLight(true);
    }

    @Override
    public void render() {
        // --- 1. LIIKUMISE LOOGIKA ---
        float horizontal = 0;
        float vertical = 0;
        float speed = 5.0f;

        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) vertical = speed;
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S)) vertical = -speed;
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) horizontal = -speed;
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) horizontal = speed;

        // --- 1b. PÖÖRAMINE HIIRE SUUNAS (UUS!) ---
        // Küsime hiire asukoha ekraanil ja tõlgime selle maailma koordinaatideks
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();
        com.badlogic.gdx.math.Vector3 mousePos = new com.badlogic.gdx.math.Vector3(mouseX, mouseY, 0);
        camera.unproject(mousePos);

        // Arvutame nurga mängija ja hiire vahel
        float playerX = playerBody.getPosition().x;
        float playerY = playerBody.getPosition().y;
        float angle = com.badlogic.gdx.math.MathUtils.atan2(mousePos.y - playerY, mousePos.x - playerX);

        // Pöörame mängijat (ja kuna lamp on küljes, pöörab ka lamp!)
        // angle on radiaanides, box2d vajab radiaane
        playerBody.setTransform(playerX, playerY, angle);

        // Anname kiiruse (säilitades nurga)
        playerBody.setLinearVelocity(horizontal, vertical);

        // --- 2. FÜÜSIKA JA KAAMERA ---
        world.step(1/60f, 6, 2);

        camera.position.x = playerBody.getPosition().x;
        camera.position.y = playerBody.getPosition().y;
        camera.update();

        // --- 3. JOONISTAMINE ---
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_STENCIL_BUFFER_BIT);

        // Valgus
        rayHandler.setCombinedMatrix(camera);
        rayHandler.updateAndRender();

        // Debug jooned
        debugRenderer.render(world, camera.combined);
    }

    @Override
    public void dispose() {
        // Vabasta mälu, kui mäng kinni pannakse
        rayHandler.dispose();
        // (flashlight.dispose() pole vaja, sest rayHandler teeb selle töö ära)

        // 2. Siis abijoonistaja
        debugRenderer.dispose();

        // 3. Kõige lõpuks füüsikamaailm ise
        world.dispose();
    }
}
