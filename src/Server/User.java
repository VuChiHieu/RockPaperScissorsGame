package Server;


public class User {
    public String username;
    public int elo;
    public int wins;
    public int losses;


    public User(String username, int elo, int wins, int losses) {
        this.username = username;
        this.elo = elo;
        this.wins = wins;
        this.losses = losses;
    }


    @Override
    public String toString() {
        return username + " | ELO: " + elo + " | W: " + wins + " | L: " + losses;
    }
}
