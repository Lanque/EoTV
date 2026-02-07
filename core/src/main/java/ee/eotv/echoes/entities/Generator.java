package ee.eotv.echoes.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class Generator {
    private final Vector2 position;
    private boolean repaired = false;
    private float repairProgress = 0f;

    public Generator(float x, float y) {
        this.position = new Vector2(x, y);
    }

    public void render(ShapeRenderer shapeRenderer) {
        Color bodyColor = repaired ? new Color(0.2f, 0.9f, 0.3f, 1f) : new Color(0.7f, 0.2f, 0.2f, 1f);
        shapeRenderer.setColor(bodyColor);
        shapeRenderer.rect(position.x - 0.35f, position.y - 0.3f, 0.7f, 0.6f);
        shapeRenderer.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapeRenderer.rect(position.x - 0.25f, position.y + 0.1f, 0.5f, 0.15f);
    }

    public Vector2 getPosition() { return position; }

    public boolean isRepaired() { return repaired; }

    public void setRepaired(boolean repaired) {
        this.repaired = repaired;
        if (repaired) {
            repairProgress = 0f;
        }
    }

    public float getRepairProgress() { return repairProgress; }

    public void setRepairProgress(float progress) {
        repairProgress = Math.max(0f, progress);
    }
}
