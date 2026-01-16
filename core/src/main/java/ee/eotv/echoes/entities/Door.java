package ee.eotv.echoes.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class Door {
    private Body body;
    private Vector2 position;
    private boolean isOpen = false;
    private float width = 1.0f;
    private float height = 1.0f;

    public Door(World world, float x, float y) {
        this.position = new Vector2(x, y);

        // Teeme ukse füüsikalise keha (nagu sein)
        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.StaticBody;
        bdef.position.set(x + width / 2, y + height / 2); // Box2D tahab keskpunkti

        body = world.createBody(bdef);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width / 2, height / 2);

        body.createFixture(shape, 0.0f);
        body.setUserData("DOOR");
        shape.dispose();
    }

    public void render(ShapeRenderer shapeRenderer) {
        if (isOpen) return; // Kui uks on lahti, siis me ei joonista seda

        // Joonistame pruuni ukse
        shapeRenderer.setColor(new Color(0.6f, 0.3f, 0.0f, 1f));
        shapeRenderer.rect(position.x, position.y, width, height);

        // Joonistame lukuaugu/käepideme
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.circle(position.x + 0.8f, position.y + 0.5f, 0.1f, 8);
    }

    public void open() {
        if (!isOpen) {
            isOpen = true;
            // See on Box2D trikk: me ei kustuta keha, vaid teeme selle "mitte-aktiivseks"
            // Nii saab mängija sellest läbi kõndida.
            body.setActive(false);
        }
    }

    public Vector2 getPosition() { return position; }
    public boolean isOpen() { return isOpen; }

    // See tagastab ukse füüsilise keskpunkti
    public Vector2 getCenter() {
        return new Vector2(position.x + width / 2, position.y + height / 2);
    }
}
