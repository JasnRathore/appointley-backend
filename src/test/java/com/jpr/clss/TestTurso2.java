package com.jpr.clss;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.UUID;
import java.sql.Timestamp;

public class TestTurso2 {
    public static void main(String[] args) {
        String url = "jdbc:dbeaver:libsql:https://appointley-jasnrathore.aws-ap-south-1.turso.io";
        String token = System.getenv("TURSO_AUTH_TOKEN");
        try {
            Class.forName("com.dbeaver.jdbc.driver.libsql.LibSqlDriver");
            try (Connection conn = DriverManager.getConnection(url, "", token)) {
                System.out.println("Connected!");
                
                try (PreparedStatement stmt = conn.prepareStatement("insert into users (auth_provider,created_at,email,email_on_booking,full_name,in_app_on_booking,marketing_emails,password_hash,updated_at,weekly_digest,id) values (?,?,?,?,?,?,?,?,?,?,?)")) {
                    stmt.setString(1, "LOCAL");
                    
                    // Hibernate passes Instant via setObject with Types.TIMESTAMP or passes Timestamp
                    // Let's test Instant directly
                    try {
                        stmt.setObject(2, Instant.now());
                    } catch(Exception e) {
                        System.out.println("setObject(Instant) failed: " + e.getMessage());
                        stmt.setTimestamp(2, Timestamp.from(Instant.now()));
                    }
                    
                    stmt.setString(3, "test2@test.com");
                    stmt.setBoolean(4, true);
                    stmt.setString(5, "Test User");
                    stmt.setBoolean(6, true);
                    stmt.setBoolean(7, false);
                    stmt.setString(8, "hash");
                    
                    try {
                        stmt.setObject(9, Instant.now());
                    } catch(Exception e) {
                        stmt.setTimestamp(9, Timestamp.from(Instant.now()));
                    }
                    
                    stmt.setBoolean(10, false);
                    stmt.setString(11, UUID.randomUUID().toString());
                    
                    stmt.execute();
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
