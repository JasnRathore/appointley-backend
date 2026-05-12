import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.UUID;

public class TestTurso {
    public static void main(String[] args) {
        String url = "jdbc:dbeaver:libsql:https://appointley-jasnrathore.aws-ap-south-1.turso.io";
        String token = System.getenv("TURSO_AUTH_TOKEN");
        try {
            Class.forName("com.dbeaver.jdbc.driver.libsql.LibSqlDriver");
            try (Connection conn = DriverManager.getConnection(url, "", token)) {
                System.out.println("Connected!");
                
                // create table for test
                try (PreparedStatement stmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS test_table (id TEXT, created_at TIMESTAMP, flag BOOLEAN, email TEXT)")) {
                    stmt.execute();
                    System.out.println("Table created!");
                } catch(Exception e) {
                    System.out.println("Create failed: " + e.getMessage());
                }

                // test insert
                try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO test_table (id, created_at, flag, email) VALUES (?, ?, ?, ?)")) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    try {
                        stmt.setObject(2, Instant.now());
                    } catch(Exception e) {
                        System.out.println("setObject(Instant) failed: " + e.getMessage());
                        stmt.setString(2, Instant.now().toString());
                    }
                    stmt.setBoolean(3, true);
                    stmt.setString(4, "test@test.com");
                    
                    stmt.executeUpdate();
                    System.out.println("Insert successful!");
                } catch(Exception e) {
                    System.out.println("Insert failed: " + e.getMessage());
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
