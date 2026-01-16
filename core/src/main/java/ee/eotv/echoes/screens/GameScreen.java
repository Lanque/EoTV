package ee.eotv.echoes.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import ee.eotv.echoes.Main;
import ee.eotv.echoes.entities.Player;
import ee.eotv.echoes.entities.Enemy;
import ee.eotv.echoes.entities.Item; // Import
import ee.eotv.echoes.managers.StoneManager;
import ee.eotv.echoes.world.LevelManager;
import ee.eotv.echoes.ui.Hud;
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

    public GameScreen(Main game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 40, 30);

        levelManager = new LevelManager();
        player = new Player(levelManager.world, levelManager.rayHandler, 2, 14);
        zombi = new Enemy(levelManager.world, 18, 10);
        Main.zombiInstance = zombi;

        stoneManager = new StoneManager(levelManager.world, levelManager);

        hud = new Hud();

        debugRenderer = new Box2DDebugRenderer();
    }

    @Override
    public void render(float delta) {
        // 1. Game Over
        if (levelManager.contactListener.isGameOver) {
            game.setScreen(new GameScreen(game));
            return;
        }

        // 2. Update
        player.update(delta, levelManager, camera);
        stoneManager.handleInput(player, camera);

        levelManager.update(delta);
        stoneManager.update(delta);
        if (zombi != null) zombi.update(player);

        // --- UUS: ESEMETE KORJAMINE ---
        Iterator<Item> itemIter = levelManager.getItems().iterator();
        while (itemIter.hasNext()) {
            Item item = itemIter.next();
            if (item.isActive()) {
                // Kontrollime, kas mängija on eseme lähedal (0.8 ühikut)
                if (player.getPosition().dst(item.getPosition()) < 0.8f) {
                    player.collectItem(item);
                    item.collect(); // Deaktiveerime eseme
                    itemIter.remove(); // Eemaldame listist
                    System.out.println("Item collected: " + item.getType());
                }
            }
        }

        // --- UUS: USTE AVAMINE ---
        for (ee.eotv.echoes.entities.Door door : levelManager.getDoors()) {
            if (!door.isOpen()) {
                // Kontrollime kaugust (1.5 meetrit on paras "käeulatus")
                if (player.getPosition().dst(door.getPosition()) < 1.5f) {

                    // Kas mängijal on võti?
                    if (player.hasKeycard) {
                        door.open();
                        System.out.println("UKS AVATUD!");
                        // Siia sobib tulevikus heli: SoundManager.playDoorOpen();
                    } else {
                        // Siia võiks panna ekraanile kirja "LOCKED" või heli "Locked sound"
                        // System.out.println("Uks on lukus! Vaja kaarti.");
                    }
                }
            }
        }

        camera.position.set(player.getPosition(), 0);
        camera.update();

        // 3. Draw
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        levelManager.drawWorld(camera);

        // --- UUS: Joonistame esemed enne karaktereid ---
        levelManager.drawItems(camera);

        // --- UUS: Joonista uksed ---
        levelManager.drawDoors(camera);

        levelManager.drawCharacters(player, zombi, camera);
        levelManager.renderLights(camera);
        stoneManager.renderTrajectory(player, camera);

        hud.render(player, camera);
    }

    @Override
    public void resize(int width, int height) {}

    @Override
    public void show() {}
    @Override
    public void pause() {}
    @Override
    public void resume() {}
    @Override
    public void hide() {}

    @Override
    public void dispose() {
        levelManager.dispose();
        debugRenderer.dispose();
        hud.dispose();
    }
}
