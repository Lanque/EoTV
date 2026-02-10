package ee.eotv.echoes.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import ee.eotv.echoes.entities.*;
import ee.eotv.echoes.managers.StoneManager;
import ee.eotv.echoes.world.LevelManager;

public class WorldRenderer {
    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final LevelManager levelManager;

    public WorldRenderer(SpriteBatch batch, LevelManager levelManager) {
        this.batch = batch;
        this.levelManager = levelManager;
        this.shapeRenderer = new ShapeRenderer();
    }

    public void render(OrthographicCamera camera, Player localPlayer, Player remotePlayer, StoneManager stoneManager, boolean showTrajectory) {
        // 1. Puhasta ekraan
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.position.set(localPlayer.getPosition(), 0);
        camera.update();

        // 2. Joonista staatiline maailm (Seinad ja põrand)
        drawStaticWorld(camera);

        // 3. Joonista interaktiivsed objektid (Uksed, Esemed, Generaatorid)
        drawInteractiveObjects(camera);

        // 4. Joonista tegelased (Mängijad ja Vaenlased)
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        localPlayer.render(batch);

        // --- UUS: KONTROLLIME, KAS TEINE MÄNGIJA ON OLEMAS ---
        if (remotePlayer != null) {
            remotePlayer.render(batch);
        }

        for (Enemy enemy : levelManager.getEnemies()) {
            enemy.render(batch);
        }

        batch.end();

        // 5. Joonista valgus (RayHandler)
        if (levelManager.isLightingEnabled()) {
            levelManager.rayHandler.setCombinedMatrix(camera.combined, camera.position.x, camera.position.y,
                camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom);
            levelManager.rayHandler.updateAndRender();
        }

        // 6. Joonista kivi visketrajektoor (kui on kivi roll)
        if (showTrajectory && stoneManager != null && localPlayer.canThrowStones()) {
            stoneManager.renderTrajectory(localPlayer, camera);
        }

        // 7. Joonista overlay (kui on downed)
        if (localPlayer.isDowned()) {
            renderDownedOverlay(camera);
        }
    }

    private void drawStaticWorld(OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        String[] layout = levelManager.getLevelLayout();
        float tileSize = 1.0f;

        for (int y = 0; y < layout.length; y++) {
            String row = layout[y];
            for (int x = 0; x < row.length(); x++) {
                float worldX = x * tileSize;
                float worldY = (layout.length - 1 - y) * tileSize;

                char tile = row.charAt(x);
                if (tile == '#') {
                    shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f); // Sein
                    shapeRenderer.rect(worldX, worldY, tileSize, tileSize);
                } else {
                    shapeRenderer.setColor(0.05f, 0.05f, 0.05f, 1f); // Põrand
                    shapeRenderer.rect(worldX, worldY, tileSize, tileSize);
                }
            }
        }

        // Exit Zone joonistamine (kui on avatud)
        if (levelManager.isExitUnlocked() && levelManager.getExitZone() != null) {
            levelManager.getExitZone().render(shapeRenderer);
        }

        shapeRenderer.end();
    }

    private void drawInteractiveObjects(OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Item item : levelManager.getItems()) item.render(shapeRenderer);
        for (Door door : levelManager.getDoors()) door.render(shapeRenderer);
        for (Generator generator : levelManager.getGenerators()) generator.render(shapeRenderer);

        shapeRenderer.end();
    }

    private void renderDownedOverlay(OrthographicCamera camera) {
        float viewWidth = camera.viewportWidth * camera.zoom;
        float viewHeight = camera.viewportHeight * camera.zoom;
        float startX = camera.position.x - viewWidth / 2f;
        float startY = camera.position.y - viewHeight / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.6f, 0.0f, 0.0f, 0.35f); // Poolläbipaistev punane
        shapeRenderer.rect(startX, startY, viewWidth, viewHeight);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
