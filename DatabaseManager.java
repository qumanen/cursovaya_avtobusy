package buses;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/buses_timetable?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "bus_admin";
    private static final String DB_PASS = "12345";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, DB_USER, DB_PASS);
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка хеширования", e);
        }
    }

    public static User authenticate(String login, String password) {
        String sql = "SELECT * FROM users WHERE login = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, hashPassword(password));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("login"), rs.getString("password"), rs.getInt("role"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<BusRoute> getRoutes(String search, String sortBy) throws SQLException {
        List<BusRoute> list = new ArrayList<>();
        String allowedSort = "id";
        if ("route_number".equals(sortBy) || "bus_type".equals(sortBy) || "destination".equals(sortBy) || "departure_time".equals(sortBy) || "arrival_time".equals(sortBy)) {
            allowedSort = sortBy;
        }

        String sql = "SELECT * FROM routes";
        boolean hasSearch = search != null && !search.trim().isEmpty();
        if (hasSearch) {
            sql += " WHERE route_number LIKE ? OR bus_type LIKE ? OR destination LIKE ?";
        }
        sql += " ORDER BY " + allowedSort + " ASC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (hasSearch) {
                String pattern = "%" + search.trim() + "%";
                pstmt.setString(1, pattern);
                pstmt.setString(2, pattern);
                pstmt.setString(3, pattern);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new BusRoute(
                        rs.getInt("id"),
                        rs.getString("route_number"),
                        rs.getString("bus_type"),
                        rs.getString("destination"),
                        rs.getString("departure_time"),
                        rs.getString("arrival_time")
                ));
            }
        }
        return list;
    }

    public static List<BusRoute> getIndividualTaskRoutes(String destination, String userTime) throws SQLException {
        List<BusRoute> list = new ArrayList<>();
        String fullTime = userTime + ":00";
        String sql;

        
        if (fullTime.compareTo("12:00:00") >= 0) {
            sql = "SELECT * FROM routes WHERE destination = ? AND arrival_time <= SUBTIME(?, '12:00:00')";
        } else {
            sql = "SELECT * FROM routes WHERE destination = ? AND arrival_time <= ADDTIME(?, '12:00:00')";
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, destination);
            pstmt.setString(2, fullTime);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new BusRoute(
                        rs.getInt("id"),
                        rs.getString("route_number"),
                        rs.getString("bus_type"),
                        rs.getString("destination"),
                        rs.getString("departure_time"),
                        rs.getString("arrival_time")
                ));
            }
        }
        return list;
    }


    public static BusRoute getRouteById(int id) throws SQLException {
        String sql = "SELECT * FROM routes WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new BusRoute(
                        rs.getInt("id"),
                        rs.getString("route_number"),
                        rs.getString("bus_type"),
                        rs.getString("destination"),
                        rs.getString("departure_time"),
                        rs.getString("arrival_time")
                );
            }
        }
        return null;
    }

    public static void addRoute(String number, String type, String dest, String dep, String arr) throws SQLException {
        String sql = "INSERT INTO routes (route_number, bus_type, destination, departure_time, arrival_time) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, number);
            pstmt.setString(2, type);
            pstmt.setString(3, dest);
            pstmt.setString(4, dep);
            pstmt.setString(5, arr);
            pstmt.executeUpdate();
        }
    }

    public static void updateRoute(int id, String number, String type, String dest, String dep, String arr) throws SQLException {
        String sql = "UPDATE routes SET route_number=?, bus_type=?, destination=?, departure_time=?, arrival_time=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, number);
            pstmt.setString(2, type);
            pstmt.setString(3, dest);
            pstmt.setString(4, dep);
            pstmt.setString(5, arr);
            pstmt.setInt(6, id);
            pstmt.executeUpdate();
        }
    }

    public static void deleteRoute(int id) throws SQLException {
        String sql = "DELETE FROM routes WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public static void createRoutesTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS routes (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "route_number VARCHAR(10) UNIQUE, " +
                "bus_type VARCHAR(50), " +
                "destination VARCHAR(100), " +
                "departure_time TIME, " +
                "arrival_time TIME)";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public static void dropRoutesTable() throws SQLException {
        String sql = "DROP TABLE IF EXISTS routes";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public static boolean isRoutesTableExists() {
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, "routes", new String[]{"TABLE"});
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id ASC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(rs.getInt("id"), rs.getString("login"), rs.getString("password"), rs.getInt("role")));
            }
        }
        return users;
    }

    public static void addUser(String login, String rawPassword, int role) throws SQLException {
        String sql = "INSERT INTO users (login, password, role) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, hashPassword(rawPassword));
            pstmt.setInt(3, role);
            pstmt.executeUpdate();
        }
    }

    public static void updateUser(int id, String login, String newRawPassword, int role) throws SQLException {
        if (newRawPassword != null && !newRawPassword.trim().isEmpty()) {
            String sql = "UPDATE users SET login=?, password=?, role=? WHERE id=?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login);
                pstmt.setString(2, hashPassword(newRawPassword));
                pstmt.setInt(3, role);
                pstmt.setInt(4, id);
                pstmt.executeUpdate();
            }
        } else {
            String sql = "UPDATE users SET login=?, role=? WHERE id=?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login);
                pstmt.setInt(2, role);
                pstmt.setInt(3, id);
                pstmt.executeUpdate();
            }
        }
    }

    public static void deleteUser(int id) throws SQLException {
    	String sql = "DELETE FROM users WHERE id = ?";
    	try (Connection conn = getConnection();
    	PreparedStatement pstmt = conn.prepareStatement(sql)) {
    	pstmt.setInt(1, id);
    	pstmt.executeUpdate();
    	}
    	}
    	}