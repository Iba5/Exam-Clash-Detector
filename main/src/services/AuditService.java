package src.services;

import src.DBConnection.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * AuditService — logs every significant action to AUDIT_LOG.
 * Never throws — audit failure must not crash the app.
 */
public class AuditService {

    public static void log(String action, String entity, String details) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO AUDIT_LOG (action, entity, details) VALUES (?, ?, ?)")) {
            ps.setString(1, action);
            ps.setString(2, entity);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Audit log failed: " + e.getMessage());
        }
    }
}