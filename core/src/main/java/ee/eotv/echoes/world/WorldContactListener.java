package ee.eotv.echoes.world;

import com.badlogic.gdx.physics.box2d.*;

public class WorldContactListener implements ContactListener {

    public boolean isGameOver = false; // Seda muutujat loeb Main klass

    @Override
    public void beginContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        if (fixA.getBody().getUserData() == null || fixB.getBody().getUserData() == null) return;

        // Kontrollime: Kas kokku said "PLAYER" ja "ENEMY"?
        if (isCollision(fixA, fixB, "PLAYER", "ENEMY")) {
            System.out.println("HAMMUSTUS! Zombi sai su kätte. Mäng läbi.");
            isGameOver = true;
        }
    }

    private boolean isCollision(Fixture a, Fixture b, String type1, String type2) {
        String dataA = (String) a.getBody().getUserData();
        String dataB = (String) b.getBody().getUserData();
        return (dataA.equals(type1) && dataB.equals(type2)) || (dataA.equals(type2) && dataB.equals(type1));
    }

    @Override public void endContact(Contact contact) { }
    @Override public void preSolve(Contact contact, Manifold oldManifold) { }
    @Override public void postSolve(Contact contact, ContactImpulse impulse) { }
}
