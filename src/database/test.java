/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 * @author admin
 */
public class test {
    public static void main(String[] args) {
        test1 a = new test1();
        a.abc();
    }
}

class test1 {
    void abc() {
        try (Connection conn = JDBCUtil.getConnection()) {
            String insertSql = "SELECT * FROM NHANVIEN";
            PreparedStatement pstmt = conn.prepareStatement(insertSql);

            pstmt.executeQuery();
            
            
        } catch (SQLException e) {
            System.err.println("Failed to prepare test data: " + e.getMessage());
        }
    }
}


