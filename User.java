package buses;

public class User {
    private int id;
    private String login;
    private String passwordHash;
    private int role; // 1 - администратор, 0 - пользователь

    public User(int id, String login, String passwordHash, int role) {
        this.id = id;
        this.login = login;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public int getId() { return id; }
    public String getLogin() { return login; }
    public String getPasswordHash() { return passwordHash; }
    public int getRole() { return role; }
}
