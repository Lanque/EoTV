package ee.eotv.echoes.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class ExitZone {
    private Rectangle bounds;

    public ExitZone(float x, float y, float width, float height) {
        this.bounds = new Rectangle(x, y, width, height);
    }

    public void render(ShapeRenderer shapeRenderer) {
        // Lülitame sisse läbipaistvuse, et roheline ala oleks "kummituslik"
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setColor(new Color(0f, 1f, 0f, 0.3f)); // Läbipaistev roheline
        shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public Rectangle getBounds() {
        return bounds;
    }
}
