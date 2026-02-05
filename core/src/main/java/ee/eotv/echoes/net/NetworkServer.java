package ee.eotv.echoes.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import ee.eotv.echoes.entities.Player;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NetworkServer {
    private static final int HOST_ID = 1;
    private static final int CLIENT_ID = 2;

    private final Server server = new Server();
    private final Map<Integer, NetMessages.InputState> latestInputs = new ConcurrentHashMap<>();
    private final Queue<NetMessages.ThrowStone> throwQueue = new ConcurrentLinkedQueue<>();

    private Player.Role hostRole;
    private Player.Role clientRole;
    private Connection clientConnection;
    private boolean gameStarted = false;

    public void start(Player.Role hostRole) throws IOException {
        this.hostRole = hostRole;
        Network.register(server.getKryo());
        server.start();
        server.bind(Network.TCP_PORT, Network.UDP_PORT);
        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof NetMessages.JoinRequest) {
                    handleJoin(connection, (NetMessages.JoinRequest) object);
                } else if (object instanceof NetMessages.InputState) {
                    handleInput((NetMessages.InputState) object);
                } else if (object instanceof NetMessages.ThrowStone) {
                    handleThrow((NetMessages.ThrowStone) object);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                if (connection == clientConnection) {
                    clientConnection = null;
                    clientRole = null;
                    gameStarted = false;
                }
            }
        });
    }

    private void handleJoin(Connection connection, NetMessages.JoinRequest request) {
        NetMessages.JoinResponse response = new NetMessages.JoinResponse();
        if (clientConnection != null) {
            response.accepted = false;
            response.message = "Server full.";
            connection.sendTCP(response);
            return;
        }

        if (request.role == null) {
            response.accepted = false;
            response.message = "Role missing.";
            connection.sendTCP(response);
            return;
        }

        if (request.role == hostRole) {
            response.accepted = false;
            response.message = "Role already taken.";
            connection.sendTCP(response);
            return;
        }

        clientConnection = connection;
        clientRole = request.role;

        response.accepted = true;
        response.playerId = CLIENT_ID;
        response.role = request.role;
        response.message = "Connected.";
        connection.sendTCP(response);

        NetMessages.StartGame startGame = new NetMessages.StartGame();
        startGame.playerId = CLIENT_ID;
        startGame.role = request.role;
        connection.sendTCP(startGame);
        gameStarted = true;
    }

    private void handleInput(NetMessages.InputState input) {
        latestInputs.put(input.playerId, copyInput(input));
    }

    private void handleThrow(NetMessages.ThrowStone throwStone) {
        throwQueue.add(throwStone);
    }

    private NetMessages.InputState copyInput(NetMessages.InputState input) {
        NetMessages.InputState copy = new NetMessages.InputState();
        copy.playerId = input.playerId;
        copy.up = input.up;
        copy.down = input.down;
        copy.left = input.left;
        copy.right = input.right;
        copy.run = input.run;
        copy.toggleLight = input.toggleLight;
        copy.aimAngle = input.aimAngle;
        return copy;
    }

    public boolean hasClient() {
        return clientConnection != null;
    }

    public boolean isGameStarted() {
        return gameStarted && clientConnection != null;
    }

    public int getHostPlayerId() {
        return HOST_ID;
    }

    public int getClientPlayerId() {
        return CLIENT_ID;
    }

    public Player.Role getHostRole() {
        return hostRole;
    }

    public Player.Role getClientRole() {
        return clientRole;
    }

    public NetMessages.InputState getLatestInput(int playerId) {
        NetMessages.InputState input = latestInputs.get(playerId);
        if (input == null) {
            input = new NetMessages.InputState();
            input.playerId = playerId;
        }
        return input;
    }

    public Queue<NetMessages.ThrowStone> getThrowQueue() {
        return throwQueue;
    }

    public void sendWorldState(NetMessages.WorldState state) {
        if (clientConnection != null) {
            clientConnection.sendTCP(state);
        }
    }

    public void sendEcho(NetMessages.EchoEvent event) {
        if (clientConnection != null) {
            clientConnection.sendTCP(event);
        }
    }

    public void sendSound(NetMessages.SoundEvent event) {
        if (clientConnection != null) {
            clientConnection.sendTCP(event);
        }
    }

    public void stop() {
        server.stop();
    }
}
