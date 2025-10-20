package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    public static final int PORT = 5000;

    public static void main(String[] args) {

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

    public static class GameRoom {}
}
