import java.sql.*;

public class PPEDatabase {

    String url = "jdbc:mysql://localhost:3306/ppe_system";
    String user = "root";
    String password = "1234567890";

    public Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(url, user, password);
    }

    public void insertUser(String username, String password) {
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();

            System.out.println("User inserted!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean validateUser(String username, String password) {
        String query = "SELECT * FROM users WHERE username=? AND password=?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void insertRecord(String fileName, String result, String filePath) {
        String query = "INSERT INTO compliance_records (image_name, result, image_path) VALUES (?, ?, ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            System.out.println("DB INSERT DEBUG -> fileName: " + fileName);
            System.out.println("DB INSERT DEBUG -> result: " + result);
            System.out.println("DB INSERT DEBUG -> filePath: " + filePath);

            ps.setString(1, fileName);
            ps.setString(2, result);
            ps.setString(3, filePath);

            int rows = ps.executeUpdate();
            System.out.println("Record inserted! Rows affected: " + rows);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PPEDatabase db = new PPEDatabase();
        boolean result = db.validateUser("admin", "admin123");
        System.out.println("Login result: " + result);
    }
}