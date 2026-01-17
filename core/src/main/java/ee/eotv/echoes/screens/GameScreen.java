package ee.eotv.echoes.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import ee.eotv.echoes.Main;
import ee.eotv.echoes.entities.Player;
import ee.eotv.echoes.entities.Enemy;
import ee.eotv.echoes.entities.Item;
import ee.eotv.echoes.managers.StoneManager;
import ee.eotv.echoes.world.LevelManager;
import ee.eotv.echoes.ui.Hud;
import ee.eotv.echoes.managers.SoundManager;
import java.util.Iterator;

public class GameScreen implements Screen {

    private final Main game;

    private LevelManager levelManager;
    private Player player;
    private Enemy zombi;
    private StoneManager stoneManager;
    private OrthographicCamera camera;
    private Box2DDebugRenderer debugRenderer;
    private Hud hud;
    private SoundManager soundManager;

    public GameScreen(Main game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 40, 30);

        levelManager = new LevelManager();
        player = new Player(levelManager.world, levelManager.rayHandler, 2, 14);
        zombi = new Enemy(levelManager.world, 18, 10);
        Main.zombiInstance = zombi;

        soundManager = new SoundManager();

        // --- PARANDATUD: Kasutame 3-argumendilist konstruktorit ---
        stoneManager = new StoneManager(levelManager.world, levelManager, soundManager);

        hud = new Hud();

        debugRenderer = new Box2DDebugRenderer();
    }

    @Override
    public void render(float delta) {
        if (levelManager.contactListener.isGameOver) {
            game.setScreen(new GameScreen(game));
            return;
        }

        // --- PARANDATUD: Kasutame 4-argumendilist update meetodit ---
        player.update(delta, levelManager, camera, soundManager);

        stoneManager.handleInput(player, camera);

        levelManager.update(delta);
        stoneManager.update(delta);
        if (zombi != null) zombi.update(player);

        // --- ESEMETE KORJAMINE ---
        Iterator<Item> itemIter = levelManager.getItems().iterator();
        while (itemIter.hasNext()) {
            Item item = itemIter.next();
            if (item.isActive()) {
                if (player.getPosition().dst(item.getPosition()) < 0.8f) {
                    player.collectItem(item);
                    soundManager.playCollect();
                    item.collect();
                    itemIter.remove();
                    System.out.println("Item collected: " + item.getType());
                }
            }
        }

        // --- USTE AVAMINE ---
        for (ee.eotv.echoes.entities.Door door : levelManager.getDoors()) {
            if (!door.isOpen()) {
                // Kasutame getCenter(), et uks avaneks mõlemalt poolt
                if (player.getPosition().dst(door.getCenter()) < 1.5f) {
                    if (player.hasKeycard) {
                        door.open();
                        soundManager.playDoor();
                        System.out.println("UKS AVATUD!");
                    }
                }
            }
        }

        camera.position.set(player.getPosition(), 0);
        camera.update();

        // Joonistamine
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        levelManager.drawWorld(camera);
        levelManager.drawItems(camera);
        levelManager.drawDoors(camera);
        levelManager.drawCharacters(player, zombi, camera);
        levelManager.renderLights(camera);
        stoneManager.renderTrajectory(player, camera);

        hud.render(player, camera);
    }

    @Override public void resize(int width, int height) {}
    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        levelManager.dispose();
        debugRenderer.dispose();
        hud.dispose();
        soundManager.dispose();
    }
}
