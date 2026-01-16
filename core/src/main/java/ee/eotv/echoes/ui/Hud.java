package ee.eotv.echoes.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import ee.eotv.echoes.entities.Player; // Import

public class Hud implements Disposable {
    private ShapeRenderer shapeRenderer;

    public Hud() {
        shapeRenderer = new ShapeRenderer();
    }

    public void render(Player player, OrthographicCamera camera) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float x = camera.position.x - 18f;
        float y = camera.position.y + 13f;

        // Stamina taust
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(x, y, 10f, 0.8f);

        // Stamina riba
        if (player.stamina > 50) shapeRenderer.setColor(Color.GREEN);
        else if (player.stamina > 20) shapeRenderer.setColor(Color.YELLOW);
        else shapeRenderer.setColor(Color.RED);

        float width = (player.stamina / player.maxStamina) * 10f;
        shapeRenderer.rect(x, y, width, 0.8f);

        // --- UUS: KIVIDE KOGUS ---
        // Joonistame iga kivi kohta halli ringi
        shapeRenderer.setColor(Color.GRAY);
        for (int i = 0; i < player.ammo; i++) {
            shapeRenderer.circle(x + 0.5f + (i * 0.8f), y - 1.0f, 0.3f, 8);
        }

        // --- UUS: VÕTMEKAART ---
        if (player.hasKeycard) {
            shapeRenderer.setColor(Color.GOLD);
            shapeRenderer.rect(x + 5f, y - 1.3f, 0.6f, 0.4f);
        }

        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
