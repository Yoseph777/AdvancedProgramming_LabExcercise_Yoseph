package university;

import java.sql.Connection;
import java.sql.DriverManager;

public class Database {
    public static Connection getConnection() {
        try {
            String url = "jdbc:mysql://localhost:3306/school";
            String user = "root";
            String password = "";
            return DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            System.out.println("DB Connection Error: " + e.getMessage());
            return null;
        }
    }
}