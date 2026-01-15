package ee.eotv.echoes;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;

public class Main extends ApplicationAdapter {

    // --- MANAŽERID ---
    private LevelManager levelManager;
    private Player player;
    private Enemy zombi;
    private StoneManager stoneManager;

    // --- GRAAFIKA ---
    private OrthographicCamera camera;
    private Box2DDebugRenderer debugRenderer;

    @Override
    public void create() {
        // 1. Kaamera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 40, 30);

        // 2. Tase (Maailm ja Seinad)
        levelManager = new LevelManager();

        // 3. Tegelased
        // Anname mängijale Leveli maailma ja valguse
        player = new Player(levelManager.world, levelManager.rayHandler, 5, 5);
        zombi = new Enemy(levelManager.world, 15, 10);

        // 4. Kivid
        stoneManager = new StoneManager(levelManager.world);

        // 5. Debug joonistaja (rohelised kastid)
        debugRenderer = new Box2DDebugRenderer();
    }

    @Override
    public void render() {
        // 1. Kontrolli kas mäng on läbi (zombi sai kätte)
        if (levelManager.contactListener.isGameOver) {
            // Restart
            dispose();
            create();
            return;
        }

        // 2. Sisend (Mängija ja Kivid)
        player.handleInput(camera);
        stoneManager.handleInput(player, camera);

        // 3. Loogika uuendused
        levelManager.update(Gdx.graphics.getDeltaTime());
        stoneManager.update(Gdx.graphics.getDeltaTime());

        // Zombi uuendamine (anname talle mängija koordinaadid numbritena!)
        zombi.update(player.getPosition().x, player.getPosition().y);

        // 4. Kaamera järgib mängijat
        camera.position.set(player.getPosition(), 0);
        camera.update();

        // 5. Joonistamine
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Valgus
        levelManager.renderLights(camera);

        // Debug jooned (seinad ja kehad)
        debugRenderer.render(levelManager.world, camera.combined);
    }

    @Override
    public void dispose() {
        levelManager.dispose();
        debugRenderer.dispose();
    }
}
