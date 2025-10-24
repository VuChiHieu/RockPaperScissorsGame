package Server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:D:/Lap Trinh Mang/Giua ki/Nhom2-RockPaperScissors/RockPaperScissorsGame/user.db";

    // KHỞI TẠO DATABASE
    public static void init() {
        String dbPath = DB_URL.replace("jdbc:sqlite:", "");
        java.io.File f = new java.io.File(dbPath);
        System.out.println("[DB] Using database file: " + f.getAbsolutePath());
        System.out.println("[DB] Exists: " + f.exists());
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Bảng người chơi
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            username TEXT PRIMARY KEY,
                            password TEXT NOT NULL,
                            elo INTEGER DEFAULT 1000,
                            wins INTEGER DEFAULT 0,
                            losses INTEGER DEFAULT 0
                        )
                    """);

            // Bảng lịch sử trận đấu
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS matches (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            player1 TEXT,
                            player2 TEXT,
                            winner TEXT,
                            loser TEXT,
                            bo INTEGER,
                            score1 INTEGER,
                            score2 INTEGER,
                            played_at DATETIME DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            System.out.println("[DB] Database initialized");
        } catch (SQLException e) {
            System.err.println("[DB] Error initializing DB: " + e.getMessage());
        }
    }

    // ĐĂNG KÝ NGƯỜI DÙNG
    public static boolean registerUser(String username, String password) {
        String sqlCheck = "SELECT username FROM users WHERE username = ?";
        String sqlInsert = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setString(1, username);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) return false; // Tài khoản đã tồn tại
            }

            try (PreparedStatement psIns = conn.prepareStatement(sqlInsert)) {
                psIns.setString(1, username);
                psIns.setString(2, password);
                psIns.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[DB] registerUser error: " + e.getMessage());
            return false;
        }
    }

    // ĐĂNG NHẬP NGƯỜI DÙNG
    public static boolean loginUser(String username, String password) {
        String sql = "SELECT username FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[DB] loginUser error: " + e.getMessage());
            return false;
        }
    }


    // TẢI 1 NGƯỜI DÙNG
    public static User loadUser(String username) {
        String sql = "SELECT username, elo, wins, losses FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("username"),
                            rs.getInt("elo"),
                            rs.getInt("wins"),
                            rs.getInt("losses")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] loadUser error: " + e.getMessage());
        }
        return null;
    }

    // TẢI TOÀN BỘ NGƯỜI DÙNG
    public static List<User> loadAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT username, elo, wins, losses FROM users";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new User(
                        rs.getString("username"),
                        rs.getInt("elo"),
                        rs.getInt("wins"),
                        rs.getInt("losses")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DB] loadAllUsers error: " + e.getMessage());
        }
        return list;
    }

    // CẬP NHẬT NGƯỜI DÙNG
    public static void updateUser(User u) {
        String sql = "UPDATE users SET elo = ?, wins = ?, losses = ? WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, u.elo);
            ps.setInt(2, u.wins);
            ps.setInt(3, u.losses);
            ps.setString(4, u.username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] updateUser error: " + e.getMessage());
        }
    }


    // LƯU LỊCH SỬ TRẬN ĐẤU
    public static void recordMatch(String p1, String p2, String winner, String loser,
                                   int bo, int s1, int s2) {
        String sql = "INSERT INTO matches(player1, player2, winner, loser, bo, score1, score2) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p1);
            ps.setString(2, p2);
            ps.setString(3, winner);
            ps.setString(4, loser);
            ps.setInt(5, bo);
            ps.setInt(6, s1);
            ps.setInt(7, s2);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] recordMatch error: " + e.getMessage());
        }
    }

    // BẢNG XẾP HẠNG TOP N
    public static List<User> getTopUsers(int limit) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT username, elo, wins, losses FROM users ORDER BY elo DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("[DB] Found user: " + rs.getString("username") + " | ELO: " + rs.getInt("elo"));
                    list.add(new User(
                            rs.getString("username"),
                            rs.getInt("elo"),
                            rs.getInt("wins"),
                            rs.getInt("losses")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] getTopUsers error: " + e.getMessage());
        }
        System.out.println("[DB] Total top users loaded: " + list.size());
        return list;
    }
}
