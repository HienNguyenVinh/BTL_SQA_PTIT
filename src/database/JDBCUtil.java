
package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JDBCUtil {

    public static String overridePassword = null;

    public static Connection getConnection() {
        try {
            // Thông tin mặc định
            final String url = "jdbc:oracle:thin:@localhost:1521/tpt_sport";
            final String user = "system";
            final String defaultPassword = "admin123";

            final String password = (overridePassword != null) ? overridePassword : defaultPassword;

            Class.forName("oracle.jdbc.driver.OracleDriver");
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(JDBCUtil.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException e) {
            e.printStackTrace(); // Có thể log đẹp hơn nếu muốn
        }
        return null;
    }
}
