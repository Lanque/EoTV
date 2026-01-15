package ee.eotv.echoes;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;

public class Main extends ApplicationAdapter {
    private LevelManager levelManager;
    private Player player;
    private Enemy zombi;
    private StoneManager stoneManager;
    private OrthographicCamera camera;
    private Box2DDebugRenderer debugRenderer;

    // Staatiline viide, et teised klassid saaksid zombile sündmusi saata
    public static Enemy zombiInstance;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 40, 30);

        levelManager = new LevelManager();

        // Mängija (2, 14) ja Zombi (18, 10) on uues mapi skeemis vabad kohad
        player = new Player(levelManager.world, levelManager.rayHandler, 2, 14);
        zombi = new Enemy(levelManager.world, 18, 10);
        zombiInstance = zombi;

        stoneManager = new StoneManager(levelManager.world, levelManager);

        debugRenderer = new Box2DDebugRenderer();
    }

    @Override
    public void render() {
        // 1. Kontrolli surma (Restart)
        if (levelManager.contactListener.isGameOver) {
            create();
            return;
        }

        // 2. Sisendi ja loogika uuendused
        player.handleInput(camera);
        stoneManager.handleInput(player, camera);

        levelManager.update(Gdx.graphics.getDeltaTime());
        stoneManager.update(Gdx.graphics.getDeltaTime());

        if (zombi != null) {
            zombi.update(player); // AI reageerib nüüd mängija taskulambile
        }

        // 3. Kaamera liigutamine
        camera.position.set(player.getPosition(), 0);
        camera.update();

        // 4. JOONISTAMINE (Järjekord on ülioluline!)
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // A) Joonistame seina ja põranda tekstuurid (hallid kastid)
        levelManager.drawWorld(camera);

        // B) Joonistame tegelaste ikoonid
        levelManager.drawCharacters(player, zombi, camera);

        // C) Joonistame valguse (see peidab kõik muu pimedusse ja valgustab ainult vihtu)
        levelManager.renderLights(camera);

        // Trajektoor
        stoneManager.renderTrajectory(player, camera);

        // D) Debug vaade (Võta kommentaar eest, kui tahad näha füüsika piire)
        // debugRenderer.render(levelManager.world, camera.combined);
    }

    @Override
    public void dispose() {
        levelManager.dispose();
        debugRenderer.dispose();
    }
}
