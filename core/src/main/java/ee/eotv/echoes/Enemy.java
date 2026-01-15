package ee.eotv.echoes;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class Enemy {
    public Body body;
    private float speed = 3.0f;

    public Enemy(World world, float x, float y) {
        createBody(world, x, y);
    }

    private void createBody(World world, float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        bodyDef.fixedRotation = true;

        this.body = world.createBody(bodyDef);
        this.body.setUserData("ENEMY");

        CircleShape circle = new CircleShape();
        circle.setRadius(0.4f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 0.1f;

        this.body.createFixture(fixtureDef);
        circle.dispose();
    }

    // SEE ON SEE UUS JA OHUTU MEETOD
    // Me võtame vastu ainult numbreid (X ja Y), mitte "nööri" (Vector2)
    public void update(float playerX, float playerY) {
        Vector2 myPos = body.getPosition();

        float diffX = playerX - myPos.x;
        float diffY = playerY - myPos.y;

        Vector2 direction = new Vector2(diffX, diffY);
        direction.nor().scl(speed);

        body.setLinearVelocity(direction);
    }
}
