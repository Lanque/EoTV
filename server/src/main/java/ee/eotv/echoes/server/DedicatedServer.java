package ee.eotv.echoes.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import ee.eotv.echoes.net.NetMessages;
import ee.eotv.echoes.net.Network;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// See klass jooksutab serverit
public class DedicatedServer extends ApplicationAdapter {
    private Server server;
    private Map<Integer, GameRoom> rooms = new ConcurrentHashMap<>();
    private Map<Integer, Integer> playerToRoomMap = new ConcurrentHashMap<>();
    private int nextRoomId = 1;

    public static void main(String[] args) {
        // LibGDX Headless on vajalik Box2D füüsika jaoks serveris
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        new HeadlessApplication(new DedicatedServer(), config);
    }

    @Override
    public void create() {
        server = new Server();
        Network.register(server.getKryo());
        try {
            server.bind(Network.TCP_PORT, Network.UDP_PORT);
            server.start();
            System.out.println("Dedicated Server started on port " + Network.TCP_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof NetMessages.CreateLobbyRequest) {
                    handleCreateLobby(connection, (NetMessages.CreateLobbyRequest) object);
                } else if (object instanceof NetMessages.JoinLobbyRequest) {
                    handleJoinLobby(connection, (NetMessages.JoinLobbyRequest) object);
                } else if (object instanceof NetMessages.InputState) {
                    handleInput(connection, (NetMessages.InputState) object);
                } else if (object instanceof NetMessages.ThrowStone) {
                    handleThrow(connection, (NetMessages.ThrowStone) object);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                Integer roomId = playerToRoomMap.remove(connection.getID());
                if (roomId != null) {
                    GameRoom room = rooms.get(roomId);
                    if (room != null) {
                        room.handleDisconnect(connection.getID());
                        if (room.isEmpty()) {
                            rooms.remove(roomId);
                        }
                    }
                }
            }
        });
    }

    private void handleCreateLobby(Connection connection, NetMessages.CreateLobbyRequest req) {
        GameRoom room = new GameRoom(nextRoomId++, connection, req.preferredRole);
        rooms.put(room.id, room);
        playerToRoomMap.put(connection.getID(), room.id);

        NetMessages.LobbyJoinedResponse response = new NetMessages.LobbyJoinedResponse();
        response.success = true;
        response.lobbyId = room.id;
        response.isHost = true;
        connection.sendTCP(response); // ILMA SELLETTA JÄÄB KLIENT OOTAMA JA KATKESTAB ÜHENDUSE
    }

    private void handleJoinLobby(Connection connection, NetMessages.JoinLobbyRequest req) {
        GameRoom room = rooms.get(req.lobbyId);
        if (room == null) {
            NetMessages.LobbyJoinedResponse response = new NetMessages.LobbyJoinedResponse();
            response.success = false;
            response.lobbyId = req.lobbyId;
            response.isHost = false;
            response.message = "Lobby not found.";
            connection.sendTCP(response);
            return;
        }

        boolean success = room.join(connection, req.preferredRole);
        if (success) {
            playerToRoomMap.put(connection.getID(), room.id);
        }
    }

    private void handleInput(Connection connection, NetMessages.InputState input) {
        if (playerToRoomMap.containsKey(connection.getID())) {
            int roomId = playerToRoomMap.get(connection.getID());
            GameRoom room = rooms.get(roomId);
            if (room != null) {
                room.handleInput(connection.getID(), input);
            }
        }
    }

    private void handleThrow(Connection connection, NetMessages.ThrowStone throwStone) {
        if (playerToRoomMap.containsKey(connection.getID())) {
            int roomId = playerToRoomMap.get(connection.getID());
            GameRoom room = rooms.get(roomId);
            if (room != null) {
                room.handleThrow(connection.getID(), throwStone);
            }
        }
    }

    @Override
    public void render() {
        // See meetod jookseb 60 korda sekundis (või vastavalt configile)
        float delta = 1/60f;
        for (GameRoom room : rooms.values()) {
            room.update(delta);
        }
    }
}





