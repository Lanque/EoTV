package ee.eotv.echoes.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
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
import ee.eotv.echoes.managers.SaveManager;

import java.util.Iterator;

public class GameScreen implements Screen {

    private final Main game;

    private LevelManager levelManager;
    private Player player;
    private Enemy zombi;
    private StoneManager stoneManager;
    private OrthographicCamera camera;
    private Hud hud;
    private SoundManager soundManager;

    // --- MENÜÜD ---
    private boolean isPaused = false;
    private boolean isVictory = false;

    private Stage stage;
    private Table menuTable;
    private Table victoryTable;
    private Skin skin;

    // --- PARANDUS: Lisasime boolean loadFromSave ---
    public GameScreen(Main game, boolean loadFromSave) {
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

        // UI Seadistamine
        stage = new Stage(new ScreenViewport());
        skin = createBasicSkin();

        createPauseMenu();
        createVictoryMenu();

        // --- KUI ON VAJA LAADIDA, SIIS LAEME ---
        if (loadFromSave && SaveManager.hasSave()) {
            SaveManager.loadGame(player);
        }
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

        TextButton exitBtn = new TextButton("EXIT TO MENU", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });

        menuTable.add(resumeBtn).width(200).height(50).pad(10).row();
        menuTable.add(saveBtn).width(200).height(50).pad(10).row();
        menuTable.add(loadBtn).width(200).height(50).pad(10).row();
        menuTable.add(exitBtn).width(200).height(50).pad(10);

        stage.addActor(menuTable);
    }

    private void createVictoryMenu() {
        victoryTable = new Table();
        victoryTable.setFillParent(true);
        victoryTable.setVisible(false);

        Label.LabelStyle labelStyle = new Label.LabelStyle(skin.getFont("default"), Color.GREEN);
        Label winLabel = new Label("VICTORY!", labelStyle);
        winLabel.setFontScale(2.0f);

        TextButton restartBtn = new TextButton("PLAY AGAIN", skin);
        restartBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Alustame uut mängu (false = ära lae vana seisu)
                game.setScreen(new GameScreen(game, false));
            }
        });

        TextButton exitBtn = new TextButton("EXIT TO MENU", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });

        victoryTable.add(winLabel).padBottom(50).row();
        victoryTable.add(restartBtn).width(200).height(50).pad(10).row();
        victoryTable.add(exitBtn).width(200).height(50).pad(10);

        stage.addActor(victoryTable);
    }

    private void togglePause() {
        if (isVictory) return;

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
        // --- Escape menüü ---
        if (!isVictory && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }

        if (!isPaused && !isVictory) {

            // Game Over loogika
            if (levelManager.contactListener.isGameOver) {
                game.setScreen(new GameScreen(game, false));
                return;
            }

            player.update(delta, levelManager, camera, soundManager);
            stoneManager.handleInput(player, camera);

            levelManager.update(delta);
            stoneManager.update(delta);
            if (zombi != null) zombi.update(player);

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

            if (levelManager.getExitZone() != null) {
                Rectangle playerRect = new Rectangle(
                    player.getPosition().x - 0.2f,
                    player.getPosition().y - 0.2f,
                    0.4f, 0.4f
                );

                if (playerRect.overlaps(levelManager.getExitZone().getBounds())) {
                    isVictory = true;
                    victoryTable.setVisible(true);
                    Gdx.input.setInputProcessor(stage);
                }
            }

            camera.position.set(player.getPosition(), 0);
            camera.update();
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Kasutame lihtsat joonistamist (ilma batchita)
        levelManager.drawWorld(camera);
        levelManager.drawItems(camera);
        levelManager.drawDoors(camera);
        levelManager.drawCharacters(player, zombi, camera);

        levelManager.renderLights(camera);
        stoneManager.renderTrajectory(player, camera);

        hud.render(player, camera);

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
        hud.dispose();
        soundManager.dispose();
        stage.dispose();
        skin.dispose();
    }
}
