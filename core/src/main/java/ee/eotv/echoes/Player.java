package ee.eotv.echoes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import box2dLight.RayHandler;
import box2dLight.ConeLight;
import com.badlogic.gdx.graphics.Color;

public class Player {
    private Body body;
    private ConeLight flashlight;
    private float speed = 5.0f;

    public Player(World world, RayHandler rayHandler, float x, float y) {
        // 1. Keha
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(x, y);
        def.fixedRotation = true;
        body = world.createBody(def);
        body.setUserData("PLAYER"); // Silt kokkupõrgete jaoks

        CircleShape shape = new CircleShape();
        shape.setRadius(0.5f);
        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 1.0f;
        body.createFixture(fdef);
        shape.dispose();

        // 2. Taskulamp
        flashlight = new ConeLight(rayHandler, 100, Color.CORAL, 30, x, y, 0, 30);
        flashlight.attachToBody(body);
    }

    public void handleInput(Camera camera) {
        // Liikumine (WASD)
        float velX = 0;
        float velY = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) velY = speed;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) velY = -speed;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) velX = -speed;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) velX = speed;

        body.setLinearVelocity(velX, velY);

        // Pööramine hiire suunas
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        float angle = MathUtils.atan2(mousePos.y - body.getPosition().y, mousePos.x - body.getPosition().x);
        // Box2D tahab radiaane, ConeLight hoolitseb ise
        body.setTransform(body.getPosition(), angle);
    }

    public Vector2 getPosition() {
        return body.getPosition();
    }

    public Body getBody() {
        return body;
    }
}
