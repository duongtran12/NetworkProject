package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/ftp_server";
    private static final String USER = "root";       // thay bằng user MySQL của bạn
    private static final String PASSWORD = "";   // thay bằng password của bạn

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
