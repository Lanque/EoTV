package ee.eotv.echoes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import box2dLight.ConeLight;
import box2dLight.RayHandler;

public class Player {
    private Body body;
    private ConeLight flashlight;
    private float speed = 5.0f;
    public boolean isLightOn = true; // Taskulambi olek

    public Player(World world, RayHandler rayHandler, float x, float y) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(x, y);
        def.fixedRotation = true;
        body = world.createBody(def);
        body.setUserData("PLAYER");

        CircleShape shape = new CircleShape();
        shape.setRadius(0.4f);
        body.createFixture(shape, 1.0f);
        shape.dispose();

        // VÕIMSAM TASKULAMP: Kaugus 45, kiirte arv 200 (teravam), vihk 35 kraadi
        flashlight = new ConeLight(rayHandler, 200, new Color(1f, 1f, 0.9f, 1f), 45f, x, y, 0, 35f);
        flashlight.attachToBody(body);
        flashlight.setSoft(true);
    }

    public void handleInput(Camera camera) {
        // Taskulambi lülitamine (Tühik või F)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            isLightOn = !isLightOn;
            flashlight.setActive(isLightOn);
        }

        float vx = 0, vy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) vy = speed;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) vy = -speed;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) vx = -speed;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) vx = speed;
        body.setLinearVelocity(vx, vy);

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse);
        float angle = MathUtils.atan2(mouse.y - body.getPosition().y, mouse.x - body.getPosition().x);
        body.setTransform(body.getPosition(), angle);
    }

    public Vector2 getPosition() { return body.getPosition(); }
}
