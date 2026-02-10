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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
import ee.eotv.echoes.entities.Generator;
import ee.eotv.echoes.managers.StoneManager;
import ee.eotv.echoes.world.LevelManager;
import ee.eotv.echoes.ui.Hud;
import ee.eotv.echoes.ui.WorldRenderer; // <--- UUS IMPORT (Vajalik!)
import ee.eotv.echoes.managers.SoundManager;
import ee.eotv.echoes.managers.SaveManager;

import java.util.Iterator;

public class GameScreen implements Screen {

    private final Main game;

    private LevelManager levelManager;
    private WorldRenderer worldRenderer; // <--- UUS: Renderdaja muutuja
    private Player player;

    private StoneManager stoneManager;
    private OrthographicCamera camera;
    private Hud hud;
    private SoundManager soundManager;
    private BitmapFont overlayFont;
    private ShapeRenderer overlayRenderer;
    private float generatorRepairTime = 10f;
    private float generatorRepairRange = 1.2f;

    // --- MENÜÜD ---
    private boolean isPaused = false;
    private boolean debugDisableLighting = false;
    private boolean isVictory = false;
    private boolean isGameOverState = false;

    private Stage stage;
    private Table menuTable;
    private Table victoryTable;
    private Table gameOverTable;
    private Skin skin;

