package ee.eotv.echoes.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label; // UUS IMPORT
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import ee.eotv.echoes.Main;
import ee.eotv.echoes.entities.Player;
import ee.eotv.echoes.entities.Enemy;
import ee.eotv.echoes.entities.Item;
import ee.eotv.echoes.managers.StoneManager;
import ee.eotv.echoes.world.LevelManager;
import ee.eotv.echoes.ui.Hud;
import ee.eotv.echoes.managers.SoundManager;
import ee.eotv.echoes.managers.SaveManager; // Import, et salvestamine töötaks

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

    // --- MENÜÜD ---
    private boolean isPaused = false;
    private boolean isVictory = false; // UUS: Kas mäng on võidetud?

    private Stage stage;
    private Table menuTable;    // Pausi menüü
    private Table victoryTable; // Võidu menüü (UUS)
    private Skin skin;

    public GameScreen(Main game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 40, 30);

        levelManager = new LevelManager();
        player = new Player(levelManager.world, levelManager.rayHandler, 2, 14);
        zombi = new Enemy(levelManager.world, 18, 10);
        Main.zombiInstance = zombi;

        soundManager = new SoundManager();
        stoneManager = new StoneManager(levelManager.world, levelManager, soundManager);

        hud = new Hud();
        debugRenderer = new Box2DDebugRenderer();

        // UI Seadistamine
        stage = new Stage(new ScreenViewport());
        skin = createBasicSkin();

        createPauseMenu();   // Loome pausi menüü
        createVictoryMenu(); // Loome võidu menüü
    }

    private void createPauseMenu() {
        menuTable = new Table();
        menuTable.setFillParent(true);
        menuTable.setVisible(false);

        TextButton resumeBtn = new TextButton("RESUME", skin);
        resumeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                togglePause();
            }
        });

        TextButton saveBtn = new TextButton("SAVE GAME", skin);
        saveBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                SaveManager.saveGame(player);
            }
        });

        TextButton loadBtn = new TextButton("LOAD GAME", skin);
        loadBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (SaveManager.hasSave()) {
                    SaveManager.loadGame(player);
                    togglePause();
                }
            }
        });

        TextButton exitBtn = new TextButton("EXIT GAME", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        menuTable.add(resumeBtn).width(200).height(50).pad(10).row();
        menuTable.add(saveBtn).width(200).height(50).pad(10).row();
        menuTable.add(loadBtn).width(200).height(50).pad(10).row();
        menuTable.add(exitBtn).width(200).height(50).pad(10);

        stage.addActor(menuTable);
    }

    // --- UUS: VÕIDUMENÜÜ LOOMINE ---
    private void createVictoryMenu() {
        victoryTable = new Table();
        victoryTable.setFillParent(true);
        victoryTable.setVisible(false);

        // Suur kiri "VICTORY!"
        Label.LabelStyle labelStyle = new Label.LabelStyle(skin.getFont("default"), Color.GREEN);
        Label winLabel = new Label("VICTORY!", labelStyle);
        winLabel.setFontScale(2.0f); // Teeme teksti 2x suuremaks

        // Nupp "PLAY AGAIN"
        TextButton restartBtn = new TextButton("PLAY AGAIN", skin);
        restartBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Taaskäivitame mängu (loome uue GameScreeni)
                game.setScreen(new GameScreen(game));
            }
        });

        // Nupp "EXIT"
        TextButton exitBtn = new TextButton("EXIT", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        victoryTable.add(winLabel).padBottom(50).row();
        victoryTable.add(restartBtn).width(200).height(50).pad(10).row();
        victoryTable.add(exitBtn).width(200).height(50).pad(10);

        stage.addActor(victoryTable);
    }

    private void togglePause() {
        if (isVictory) return; // Kui mäng on läbi, ei saa pausi panna

        isPaused = !isPaused;
        menuTable.setVisible(isPaused);

        if (isPaused) {
            Gdx.input.setInputProcessor(stage);
        } else {
            Gdx.input.setInputProcessor(null);
        }
    }

    private Skin createBasicSkin() {
        Skin skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        skin.add("default", new BitmapFont());

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", 0.3f, 0.3f, 0.3f, 0.9f);
        textButtonStyle.down = skin.newDrawable("white", 0.5f, 0.5f, 0.5f, 0.9f);
        textButtonStyle.over = skin.newDrawable("white", 0.4f, 0.4f, 0.4f, 0.9f);
        textButtonStyle.font = skin.getFont("default");
        skin.add("default", textButtonStyle);

        return skin;
    }

    @Override
    public void render(float delta) {
        // Pausi nupp (ESCAPE) - töötab ainult siis, kui mäng pole veel läbi
        if (!isVictory && (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))) {
            togglePause();
        }

        // --- MÄNGU LOOGIKA (Töötab ainult kui pole pausi ega võitu) ---
        if (!isPaused && !isVictory) {

            // Game Over (Zombi sai kätte)
            if (levelManager.contactListener.isGameOver) {
                game.setScreen(new GameScreen(game)); // Restart
                return;
            }

            player.update(delta, levelManager, camera, soundManager);
            stoneManager.handleInput(player, camera);

            levelManager.update(delta);
            stoneManager.update(delta);
            if (zombi != null) zombi.update(player);

            // Korjamine
            Iterator<Item> itemIter = levelManager.getItems().iterator();
            while (itemIter.hasNext()) {
                Item item = itemIter.next();
                if (item.isActive()) {
                    if (player.getPosition().dst(item.getPosition()) < 0.8f) {
                        player.collectItem(item);
                        soundManager.playCollect();
                        item.collect();
                        itemIter.remove();
                    }
                }
            }

            // Uksed
            for (ee.eotv.echoes.entities.Door door : levelManager.getDoors()) {
                if (!door.isOpen()) {
                    if (player.getPosition().dst(door.getCenter()) < 1.5f) {
                        if (player.hasKeycard) {
                            door.open();
                            soundManager.playDoor();
                        }
                    }
                }
            }

            // --- VÕIDU KONTROLL ---
            if (levelManager.getExitZone() != null) {
                Rectangle playerRect = new Rectangle(
                    player.getPosition().x - 0.2f,
                    player.getPosition().y - 0.2f,
                    0.4f, 0.4f
                );

                if (playerRect.overlaps(levelManager.getExitZone().getBounds())) {
                    // MÄNG LÄBI - VÕIT!
                    isVictory = true;
                    victoryTable.setVisible(true); // Näitame võidumenüüd
                    Gdx.input.setInputProcessor(stage); // Hiir menüüle
                    System.out.println("VICTORY! ESCAPED!");
                }
            }

            camera.position.set(player.getPosition(), 0);
            camera.update();
        }

        // --- JOONISTAMINE ---
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        levelManager.drawWorld(camera);
        levelManager.drawItems(camera);
        levelManager.drawDoors(camera);
        levelManager.drawCharacters(player, zombi, camera);
        levelManager.renderLights(camera);
        stoneManager.renderTrajectory(player, camera);

        hud.render(player, camera);

        // --- MENÜÜDE JOONISTAMINE (Paus või Võit) ---
        if (isPaused || isVictory) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            stage.act(delta);
            stage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

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
        stage.dispose();
        skin.dispose();
    }
}
