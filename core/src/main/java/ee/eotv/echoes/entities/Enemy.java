package ee.eotv.echoes.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class Enemy {
    public Body body;
    private float speed = 3.2f;
    private Vector2 targetPos;

    // --- GRAAFIKA MUUTUJAD ---
    private Texture texture;
    private Animation<TextureRegion> walkAnimation;
    private TextureRegion idleFrame;
    private float stateTime;
    private boolean facingRight = true;

    // Suurus maailmas (meetrites)
    private float width = 1.0f;
    private float height = 1.0f;

    // Nihutamine (kui pilt ei ole täpselt kasti keskel)
    private float drawOffsetX = 0f;
    private float drawOffsetY = 0.1f; // Tõstame natuke üles

    public Enemy(World world, float x, float y) {
        // 1. FÜÜSIKA (Sama mis enne)
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(x, y);
        def.fixedRotation = true;
        body = world.createBody(def);
        body.setUserData("ENEMY");

        CircleShape s = new CircleShape();
        s.setRadius(0.35f);
        body.createFixture(s, 1.0f);
        s.dispose();

        targetPos = new Vector2(x, y);

        // 2. TEKSTUURID JA ANIMATSIOON (Täpselt nagu Player klassis)
        texture = new Texture(Gdx.files.internal("images/characters.png"));

        // --- SIIN SAAD PIKSLEID MUUTA ---
        int frameWidth = 20;  // Ühe kaadri laius
        int frameHeight = 35; // Ühe kaadri kõrgus

        // ORANŽ TEGELANE on 1. reas (kõige üleval).
        // Playeril oli startY = 35. Enemy jaoks paneme väiksema numbri.
        int startX = 9;
        int startY = 3;  // <--- MUUDA SEDA: 3 peaks olema ülemine rida
        int padding = 12; // Vahe piltide vahel

        TextureRegion[] walkFrames = new TextureRegion[4];

        for (int i = 0; i < 4; i++) {
            walkFrames[i] = new TextureRegion(
                texture,
                startX + (i * (frameWidth + padding)),
                startY,
                frameWidth,
                frameHeight
            );
        }

        walkAnimation = new Animation<>(0.15f, walkFrames);
        // Seismise pilt on esimene kaader
        idleFrame = new TextureRegion(texture, startX, startY, frameWidth, frameHeight);

        stateTime = 0f;
    }

    public void investigate(float x, float y) {
        targetPos.set(x, y);
    }

    public void update(Player player) {
        Vector2 myPos = body.getPosition();
        Vector2 playerPos = player.getPosition();
        float distToPlayer = myPos.dst(playerPos);

        // Lihtsustatud loogika
        if (player.isLightOn && distToPlayer < 12f) targetPos.set(playerPos);
        if (player.isRunning && player.isMoving && distToPlayer < 15f) targetPos.set(playerPos);

        if (myPos.dst(targetPos) > 0.5f) {
            Vector2 dir = new Vector2(targetPos.x - myPos.x, targetPos.y - myPos.y);
            body.setLinearVelocity(dir.nor().scl(speed));

            // Suuna määramine
            if (dir.x > 0) facingRight = true;
            if (dir.x < 0) facingRight = false;
        } else {
            body.setLinearVelocity(0, 0);
        }
    }

    // --- JOONISTAMINE ---
    public void render(SpriteBatch batch) {
        stateTime += Gdx.graphics.getDeltaTime();

        TextureRegion currentFrame;

        // Kui liigub, kasuta animatsiooni, muidu idleFrame
        if (body.getLinearVelocity().len() > 0.1f) {
            currentFrame = walkAnimation.getKeyFrame(stateTime, true);
        } else {
            currentFrame = idleFrame;
        }

        // Pööramine (Flip)
        if (!facingRight && !currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        } else if (facingRight && currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        }

        // Joonistame
        float drawX = body.getPosition().x - (width / 2) + drawOffsetX;
        float drawY = body.getPosition().y - (height / 2) + drawOffsetY;

        batch.draw(currentFrame, drawX, drawY, width, height);
    }

    public Vector2 getPosition() { return body.getPosition(); }

    public void dispose() {
        if (texture != null) texture.dispose();
    }
}
