package ee.eotv.echoes.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.TimeUtils;
import ee.eotv.echoes.Main;
import ee.eotv.echoes.entities.Door;
import ee.eotv.echoes.entities.Generator;
import ee.eotv.echoes.entities.Item;
import ee.eotv.echoes.entities.Player;
import ee.eotv.echoes.managers.SoundManager;
import ee.eotv.echoes.managers.StoneManager;
import ee.eotv.echoes.net.NetMessages;
import ee.eotv.echoes.net.NetworkClient;
import ee.eotv.echoes.ui.Hud;
import ee.eotv.echoes.ui.WorldRenderer; // UUS IMPORT
import ee.eotv.echoes.world.LevelManager;

import java.util.Queue;

public class MultiplayerGameScreen implements Screen {
    private final Main game;
    private final NetworkClient client;
    private final Player.Role localRole;
    private final int localPlayerId;
    private final int remotePlayerId;

    private LevelManager levelManager;
    private WorldRenderer worldRenderer; // UUS RENDERDAJA
    private Player localPlayer;
    private Player remotePlayer;
    private Player hostPlayer;
    private Player clientPlayer;

    private SoundManager soundManager;
    private StoneManager trajectoryManager;
    private Hud hud;
    private OrthographicCamera camera;
    private ShapeRenderer overlayRenderer;

    private Stage stage;
    private Skin skin;
    private Table pauseTable;
    private Table gameOverTable;
    private Table victoryTable;
    private boolean isPaused = false;
    private BitmapFont overlayFont;

    private static final float REVIVE_TIME_REQUIRED = 2.5f;
    private static final float GENERATOR_REPAIR_TIME = 10f;
    private static final float INTERP_DELAY_SEC = 0.1f;

    private float reviveRange = 1.8f;
    private float reviveDisplayTimer = 0f;

    private float generatorRepairRange = 1.2f;

    private float currentPower = 0f;
    private float maxPower = 1.2f;
    private boolean isCharging = false;

    private boolean isGameOver = false;
    private boolean isVictory = false;
    private boolean debugDisableLighting = false;

    private NetMessages.WorldState previousState;
    private NetMessages.WorldState currentState;
    private long previousStateTimeMs;
    private long currentStateTimeMs;
    private boolean pendingLocalToggle = false;
    private float lastLocalAimAngle = 0f;
    public MultiplayerGameScreen(Main game, NetworkClient client, NetMessages.StartGame startGame) {
        this.game = game;
        this.client = client;
        this.localRole = Player.fromNetRole(startGame.role);
        this.localPlayerId = startGame.playerId;
        this.remotePlayerId = startGame.playerId == 1 ? 2 : 1;
        init();
    }

