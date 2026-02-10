package ee.eotv.echoes.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import ee.eotv.echoes.net.NetMessages;

public class Item {
    public enum Type {
        STONE,      // Visatav kivi
        KEYCARD     // Uksekaart
    }

    public static NetMessages.ItemType toNetType(Type type) {
        if (type == null) return NetMessages.ItemType.STONE;
        return type == Type.KEYCARD ? NetMessages.ItemType.KEYCARD : NetMessages.ItemType.STONE;
    }

    public static Type fromNetType(NetMessages.ItemType type) {
        if (type == null) return Type.STONE;
        return type == NetMessages.ItemType.KEYCARD ? Type.KEYCARD : Type.STONE;
    }

    private Vector2 position;
    private Type type;
    private boolean active = true; // Kas ese on maas või juba korjatud

    public Item(Type type, float x, float y) {
        this.type = type;
        this.position = new Vector2(x, y);
    }

    public void render(ShapeRenderer shapeRenderer) {
        if (!active) return;

        if (type == Type.STONE) {
            // Kivi on väike hall ring
            shapeRenderer.setColor(Color.GRAY);
            shapeRenderer.circle(position.x, position.y, 0.15f, 8);
        } else if (type == Type.KEYCARD) {
            // Võtmekaart on väike kuldne ristkülik
            shapeRenderer.setColor(Color.GOLD);
            shapeRenderer.rect(position.x - 0.15f, position.y - 0.1f, 0.3f, 0.2f);
        }
    }

    public Vector2 getPosition() { return position; }
    public Type getType() { return type; }
    public boolean isActive() { return active; }
    public void collect() { this.active = false; }
}
