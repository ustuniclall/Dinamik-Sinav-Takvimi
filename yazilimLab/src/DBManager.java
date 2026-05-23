import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBManager {

 // GÜNCELLENDİ: Olası saat dilimi hatalarını önlemek için &serverTimezone=UTC eklendi.
    private static final String URL = "jdbc:mysql://localhost:3306/sinav_database?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "654321"; // Lütfen kendi şifrenizle değiştirin

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
