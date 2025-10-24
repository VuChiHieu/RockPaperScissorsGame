package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    public static final int PORT = 5000;
    public static final Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());
    public static final Map<String, User> playerStats = Collections.synchronizedMap(new HashMap<>());
    public static final Map<String, GameRoom> gameRooms = Collections.synchronizedMap(new HashMap<>());
    private static int roomCounter = 0;

    public static void main(String[] args) {
        Database.init();

        // Load all users vào bộ nhớ
        for (User u : Database.loadAllUsers()) {
            playerStats.put(u.username, u);
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] Server đang chạy trên cổng " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] Client kết nối: " + socket.getInetAddress());
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Lỗi: " + e.getMessage());
        }
    }

    public static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private String currentRoom;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));

                String line;
                while ((line = in.readLine()) != null) {
                    handleMessage(line);
                }
            } catch (IOException e) {
                System.err.println("[ClientHandler] Lỗi kết nối: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void handleMessage(String message) {
            String command;
            String data = "";

            int index = message.indexOf('|');
            if (index != -1) {
                command = message.substring(0, index);
                data = message.substring(index + 1);
            } else {
                command = message;
            }

            switch (command) {
                case "REGISTER" -> handleRegister(data);
                case "LOGIN" -> handleLogin(data);
                case "CREATE_ROOM" -> handleCreateRoom(data);
                case "JOIN_ROOM" -> handleJoinRoom(data);
                case "PLAY" -> handlePlay(data);
                case "CHAT" -> handleChat(data);
                case "LEADERBOARD" -> {
                    String leaderboard = LeaderboardManager.getLeaderboardText().replace("\n", "\\n");
                    sendMessage("LEADERBOARD|" + leaderboard);
                }
                case "LIST_ROOMS" -> handleListRooms();
                case "QUIT_ROOM" -> handleQuitRoom();
                case "GET_STATS" -> handleGetStats();
                default -> System.err.println("Lệnh không hợp lệ: " + command);
            }
        }


        // Đăng ký tài khoản — yêu cầu đăng nhập lại sau khi đăng ký
        private void handleRegister(String data) {
            String[] d = data.split("\\|");
            if (d.length != 2 || d[0].isBlank() || d[1].isBlank()) {
                sendMessage("ERROR|Thiếu thông tin đăng ký");
                return;
            }

            String user = d[0].trim();
            String pass = d[1].trim();

            if (Database.registerUser(user, pass)) {
                sendMessage("REGISTER_SUCCESS|Đăng ký thành công! Vui lòng đăng nhập lại để tiếp tục.");
            } else {
                sendMessage("ERROR|Tài khoản đã tồn tại hoặc lỗi cơ sở dữ liệu");
            }
        }

        // Đăng nhập
        private void handleLogin(String data) {
            String[] d = data.split("\\|");
            if (d.length != 2 || d[0].isBlank() || d[1].isBlank()) {
                sendMessage("ERROR|Thiếu thông tin đăng nhập");
                return;
            }

            String user = d[0].trim();
            String pass = d[1].trim();

            if (Database.loginUser(user, pass)) {
                this.username = user;
                clients.put(user, this);

                // Load hoặc tạo user mặc định
                User u = Database.loadUser(user);
                if (u == null) {
                    u = new User(user, 1000, 0, 0);
                    Database.updateUser(u);
                }
                playerStats.put(user, u);

                sendMessage("LOGIN_SUCCESS|Chào mừng " + user);
                broadcast("NOTIFY|" + user + " đã vào game");
            } else {
                sendMessage("ERROR|Sai tên đăng nhập hoặc mật khẩu");
            }
        }

        private void handleCreateRoom(String boStr) {
            if (username == null) {
                sendMessage("ERROR|Bạn chưa đăng nhập");
                return;
            }
            try {
                int bo = Integer.parseInt(boStr);
                String id = "room_" + (++roomCounter);
                GameRoom room = new GameRoom(id, username, bo, this);
                gameRooms.put(id, room);
                this.currentRoom = id;
                sendMessage("ROOM_CREATED|" + id + "|" + bo);
            } catch (NumberFormatException e) {
                sendMessage("ERROR|Giá trị BO không hợp lệ");
            }
        }

        private void handleJoinRoom(String roomId) {
            if (username == null) {
                sendMessage("ERROR|Bạn chưa đăng nhập");
                return;
            }
            GameRoom room = gameRooms.get(roomId);
            if (room != null && room.addPlayer(username, this)) {
                currentRoom = roomId;
                sendMessage("ROOM_JOINED|" + roomId + "|" + room.bo);
                room.notifyPlayers("PLAYER_JOINED|" + username);
            } else {
                sendMessage("ERROR|Phòng không tồn tại hoặc đã đầy");
            }
        }

        private void handlePlay(String move) {
            if (currentRoom == null || !gameRooms.containsKey(currentRoom)) {
                sendMessage("ERROR|Bạn chưa vào phòng");
                return;
            }
            gameRooms.get(currentRoom).playerMove(username, move);
        }

        private void handleChat(String text) {
            if (username == null || text.isBlank()) return;
            String full = username + ": " + text;
            if (currentRoom != null && gameRooms.containsKey(currentRoom)) {
                gameRooms.get(currentRoom).notifyPlayers("CHAT|" + full);
            } else {
                broadcast("CHAT|" + full);
            }
        }

        private void handleListRooms() {
            StringBuilder sb = new StringBuilder();
            for (GameRoom r : gameRooms.values()) {
                sb.append(r.roomId).append(", players=").append(r.playerCount()).append(", bo=").append(r.bo).append("\n");
            }
            sendMessage("ROOM_LIST|" + sb.toString());
        }

        private void handleQuitRoom() {
            if (currentRoom != null) {
                GameRoom r = gameRooms.get(currentRoom);
                if (r != null) {
                    r.notifyPlayers("NOTIFY|" + username + " đã rời phòng");
                    gameRooms.remove(currentRoom);
                }
            }
            currentRoom = null;
        }

        private void handleGetStats() {
            if (username == null) {
                sendMessage("ERROR|Bạn chưa đăng nhập");
                return;
            }
            User u = playerStats.get(username);
            if (u != null)
                sendMessage("STATS|" + u.username + "|" + u.wins + "|" + u.losses + "|" + u.elo);
            else
                sendMessage("STATS|Không tìm thấy người chơi");
        }

        public void sendMessage(String msg) {
            out.println(msg);
        }

        private void broadcast(String msg) {
            synchronized (clients) {
                for (ClientHandler ch : clients.values()) {
                    ch.sendMessage(msg);
                }
            }
        }

        private void cleanup() {
            if (username != null) {
                clients.remove(username);
                handleQuitRoom();
                broadcast("NOTIFY|" + username + " đã thoát game");
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    // ==============================
    //         GAME ROOM CLASS
    // ==============================
    public static class GameRoom {
        public final String roomId;
        public final int bo;
        public final String player1;
        public String player2;
        public ClientHandler handler1;
        public ClientHandler handler2;

        private String move1, move2;
        private int wins1 = 0, wins2 = 0;

        public GameRoom(String roomId, String player1, int bo, ClientHandler creatorHandler) {
            this.roomId = roomId;
            this.player1 = player1;
            this.bo = bo;
            this.handler1 = creatorHandler;
        }

        public boolean addPlayer(String username, ClientHandler handler) {
            if (player2 == null && !username.equals(player1)) {
                this.player2 = username;
                this.handler2 = handler;
                return true;
            }
            return false;
        }

        public int playerCount() {
            int c = 0;
            if (player1 != null) c++;
            if (player2 != null) c++;
            return c;
        }

        public void playerMove(String username, String move) {
            if (username.equals(player1)) move1 = move;
            else if (username.equals(player2)) move2 = move;

            if (move1 != null && move2 != null) processRound();
        }

        private void processRound() {
            String winner = getRoundWinner();
            if (!"HÒA".equals(winner)) {
                if (winner.equals(player1)) wins1++;
                else wins2++;
            }
            notifyPlayers("ROUND_RESULT|" + move1 + "|" + move2 + "|" + winner + "|" + wins1 + "|" + wins2);

            if (wins1 > bo / 2 || wins2 > bo / 2) endGame();

            move1 = move2 = null;
        }

        private String getRoundWinner() {
            if (move1 == null || move2 == null) return "HÒA";
            if (move1.equals(move2)) return "HÒA";
            if ((move1.equals("KÉO") && move2.equals("BAO")) ||
                    (move1.equals("BÚA") && move2.equals("KÉO")) ||
                    (move1.equals("BAO") && move2.equals("BÚA"))) {
                return player1;
            }
            return player2;
        }

        private void endGame() {
            String winner = wins1 > wins2 ? player1 : player2;
            String loser = winner.equals(player1) ? player2 : player1;

            User uWinner = playerStats.getOrDefault(winner, new User(winner, 1000, 0, 0));
            User uLoser = playerStats.getOrDefault(loser, new User(loser, 1000, 0, 0));

            int K = 32;
            double expectedW = expectedScore(uWinner.elo, uLoser.elo);
            double expectedL = expectedScore(uLoser.elo, uWinner.elo);
            int deltaW = (int) Math.round(K * (1 - expectedW));
            int deltaL = (int) Math.round(K * (0 - expectedL));

            uWinner.wins++;
            uWinner.elo = Math.max(0, uWinner.elo + deltaW);

            uLoser.losses++;
            uLoser.elo = Math.max(0, uLoser.elo + deltaL);

            playerStats.put(uWinner.username, uWinner);
            playerStats.put(uLoser.username, uLoser);

            Database.updateUser(uWinner);
            Database.updateUser(uLoser);
            Database.recordMatch(player1, player2, winner, loser, bo, wins1, wins2);

            notifyPlayers("GAME_END|" + winner + "|" + loser + "|" + wins1 + "|" + wins2);
            gameRooms.remove(roomId);
        }

        private double expectedScore(int a, int b) {
            return 1.0 / (1.0 + Math.pow(10.0, (b - a) / 400.0));
        }

        public void notifyPlayers(String msg) {
            if (handler1 != null) handler1.sendMessage(msg);
            if (handler2 != null) handler2.sendMessage(msg);
        }
    }
}
