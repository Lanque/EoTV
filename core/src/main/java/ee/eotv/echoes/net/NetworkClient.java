package ee.eotv.echoes.net;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import ee.eotv.echoes.entities.Player;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NetworkClient {
    private final Client client = new Client();
    private volatile NetMessages.JoinResponse joinResponse;
    private volatile NetMessages.LobbyJoinedResponse lobbyResponse;
    private volatile NetMessages.StartGame startGame;
    private volatile NetMessages.WorldState latestWorldState;
    private final Queue<NetMessages.EchoEvent> echoEvents = new ConcurrentLinkedQueue<>();
    private final Queue<NetMessages.SoundEvent> soundEvents = new ConcurrentLinkedQueue<>();
    private volatile boolean disconnected = false;

    public void connect(String host) throws IOException {
        Network.register(client.getKryo());
        client.start();
        client.connect(5000, host, Network.TCP_PORT, Network.UDP_PORT);
        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof NetMessages.JoinResponse) {
                    joinResponse = (NetMessages.JoinResponse) object;
                } else if (object instanceof NetMessages.LobbyJoinedResponse) {
                    lobbyResponse = (NetMessages.LobbyJoinedResponse) object;
                } else if (object instanceof NetMessages.StartGame) {
                    startGame = (NetMessages.StartGame) object;
                } else if (object instanceof NetMessages.WorldState) {
                    latestWorldState = (NetMessages.WorldState) object;
                } else if (object instanceof NetMessages.EchoEvent) {
                    echoEvents.add((NetMessages.EchoEvent) object);
                } else if (object instanceof NetMessages.SoundEvent) {
                    soundEvents.add((NetMessages.SoundEvent) object);
                }
            }
            @Override
            public void disconnected(Connection connection) {
                disconnected = true;
            }
        });
    }

    public NetMessages.JoinResponse getJoinResponse() {
        return joinResponse;
    }

    public NetMessages.LobbyJoinedResponse getLobbyResponse() {
        return lobbyResponse;
    }

    public NetMessages.StartGame getStartGame() {
        return startGame;
    }

    public NetMessages.WorldState getLatestWorldState() {
        return latestWorldState;
    }

    public Queue<NetMessages.EchoEvent> getEchoEvents() {
        return echoEvents;
    }

    public Queue<NetMessages.SoundEvent> getSoundEvents() {
        return soundEvents;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public void createLobby(Player.Role preferredRole) {
        NetMessages.CreateLobbyRequest req = new NetMessages.CreateLobbyRequest();
        req.preferredRole = Player.toNetRole(preferredRole);
        client.sendTCP(req);
    }

    public void joinLobby(int lobbyId, Player.Role preferredRole) {
        NetMessages.JoinLobbyRequest req = new NetMessages.JoinLobbyRequest();
        req.lobbyId = lobbyId;
        req.preferredRole = Player.toNetRole(preferredRole);
        client.sendTCP(req);
    }

    public void sendInput(NetMessages.InputState input) {
        client.sendTCP(input);
    }

    public void sendThrow(NetMessages.ThrowStone throwStone) {
        client.sendTCP(throwStone);
    }

    public void stop() {
        client.stop();
    }
}
