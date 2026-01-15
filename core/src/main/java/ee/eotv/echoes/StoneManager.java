package ee.eotv.echoes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import java.util.ArrayList;
import java.util.Iterator;

public class StoneManager {
    // Sisemine abiklass
    private class Stone {
        Body body;
        float timeAlive = 0;
        public Stone(Body b) { this.body = b; }
    }

    private World world;
    private ArrayList<Stone> stones = new ArrayList<>();

    // Laadimine
    private float currentPower = 0f;
    private float maxPower = 1.2f;
    private boolean isCharging = false;

    public StoneManager(World world) {
        this.world = world;
    }

    public void handleInput(Player player, Camera camera) {
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            isCharging = true;
            currentPower += 1.5f * Gdx.graphics.getDeltaTime();
            if (currentPower > maxPower) currentPower = maxPower;
        } else if (isCharging) {
            throwStone(player, camera, currentPower);
            currentPower = 0;
            isCharging = false;
        }
    }

    private void throwStone(Player player, Camera camera, float power) {
        Vector2 startPos = player.getPosition();
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        Vector2 dir = new Vector2(mousePos.x - startPos.x, mousePos.y - startPos.y).nor();
        Vector2 spawnPos = new Vector2(startPos).add(dir.x * 0.6f, dir.y * 0.6f);

        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(spawnPos);
        def.bullet = true;

        Body body = world.createBody(def);
        body.setUserData("STONE");

        CircleShape shape = new CircleShape();
        shape.setRadius(0.1f);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 2.0f;
        fdef.restitution = 0.5f; // Põrkab natuke rohkem
        body.createFixture(fdef);
        shape.dispose();

        body.applyLinearImpulse(dir.scl(power), body.getWorldCenter(), true);
        stones.add(new Stone(body));
    }

    public void update(float delta) {
        // Kasutame iteraatorit, et saaks tsükli sees kustutada
        Iterator<Stone> iter = stones.iterator();
        while (iter.hasNext()) {
            Stone s = iter.next();
            s.timeAlive += delta;

            // Kui kivi on liiga kaua lennanud (0.6s), pidurdame
            if (s.timeAlive > 0.6f) {
                s.body.setLinearDamping(5.0f);
            }
            // Kui kivi on väga kaua maas olnud (5s), kustutame ära
            if (s.timeAlive > 5.0f) {
                world.destroyBody(s.body);
                iter.remove();
            }
        }
    }
}
