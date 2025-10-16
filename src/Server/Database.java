package Server;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Database {
    private static final String DB_URL = "jdbc:sqlite:D:/Java/RockPaperScissorsGame/user.db";

    // Khởi tạo database
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


            System.out.println("[DB] Database initialized ✅");
        } catch (SQLException e) {
            System.err.println("[DB] Error initializing DB: " + e.getMessage());
        }
    }
}