    public GameScreen(Main game, boolean loadFromSave) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 40, 30);

        levelManager = new LevelManager();

        // --- UUS: Loome renderdaja ---
        worldRenderer = new WorldRenderer(game.batch, levelManager);

        // MÄNGIJA START
        player = new Player(levelManager.world, levelManager.rayHandler, 2, 25);
        player.setRole(Player.Role.ALL); // Üksikmängus on tal kõik asjad

        // ZOMBID (Debug info Main klassile, nagu sul oli)
        if (!levelManager.getEnemies().isEmpty()) {
            Main.zombiInstance = levelManager.getEnemies().get(0);
        }

        soundManager = new SoundManager();
        stoneManager = new StoneManager(levelManager.world, levelManager, soundManager);

        hud = new Hud();
        overlayFont = new BitmapFont();
        overlayRenderer = new ShapeRenderer();

        stage = new Stage(new ScreenViewport());
        skin = createBasicSkin();

        createPauseMenu();
        createVictoryMenu();
        createGameOverMenu();

        if (loadFromSave && SaveManager.hasSave()) {
            SaveManager.loadGame(player);
        }
    }

    @Override
    public void render(float delta) {
        if (!isVictory && !isGameOverState && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            debugDisableLighting = !debugDisableLighting;
            levelManager.setLightingEnabled(!debugDisableLighting);
        }

        // --- 1. GAME LOOP (LOOGIKA) ---
        if (!isPaused && !isVictory && !isGameOverState) {

            if (levelManager.contactListener.isGameOver) {
                isGameOverState = true;
                gameOverTable.setVisible(true);
                Gdx.input.setInputProcessor(stage);
            } else {
                boolean holdRepair = Gdx.input.isKeyPressed(Input.Keys.E);
                Generator repairTarget = (!player.isDowned() && holdRepair) ? findRepairTarget(player) : null;
                player.setFrozen(repairTarget != null);

                player.update(delta, levelManager, camera, soundManager);
                if (repairTarget == null) {
                    stoneManager.handleInput(player, camera);
                }
                levelManager.update(delta);
                stoneManager.update(delta);

                // Vaenlaste AI
                for (Enemy enemy : levelManager.getEnemies()) {
                    if (!enemy.isActive()) continue;
                    enemy.update(player, delta);
                }

                // Esemed
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

                // Võit
                updateGenerators(delta, repairTarget);
                if (levelManager.isExitUnlocked() && levelManager.getExitZone() != null) {
                    Rectangle playerRect = new Rectangle(player.getPosition().x - 0.2f, player.getPosition().y - 0.2f, 0.4f, 0.4f);
                    if (playerRect.overlaps(levelManager.getExitZone().getBounds())) {
                        isVictory = true;
                        victoryTable.setVisible(true);
                        Gdx.input.setInputProcessor(stage);
                    }
                }
            }
        }

        // --- 2. RENDERDAMINE (KÕIK UUS!) ---
        // Vana kood (levelManager.drawWorld jne) on kustutatud.
        // Kasutame WorldRendererit. Teise mängija asemel on 'null', sest see on üksikmäng.
        worldRenderer.render(camera, player, null, stoneManager, true);

        // --- 3. UI JA Overlay ---
        hud.render(player, camera);
        renderGeneratorPrompt();

        if (isPaused || isVictory || isGameOverState) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            stage.act(delta);
            stage.draw();
        }
    }

    // --- MENÜÜDE LOOGIKA (See on sama, mis sul enne oli) ---

    private void createPauseMenu() {
        menuTable = new Table();
        menuTable.setFillParent(true);
        menuTable.setVisible(false);

        TextButton resumeBtn = new TextButton("RESUME", skin);
        resumeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundManager.playClick();
                togglePause();
            }
        });

        TextButton saveBtn = new TextButton("SAVE GAME", skin);
        saveBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundManager.playClick();
                SaveManager.saveGame(player);
            }
        });

        TextButton loadBtn = new TextButton("LOAD GAME", skin);
        loadBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (SaveManager.hasSave()) {
                    soundManager.playClick();
                    SaveManager.loadGame(player);
                    togglePause();
                }
            }
        });

        TextButton exitBtn = new TextButton("EXIT TO MENU", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundManager.playClick();
                soundManager.stopMenuMusic();
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

        Label winLabel = new Label("VICTORY!", new Label.LabelStyle(skin.getFont("default"), Color.GREEN));
        winLabel.setFontScale(2.0f);

        TextButton restartBtn = new TextButton("PLAY AGAIN", skin);
        restartBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundManager.playClick();
                game.setScreen(new GameScreen(game, false));
            }
        });

        TextButton exitBtn = new TextButton("EXIT TO MENU", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundManager.playClick();
                game.setScreen(new MainMenuScreen(game));
            }
        });

        victoryTable.add(winLabel).padBottom(50).row();
        victoryTable.add(restartBtn).width(200).height(50).pad(10).row();
        victoryTable.add(exitBtn).width(200).height(50).pad(10);
        stage.addActor(victoryTable);
    }

    private void createGameOverMenu() {
        gameOverTable = new Table();
        gameOverTable.setFillParent(true);
        gameOverTable.setVisible(false);

        Label loseLabel = new Label("GAME OVER", new Label.LabelStyle(skin.getFont("default"), Color.RED));
        loseLabel.setFontScale(2.0f);

        TextButton restartBtn = new TextButton("TRY AGAIN", skin);
        restartBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundManager.playClick();
                game.setScreen(new GameScreen(game, false));
            }
        });

        TextButton exitBtn = new TextButton("EXIT TO MENU", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                soundManager.playClick();
                game.setScreen(new MainMenuScreen(game));
            }
        });

        gameOverTable.add(loseLabel).padBottom(50).row();
        gameOverTable.add(restartBtn).width(200).height(50).pad(10).row();
        gameOverTable.add(exitBtn).width(200).height(50).pad(10);
        stage.addActor(gameOverTable);
    }

    private void togglePause() {
        if (isVictory || isGameOverState) return;
        isPaused = !isPaused;
        menuTable.setVisible(isPaused);
        if (isPaused) Gdx.input.setInputProcessor(stage);
        else Gdx.input.setInputProcessor(null);
    }

    private Skin createBasicSkin() {
        Skin skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        skin.add("default", new BitmapFont());
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.up = skin.newDrawable("white", 0.3f, 0.3f, 0.3f, 0.9f);
        s.down = skin.newDrawable("white", 0.5f, 0.5f, 0.5f, 0.9f);
        s.over = skin.newDrawable("white", 0.4f, 0.4f, 0.4f, 0.9f);
        s.font = skin.getFont("default");
        skin.add("default", s);
        return skin;
    }

    private Generator findRepairTarget(Player player) {
        Generator closest = null;
        float bestDist = generatorRepairRange;
        for (Generator generator : levelManager.getGenerators()) {
            if (generator.isRepaired()) continue;
            float dist = player.getPosition().dst(generator.getPosition());
            if (dist <= bestDist) {
                bestDist = dist;
                closest = generator;
            }
        }
        return closest;
    }

    private void updateGenerators(float delta, Generator repairTarget) {
        for (Generator generator : levelManager.getGenerators()) {
            if (generator.isRepaired()) continue;
            if (generator == repairTarget) {
                float next = generator.getRepairProgress() + delta;
                if (next >= generatorRepairTime) {
                    generator.setRepaired(true);
                } else {
                    generator.setRepairProgress(next);
                }
            } else {
                generator.setRepairProgress(0f);
            }
        }

        if (!levelManager.isExitUnlocked() && levelManager.areAllGeneratorsRepaired()) {
            levelManager.setExitUnlocked(true);
        }
        if (levelManager.isExitUnlocked()) {
            clearEnemiesInSafeZone();
        }
    }

    private void clearEnemiesInSafeZone() {
        if (levelManager.getExitZone() == null) return;
        Rectangle bounds = levelManager.getExitZone().getBounds();
        for (Enemy enemy : levelManager.getEnemies()) {
            if (enemy.isActive() && bounds.contains(enemy.getPosition())) {
                enemy.setActive(false);
            }
        }
    }

    private void renderGeneratorPrompt() {
        if (isPaused || isVictory || isGameOverState || player.isDowned()) return;
        boolean isHoldingRepair = Gdx.input.isKeyPressed(Input.Keys.E);
        Generator target = findRepairTarget(player);
        if (target == null || target.isRepaired()) return;

        String text = isHoldingRepair ? "Repairing generator..." : "Hold E to repair";
        float textX = stage.getViewport().getWorldWidth() * 0.5f - 90f;
        float textY = 60f;

        overlayFont.getData().setScale(1.0f);
        game.batch.setProjectionMatrix(stage.getCamera().combined);
        game.batch.begin();
        overlayFont.setColor(1f, 1f, 1f, 1f);
        overlayFont.draw(game.batch, text, textX, textY);
        game.batch.end();

        float progress = Math.min(target.getRepairProgress() / generatorRepairTime, 1f);
        if (!isHoldingRepair && progress <= 0f) return;

        float barWidth = 160f;
        float barHeight = 6f;
        float barX = stage.getViewport().getWorldWidth() * 0.5f - barWidth * 0.5f;
        float barY = 42f;

        overlayRenderer.setProjectionMatrix(stage.getCamera().combined);
        overlayRenderer.begin(ShapeRenderer.ShapeType.Filled);
        overlayRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        overlayRenderer.rect(barX, barY, barWidth, barHeight);
        overlayRenderer.setColor(0.2f, 0.8f, 0.3f, 0.9f);
        overlayRenderer.rect(barX, barY, barWidth * progress, barHeight);
        overlayRenderer.end();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override
    public void show() {
        // TÄHTIS: Nullime sisendi protsessori, et MainMenu nupud enam ei töötaks.
        // Mängus kasutame me pollingut (Gdx.input.isKeyPressed), seega null on okei.
        Gdx.input.setInputProcessor(null);
    }
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
        player.dispose();
        overlayFont.dispose();
        overlayRenderer.dispose();

        // Renderdaja sulgemine on oluline!
        if (worldRenderer != null) worldRenderer.dispose();

        // Vaenlaste tekstuurid
        for (ee.eotv.echoes.entities.Enemy enemy : levelManager.getEnemies()) {
            enemy.dispose();
        }
    }
}


