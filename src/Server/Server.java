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
                case "LIST_ROOMS" -> handleListRooms();
                case "QUIT_ROOM" -> handleQuitRoom();
                case "GET_STATS" -> handleGetStats();
                default -> System.err.println("Lệnh không hợp lệ: " + command);
            }
        }


        // Đăng ký tài khoản — yêu cầu đăng nhập lại sau khi đăng ký
        private void handleRegister(String data) {

        }

        // Đăng nhập
        private void handleLogin(String data) {

        }

        private void handleCreateRoom(String boStr) {

        }

        private void handleJoinRoom(String roomId) {}

        private void handlePlay(String move) {}

        private void handleChat(String text) {}

        private void handleListRooms() {}

        private void handleQuitRoom() {}

        private void handleGetStats() {}

        public void sendMessage(String msg) {
            out.println(msg);
        }

        private void broadcast(String msg) {}

        private void cleanup() {}
    }
}