    private void init() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 40, 30);

        levelManager = new LevelManager();
        // Loome uue Renderdaja
        worldRenderer = new WorldRenderer(game.batch, levelManager);

        soundManager = new SoundManager();
        trajectoryManager = new StoneManager(null, null, null);
        trajectoryManager.maxPower = maxPower;
        hud = new Hud();
        overlayRenderer = new ShapeRenderer();
        stage = new Stage(new ScreenViewport());
        skin = createBasicSkin();
        createPauseMenu();
        createVictoryMenu();
        overlayFont = new BitmapFont();

        Vector2 hostStart = new Vector2(2, 25);
        Vector2 clientStart = new Vector2(4, 25);

        Player.Role hostRole = (localPlayerId == 1) ? localRole : oppositeRole(localRole);
        Player.Role clientRole = (localPlayerId == 2) ? localRole : oppositeRole(localRole);

        hostPlayer = new Player(levelManager.world, levelManager.rayHandler, hostStart.x, hostStart.y);
        clientPlayer = new Player(levelManager.world, levelManager.rayHandler, clientStart.x, clientStart.y);
        hostPlayer.setRole(hostRole);
        clientPlayer.setRole(clientRole);

        if (localPlayerId == 1) {
            localPlayer = hostPlayer;
            remotePlayer = clientPlayer;
        } else {
            localPlayer = clientPlayer;
            remotePlayer = hostPlayer;
        }
    }

    @Override
    public void render(float delta) {
        // --- 1. GAME LOOP LOOGIKA ---
        if (!isGameOver && !isVictory && !isPaused) {
            Gdx.input.setInputProcessor(null);
            updateClient(delta);
        }

        // --- 2. JÄRGMINE RIDA ON NÜÜD KOGU JOONISTAMINE ---
        boolean showTrajectory = localRole == Player.Role.STONES;
        if (showTrajectory) {
            trajectoryManager.currentPower = currentPower;
            trajectoryManager.isCharging = isCharging;
        } else {
            trajectoryManager.isCharging = false;
            trajectoryManager.currentPower = 0f;
        }
        worldRenderer.render(camera, localPlayer, remotePlayer, trajectoryManager, showTrajectory);

        // --- 3. UI JA MENÜÜD ---
        hud.render(localPlayer, camera);
        renderRevivePrompt();
        renderGeneratorPrompt();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            debugDisableLighting = !debugDisableLighting;
            levelManager.setLightingEnabled(!debugDisableLighting);
        }

        if (isPaused) {
            renderPauseMenu(delta);
        } else if (isGameOver) {
            renderGameOverMenu(delta);
        } else if (isVictory) {
            renderVictoryMenu(delta);
        }
    }

    // --- LOGIC METHODS ---

    private void updateClient(float delta) {
        if (client != null && client.isDisconnected()) {
            game.setScreen(new MainMenuScreen(game));
            return;
        }
        handleClientInput();
        applyWorldState();
        applyLocalVisuals();
        levelManager.updateClient(delta);
        handleEchoEvents();
        handleSoundEvents();
        updateReviveDisplayTimer(delta, Gdx.input.isKeyPressed(Input.Keys.E));
    }

    private void handleClientInput() {
        if (client == null) return;

        NetMessages.InputState input = new NetMessages.InputState();
        input.playerId = localPlayerId;
        input.up = Gdx.input.isKeyPressed(Input.Keys.W);
        input.down = Gdx.input.isKeyPressed(Input.Keys.S);
        input.left = Gdx.input.isKeyPressed(Input.Keys.A);
        input.right = Gdx.input.isKeyPressed(Input.Keys.D);
        input.run = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
        input.toggleLight = Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.F);
        input.revive = Gdx.input.isKeyPressed(Input.Keys.E);

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        float angleRad = (float) Math.atan2(mouse.y - localPlayer.getPosition().y, mouse.x - localPlayer.getPosition().x);
        input.aimAngle = (float) Math.toDegrees(angleRad);
        lastLocalAimAngle = input.aimAngle;
        client.sendInput(input);

        boolean isHoldingRepair = input.revive;
        boolean isReviving = isAttemptingRevive(localPlayer, remotePlayer, isHoldingRepair);
        boolean isRepairing = !isReviving && !localPlayer.isDowned() && isHoldingRepair && findRepairTarget(localPlayer) != null;

        if (input.toggleLight) {
            pendingLocalToggle = true;
        }

        if (localRole == Player.Role.STONES && !isRepairing) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && localPlayer.ammo > 0) {
                isCharging = true;
                currentPower = Math.min(currentPower + 1.5f * Gdx.graphics.getDeltaTime(), maxPower);
            } else if (isCharging) {
                NetMessages.ThrowStone throwStone = new NetMessages.ThrowStone();
                throwStone.playerId = localPlayerId;
                throwStone.targetX = mouse.x;
                throwStone.targetY = mouse.y;
                throwStone.power = currentPower;
                client.sendThrow(throwStone);
                currentPower = 0f;
                isCharging = false;
            }
        } else if (isRepairing) {
            currentPower = 0f;
            isCharging = false;
        }
    }

    private void applyWorldState() {
        NetMessages.WorldState latest = client.getLatestWorldState();
        if (latest != null && (currentState == null || latest.tick != currentState.tick)) {
            previousState = currentState;
            previousStateTimeMs = currentStateTimeMs;
            currentState = latest;
            currentStateTimeMs = TimeUtils.millis();
        }

        if (currentState == null) return;

        float alpha = 1f;
        if (previousState != null) {
            long renderTime = TimeUtils.millis() - (long) (INTERP_DELAY_SEC * 1000f);
            long frameSpan = Math.max(1L, currentStateTimeMs - previousStateTimeMs);
            alpha = (renderTime - previousStateTimeMs) / (float) frameSpan;
            if (alpha < 0f) alpha = 0f;
            if (alpha > 1f) alpha = 1f;
        }

        applyInterpolatedPlayers(alpha);
        applyInterpolatedEnemies(alpha);

        levelManager.getItems().clear();
        if (currentState.items != null) {
            for (NetMessages.ItemState is : currentState.items) {
                if (is != null) {
                    levelManager.spawnItem(Item.fromNetType(is.type), is.x, is.y);
                }
            }
        }

        if (currentState.doors != null) {
            for (NetMessages.DoorState ds : currentState.doors) {
                for (Door door : levelManager.getDoors()) {
                    if (Math.abs(door.getPosition().x - ds.x) < 0.01f &&
                        Math.abs(door.getPosition().y - ds.y) < 0.01f) {
                        if (ds.open && !door.isOpen()) door.open();
                    }
                }
            }
        }

        if (currentState.generators != null) {
            for (NetMessages.GeneratorState gs : currentState.generators) {
                if (gs == null || gs.index < 0 || gs.index >= levelManager.getGenerators().size()) continue;
                Generator generator = levelManager.getGenerators().get(gs.index);
                generator.setRepairProgress(gs.progress);
                generator.setRepaired(gs.repaired);
            }
        }

        if (currentState.exitUnlocked != levelManager.isExitUnlocked()) {
            levelManager.setExitUnlocked(currentState.exitUnlocked);
        }

        isGameOver = currentState.gameOver;
        isVictory = currentState.victory;
    }

    private void applyLocalVisuals() {
        if (localPlayer == null) return;
        localPlayer.setAimAngle(lastLocalAimAngle);
        if (pendingLocalToggle && !localPlayer.isDowned() && localPlayer.canUseFlashlight()) {
            localPlayer.setLightOn(!localPlayer.isLightOn);
        }
        pendingLocalToggle = false;
    }

    private void applyInterpolatedPlayers(float alpha) {
        NetMessages.PlayerState currentLocal = findPlayerState(currentState, localPlayerId);
        NetMessages.PlayerState currentRemote = findPlayerState(currentState, remotePlayerId);
        NetMessages.PlayerState previousLocal = previousState == null ? null : findPlayerState(previousState, localPlayerId);
        NetMessages.PlayerState previousRemote = previousState == null ? null : findPlayerState(previousState, remotePlayerId);

        // Local player uses latest state to avoid visible input lag (flashlight/position).
        applyPlayerState(localPlayer, currentLocal, null, 1f);
        applyPlayerState(remotePlayer, currentRemote, previousRemote, alpha);
    }

    private void applyPlayerState(Player target, NetMessages.PlayerState current, NetMessages.PlayerState previous, float alpha) {
        if (current == null || target == null) return;
        float x = current.x;
        float y = current.y;
        float vx = current.vx;
        float vy = current.vy;
        if (previous != null) {
            x = lerp(previous.x, current.x, alpha);
            y = lerp(previous.y, current.y, alpha);
            vx = lerp(previous.vx, current.vx, alpha);
            vy = lerp(previous.vy, current.vy, alpha);
        }
        Player.Role roleValue = current.role == null ? null : Player.fromNetRole(current.role);
        target.setNetworkState(x, y, vx, vy, current.lightOn, current.running, current.moving,
            current.stamina, current.ammo, current.hasKeycard, current.aimAngle, roleValue, current.downed);
    }

    private void applyInterpolatedEnemies(float alpha) {
        if (currentState.enemies == null) return;
        for (int i = 0; i < currentState.enemies.length; i++) {
            NetMessages.EnemyState currentEnemy = currentState.enemies[i];
            if (currentEnemy == null || currentEnemy.index < 0 || currentEnemy.index >= levelManager.getEnemies().size()) continue;
            NetMessages.EnemyState previousEnemy = null;
            if (previousState != null && previousState.enemies != null && i < previousState.enemies.length) {
                previousEnemy = previousState.enemies[i];
            }
            float x = currentEnemy.x;
            float y = currentEnemy.y;
            float vx = currentEnemy.vx;
            float vy = currentEnemy.vy;
            if (previousEnemy != null) {
                x = lerp(previousEnemy.x, currentEnemy.x, alpha);
                y = lerp(previousEnemy.y, currentEnemy.y, alpha);
                vx = lerp(previousEnemy.vx, currentEnemy.vx, alpha);
                vy = lerp(previousEnemy.vy, currentEnemy.vy, alpha);
            }
            levelManager.getEnemies().get(currentEnemy.index)
                .setNetworkState(x, y, vx, vy, currentEnemy.facingRight, currentEnemy.active);
        }
    }

    private NetMessages.PlayerState findPlayerState(NetMessages.WorldState state, int playerId) {
        if (state == null || state.players == null) return null;
        for (NetMessages.PlayerState ps : state.players) {
            if (ps != null && ps.id == playerId) return ps;
        }
        return null;
    }

    private float lerp(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }

    private void handleEchoEvents() {
        Queue<NetMessages.EchoEvent> echoes = client.getEchoEvents();
        NetMessages.EchoEvent event;
        while ((event = echoes.poll()) != null) {
            levelManager.addEcho(event.x, event.y, event.radius, new Color(event.r, event.g, event.b, event.a));
        }
    }

    private void handleSoundEvents() {
        Queue<NetMessages.SoundEvent> sounds = client.getSoundEvents();
        NetMessages.SoundEvent event;
        while ((event = sounds.poll()) != null) {
            playSoundWithDistance(event.type, event.x, event.y);
        }
    }

    private Player.Role oppositeRole(Player.Role role) {
        return role == Player.Role.FLASHLIGHT ? Player.Role.STONES : Player.Role.FLASHLIGHT;
    }

    private void renderRevivePrompt() {
        boolean canRevive = !localPlayer.isDowned()
            && remotePlayer.isDowned()
            && localPlayer.getPosition().dst(remotePlayer.getPosition()) <= reviveRange;

        if (!canRevive) return;

        String text = "Hold E to revive";
        float textX = stage.getViewport().getWorldWidth() * 0.5f - 70f;
        float textY = 40f;

        overlayFont.getData().setScale(1.0f);
        game.batch.setProjectionMatrix(stage.getCamera().combined);
        game.batch.begin();
        overlayFont.setColor(1f, 1f, 1f, 1f);
        overlayFont.draw(game.batch, text, textX, textY);
        game.batch.end();

        float progress = Math.min(reviveDisplayTimer / REVIVE_TIME_REQUIRED, 1f);
        float barWidth = 140f;
        float barHeight = 6f;
        float barX = stage.getViewport().getWorldWidth() * 0.5f - barWidth * 0.5f;
        float barY = 22f;

        overlayRenderer.setProjectionMatrix(stage.getCamera().combined);
        overlayRenderer.begin(ShapeRenderer.ShapeType.Filled);
        overlayRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
        overlayRenderer.rect(barX, barY, barWidth, barHeight);
        overlayRenderer.setColor(0.8f, 0.2f, 0.2f, 0.9f);
        overlayRenderer.rect(barX, barY, barWidth * progress, barHeight);
        overlayRenderer.end();
    }

    private void renderGeneratorPrompt() {
        if (localPlayer.isDowned()) return;
        boolean isHoldingRepair = Gdx.input.isKeyPressed(Input.Keys.E);
        boolean isReviving = isAttemptingRevive(localPlayer, remotePlayer, isHoldingRepair);
        if (isReviving) return;

        Generator target = findRepairTarget(localPlayer);
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

        float progress = Math.min(target.getRepairProgress() / GENERATOR_REPAIR_TIME, 1f);
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

    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            Gdx.input.setInputProcessor(stage);
        } else {
            Gdx.input.setInputProcessor(null);
        }
    }

    private void renderPauseMenu(float delta) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        pauseTable.setVisible(true);
        gameOverTable.setVisible(false);
        stage.act(delta);
        stage.draw();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void createPauseMenu() {
        pauseTable = new Table();
        pauseTable.setFillParent(true);

        TextButton resumeBtn = new TextButton("RESUME", skin);
        resumeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                togglePause();
            }
        });

        TextButton exitBtn = new TextButton("EXIT TO MENU", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (client != null) client.stop();
                game.setScreen(new MainMenuScreen(game));
            }
        });

        pauseTable.add(resumeBtn).width(200).height(50).pad(10).row();
        pauseTable.add(exitBtn).width(200).height(50).pad(10);
        stage.addActor(pauseTable);

        gameOverTable = new Table();
        gameOverTable.setFillParent(true);

        TextButton restartBtn = new TextButton("RESTART", skin);
        restartBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (client != null) client.stop();
                game.setScreen(new MultiplayerMenuScreen(game));
            }
        });

        TextButton gameOverExitBtn = new TextButton("EXIT TO MENU", skin);
        gameOverExitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (client != null) client.stop();
                game.setScreen(new MainMenuScreen(game));
            }
        });

        com.badlogic.gdx.scenes.scene2d.ui.Label gameOverLabel =
            new com.badlogic.gdx.scenes.scene2d.ui.Label("GAME OVER", skin);

        gameOverTable.add(gameOverLabel).padBottom(30).row();
        gameOverTable.add(restartBtn).width(200).height(50).pad(10).row();
        gameOverTable.add(gameOverExitBtn).width(200).height(50).pad(10);
        gameOverTable.setVisible(false);
        stage.addActor(gameOverTable);
    }

    private void createVictoryMenu() {
        victoryTable = new Table();
        victoryTable.setFillParent(true);

        TextButton restartBtn = new TextButton("RESTART", skin);
        restartBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (client != null) client.stop();
                game.setScreen(new MultiplayerMenuScreen(game));
            }
        });

        TextButton exitBtn = new TextButton("EXIT TO MENU", skin);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (client != null) client.stop();
                game.setScreen(new MainMenuScreen(game));
            }
        });

        com.badlogic.gdx.scenes.scene2d.ui.Label victoryLabel =
            new com.badlogic.gdx.scenes.scene2d.ui.Label("VICTORY", skin);

        victoryTable.add(victoryLabel).padBottom(30).row();
        victoryTable.add(restartBtn).width(200).height(50).pad(10).row();
        victoryTable.add(exitBtn).width(200).height(50).pad(10);
        victoryTable.setVisible(false);
        stage.addActor(victoryTable);
    }

    private void renderGameOverMenu(float delta) {
        gameOverTable.setVisible(true);
        pauseTable.setVisible(false);
        if (victoryTable != null) victoryTable.setVisible(false);
        Gdx.input.setInputProcessor(stage);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        stage.act(delta);
        stage.draw();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderVictoryMenu(float delta) {
        if (victoryTable == null) return;
        victoryTable.setVisible(true);
        pauseTable.setVisible(false);
        gameOverTable.setVisible(false);
        Gdx.input.setInputProcessor(stage);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        stage.act(delta);
        stage.draw();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private Skin createBasicSkin() {
        Skin skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        skin.add("default", new BitmapFont());

        skin.add("default", new com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(skin.getFont("default"), Color.WHITE));

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up = skin.newDrawable("white", 0.2f, 0.2f, 0.2f, 0.9f);
        style.down = skin.newDrawable("white", 0.4f, 0.4f, 0.4f, 0.9f);
        style.over = skin.newDrawable("white", 0.3f, 0.3f, 0.3f, 0.9f);
        style.font = skin.getFont("default");
        skin.add("default", style);
        return skin;
    }

    private void updateReviveDisplayTimer(float delta, boolean isHoldingRevive) {
        boolean canRevive = !localPlayer.isDowned()
            && remotePlayer.isDowned()
            && localPlayer.getPosition().dst(remotePlayer.getPosition()) <= reviveRange;
        if (canRevive && isHoldingRevive) {
            reviveDisplayTimer = Math.min(reviveDisplayTimer + delta, REVIVE_TIME_REQUIRED);
        } else {
            reviveDisplayTimer = 0f;
        }
    }

    private boolean isAttemptingRevive(Player helper, Player target, boolean isHolding) {
        return isHolding
            && !helper.isDowned()
            && target.isDowned()
            && helper.getPosition().dst(target.getPosition()) <= reviveRange;
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

    private void playSoundWithDistance(NetMessages.SoundType type, float x, float y) {
        float dist = localPlayer.getPosition().dst(x, y);
        float maxDist = getMaxDistance(type);
        if (dist > maxDist) return;
        float volume = Math.max(0.05f, 1f - (dist / maxDist));

        switch (type) {
            case STEP:
                soundManager.playStep(volume * 0.4f);
                break;
            case THROW:
                soundManager.playThrow(volume * 0.9f);
                break;
            case HIT:
                soundManager.playHit(volume * 0.8f);
                break;
            case COLLECT:
                soundManager.playCollect(volume * 0.7f);
                break;
            case DOOR:
                soundManager.playDoor(volume);
                break;
            case LIGHT_ON:
                soundManager.playLightOn(volume * 0.7f);
                break;
            case LIGHT_OFF:
                soundManager.playLightOff(volume * 0.7f);
                break;
        }
    }

    private float getMaxDistance(NetMessages.SoundType type) {
        switch (type) {
            case STEP:
                return 10f;
            case LIGHT_ON:
            case LIGHT_OFF:
                return 12f;
            case COLLECT:
                return 9f;
            case DOOR:
                return 16f;
            case THROW:
                return 18f;
            case HIT:
                return 20f;
            default:
                return 10f;
        }
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }
    @Override public void show() { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (overlayRenderer != null) overlayRenderer.dispose();
        levelManager.dispose();
        hud.dispose();
        soundManager.dispose();
        localPlayer.dispose();
        remotePlayer.dispose();
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (overlayFont != null) overlayFont.dispose();
        if (client != null) client.stop();
        if (worldRenderer != null) worldRenderer.dispose(); // UUS DISPOSE
    }
}
