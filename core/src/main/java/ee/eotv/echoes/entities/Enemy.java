package ee.eotv.echoes.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class Enemy {
    public Body body;
    private float speed = 3.2f;
    private Vector2 targetPos; // Kuhu zombi parajasti liigub

    public Enemy(World world, float x, float y) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(x, y);
        def.fixedRotation = true;
        body = world.createBody(def);
        body.setUserData("ENEMY");

        CircleShape s = new CircleShape(); s.setRadius(0.35f);
        body.createFixture(s, 1.0f); s.dispose();
        targetPos = new Vector2(x, y); // Alguses seisab paigal
    }

    // Meetod, mida kutsutakse kivi maandumisel
    public void investigate(float x, float y) {
        targetPos.set(x, y);
    }

    public void update(Player player) {
        Vector2 myPos = body.getPosition();
        Vector2 playerPos = player.getPosition();
        float distToPlayer = myPos.dst(playerPos);

        // 1. NÄGEMINE: Kui taskulamp on sees
        if (player.isLightOn && distToPlayer < 25f) {
            targetPos.set(playerPos);
        }

        // 2. KUULMINE: Kui mängija joobseb ja on lähedal (25 meetrit)
        if (player.isRunning && player.isMoving && distToPlayer < 25f) {
            targetPos.set(playerPos);
        }

        // Liikumine targetPos suunas... (sama mis varem)
        if (myPos.dst(targetPos) > 0.5f) {
            Vector2 dir = new Vector2(targetPos.x - myPos.x, targetPos.y - myPos.y);
            body.setLinearVelocity(dir.nor().scl(speed));
        } else {
            body.setLinearVelocity(0, 0);
        }
    }

    public Vector2 getPosition() { return body.getPosition(); }
}
