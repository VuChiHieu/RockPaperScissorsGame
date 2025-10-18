package Server;

import java.util.List;

public class LeaderboardManager {

    public static String getLeaderboardText() {
        List<User> topUsers = Database.getTopUsers(10);

        StringBuilder sb = new StringBuilder();
        sb.append("🏆 BẢNG XẾP HẠNG 🏆\n");
        sb.append("===========================================\n");
        sb.append(String.format("%-4s %-15s %6s %8s %8s\n", "#", "Người chơi", "ELO", "Thắng", "Thua"));
        sb.append("-------------------------------------------\n");

        if (topUsers.isEmpty()) {
            sb.append("Chưa có người chơi nào!\n");
            return sb.toString();
        }

        int rank = 1;
        for (User u : topUsers) {
            sb.append(String.format("%-4d %-15s %6d %8d %8d\n",
                    rank++, u.username, u.elo, u.wins, u.losses));
        }

        sb.append("===========================================\n");
        sb.append("Tổng số người chơi: ").append(topUsers.size()).append("\n");

        return sb.toString();
    }
}
