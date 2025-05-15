package test;

import Model.SuKien;
import Model.TaiKhoan;
import dao.TaiKhoanDAO;
import database.JDBCUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;


public class TaiKhoanDAOTest {

    private TaiKhoanDAO taiKhoanDAO;
    private Connection connection;

    private static final String EXISTING_ACTIVE_USER_FOR_CHANGE_PASS = "NV12";
    private static final String EXISTING_ACTIVE_USER = "NV01";
    private static final String EXISTING_ACTIVE_USER_FOR_UPDATE = "NV03";
    private static final String EXISTING_INACTIVE_USER = "testuser_inactive";
    private static final String EXISTING_ADMIN_USER = "NV02";
    private static final String EXISTING_ADMIN_PASS = "@123@123";
    private static final String EXISTING_USER_PASSWORD = "123@123@";
    private static final String NON_EXISTENT_USER = "nonexistent_user";
    private static final String STATUS_ACTIVE = "Hoạt động";
    private static final String STATUS_INACTIVE = "Khóa";
    private static final String ROLE_ADMIN = "QL";
    private static final String ROLE_USER = "NVBH";
    private static final String NEW_PASSWORD = "newPassword456";


    @Before
    public void setUp() throws SQLException {
        taiKhoanDAO = TaiKhoanDAO.getInstance();
        connection = JDBCUtil.getConnection();
        connection.setAutoCommit(false);
        System.out.println("------------ Test Started - Transaction Began ------------");
    }

    @After
    public void tearDown() throws SQLException {
        if (connection != null) {
            try {
                connection.rollback();
                System.out.println("------------ Test Finished - Transaction Rolled Back ------------");
            } catch (SQLException e) {
                System.err.println("Error rolling back transaction: " + e.getMessage());
            } finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    // --- Test cho phương thức update ---
    @Test
    public void testUpdate_Success() {
        System.out.println("Testing update - Success Case");
        TaiKhoan tkToUpdate = new TaiKhoan(EXISTING_ACTIVE_USER_FOR_UPDATE, "", "", STATUS_INACTIVE);
        int result = taiKhoanDAO.update(tkToUpdate);
        assertTrue("Update should affect at least one row", result > 0);

        TaiKhoan updatedTk = taiKhoanDAO.getById(EXISTING_ACTIVE_USER_FOR_UPDATE);
        assertNotNull("Updated account should exist", updatedTk);
        assertEquals("Status should be updated", STATUS_INACTIVE, updatedTk.getTrangThai());
    }

    @Test
    public void testUpdate_NonExistentUser() {
        System.out.println("Testing update - Non Existent User");
        TaiKhoan tkToUpdate = new TaiKhoan(NON_EXISTENT_USER, "", "", STATUS_ACTIVE);
        int result = taiKhoanDAO.update(tkToUpdate);
        assertEquals("Update should affect 0 rows for non-existent user", 0, result);
    }

    @Test
    public void testUpdate_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        TaiKhoan tkToUpdate = new TaiKhoan(EXISTING_ACTIVE_USER_FOR_UPDATE, "", "", STATUS_INACTIVE);
        try {
            int result = taiKhoanDAO.update(tkToUpdate);
            assertNull("Khi gặp SQLException, update() phải trả về null", result);
        } catch (Exception e) {
            fail("update() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test cho phương thức getAll ---
    @Test
    public void testGetAll_DataExists() {
        System.out.println("Testing getAll - Data Exists");

        ArrayList<TaiKhoan> result = taiKhoanDAO.getAll();
        assertNotNull("Result list should not be null", result);
        System.out.println(result);
        assertFalse("Result list should not be empty", result.isEmpty());

        System.out.println("Found " + result.size() + " accounts.");
    }

     @Test
     public void testGetAll_NoData() {
         try (Connection conn = JDBCUtil.getConnection()) {
             conn.setAutoCommit(false);

             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM TAIKHOAN;");

             int rowsDeleted = pstmt.executeUpdate();
             ArrayList<TaiKhoan> result = taiKhoanDAO.getAll();
             assertNotNull("getAll() should return an empty list, not null, when no data", result);
             assertEquals("getAll() should return an empty list when no data", 0, result.size());
             System.out.println("testGetAll_Empty: Skipped (requires specific empty DB state)");

             conn.rollback();
             System.out.println("Cleared test data.");
         } catch (SQLException e) {
             System.err.println("Failed to clear test data: " + e.getMessage());
         }
     }

    @Test
    public void testGetAll_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<TaiKhoan> result = taiKhoanDAO.getAll();
            assertNull("Khi gặp SQLException, getAll() phải trả về null", result);
        } catch (Exception e) {
            fail("getAll() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test cho phương thức getById ---

    @Test
    public void testGetById_Found() {
        System.out.println("Testing getById - Found");
        TaiKhoan result = taiKhoanDAO.getById(EXISTING_ACTIVE_USER);
        assertNotNull("Account should be found", result);
        assertEquals("Username should match", EXISTING_ACTIVE_USER, result.getTenDangNhap());
        assertEquals(ROLE_USER, result.getPhanQuyen());
        assertEquals(STATUS_ACTIVE, result.getTrangThai());
    }

    @Test
    public void testGetById_NotFound() {
        System.out.println("Testing getById - Not Found");
        TaiKhoan result = taiKhoanDAO.getById(NON_EXISTENT_USER);
        assertNull("Account should not be found", result);
    }

    @Test
    public void testGetById_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            TaiKhoan result = taiKhoanDAO.getById("abc");
            assertNull("Khi gặp SQLException, getById() phải trả về null", result);
        } catch (Exception e) {
            fail("getById() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test cho phương thức getByTrangThai ---

    @Test
    public void testGetByTrangThai_FoundActive() {
        System.out.println("Testing getByTrangThai - Found Active");
        ArrayList<TaiKhoan> result = taiKhoanDAO.getByTrangThai(STATUS_ACTIVE);
        assertNotNull("Result list should not be null", result);
        assertFalse("Should find active accounts", result.isEmpty());

        for (TaiKhoan tk : result) {
            assertEquals("Account status should be Active", STATUS_ACTIVE, tk.getTrangThai());
        }
        System.out.println("Found " + result.size() + " active accounts.");
    }

    @Test
    public void testGetByTrangThai_FoundInactive() {
        System.out.println("Testing getByTrangThai - Found Inactive");
        ArrayList<TaiKhoan> result = taiKhoanDAO.getByTrangThai(STATUS_INACTIVE);
        assertNotNull("Result list should not be null", result);

        if (!result.isEmpty()) {
            System.out.println("Found " + result.size() + " inactive accounts.");
            for (TaiKhoan tk : result) {
                assertEquals("Account status should be Inactive", STATUS_INACTIVE, tk.getTrangThai());
            }
        } else {
            System.out.println("No inactive accounts found (as per test data).");
        }
    }

    @Test
    public void testGetByTrangThai_NotFound() {
        System.out.println("Testing getByTrangThai - Status Not Found");
        ArrayList<TaiKhoan> result = taiKhoanDAO.getByTrangThai("TrangThaiKhongTonTai");
        assertNotNull("Result list should not be null", result);
        assertTrue("Should not find accounts with non-existent status", result.isEmpty());
    }

    @Test
    public void testGetByTrangThai_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<TaiKhoan> result = taiKhoanDAO.getByTrangThai(STATUS_ACTIVE);
            assertNull("Khi gặp SQLException, getByTrangThai() phải trả về null", result);
        } catch (Exception e) {
            fail("getByTrangThai() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test cho phương thức getByPhanQuyen ---

    @Test
    public void testGetByPhanQuyen_FoundAdmin() {
        System.out.println("Testing getByPhanQuyen - Found Admin");
        ArrayList<TaiKhoan> result = taiKhoanDAO.getByPhanQuyen(ROLE_ADMIN);
        assertNotNull("Result list should not be null", result);
        assertFalse("Should find admin accounts", result.isEmpty());
        for (TaiKhoan tk : result) {
            assertEquals("Account role should be Admin", ROLE_ADMIN, tk.getPhanQuyen());
        }
        System.out.println("Found " + result.size() + " admin accounts.");
    }

    @Test
    public void testGetByPhanQuyen_FoundUser() {
        System.out.println("Testing getByPhanQuyen - Found User");
        ArrayList<TaiKhoan> result = taiKhoanDAO.getByPhanQuyen(ROLE_USER);
        assertNotNull("Result list should not be null", result);
        assertFalse("Should find user accounts", result.isEmpty());
        for (TaiKhoan tk : result) {
            assertEquals("Account role should be User", ROLE_USER, tk.getPhanQuyen());
        }
        System.out.println("Found " + result.size() + " user accounts.");
    }

    @Test
    public void testGetByPhanQuyen_NotFound() {
        System.out.println("Testing getByPhanQuyen - Role Not Found");
        ArrayList<TaiKhoan> result = taiKhoanDAO.getByPhanQuyen("QuyenKhongTonTai");
        assertNotNull("Result list should not be null", result);
        assertTrue("Should not find accounts with non-existent role", result.isEmpty());
    }

    @Test
    public void testGetByPhanQuyen_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<TaiKhoan> result = taiKhoanDAO.getByPhanQuyen(ROLE_USER);
            assertNull("Khi gặp SQLException, getByPhanQuyen() phải trả về null", result);
        } catch (Exception e) {
            fail("getByPhanQuyen() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test cho phương thức layLaiMatKhau ---

    @Test
    public void testLayLaiMatKhau_Success() {
        System.out.println("Testing layLaiMatKhau - Success");

        String password = taiKhoanDAO.layLaiMatKhau(EXISTING_ACTIVE_USER_FOR_CHANGE_PASS);
        assertNotNull("Password/token should not be null", password);
        assertFalse("Password/token should not be empty", password.isEmpty());
        System.out.println("Retrieved password/token: " + password);
    }

    @Test
    public void testLayLaiMatKhau_NonExistentUser() {
        System.out.println("Testing layLaiMatKhau - Non Existent User");
        String password = taiKhoanDAO.layLaiMatKhau(NON_EXISTENT_USER);
        assertNotNull("Result should not be null", password);
        assertEquals("Password should be empty for non-existent user", "", password);
    }

    @Test
    public void testLayLaiMatKhau_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            String result = taiKhoanDAO.layLaiMatKhau(EXISTING_ACTIVE_USER);
            assertNull("Khi gặp SQLException, layLaiMatKhau() phải trả về null", result);
        } catch (Exception e) {
            fail("layLaiMatKhau() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test cho phương thức doiMatKhau ---

    // Skip do phương thức là hàm Void

    // --- Test cho phương thức kiemTraTrangThai ---

    @Test
    public void testKiemTraTrangThai_ValidActiveLogin() {
        System.out.println("Testing kiemTraTrangThai - Valid Active Login");
        int status = taiKhoanDAO.kiemTraTrangThai(EXISTING_ACTIVE_USER, EXISTING_USER_PASSWORD);

        assertEquals("Status should indicate active", 1, status);
    }

    @Test
    public void testKiemTraTrangThai_ValidInactiveLogin() {
        System.out.println("Testing kiemTraTrangThai - Valid Inactive Login");

        int status = taiKhoanDAO.kiemTraTrangThai(EXISTING_INACTIVE_USER, EXISTING_USER_PASSWORD);

        assertEquals("Status should indicate inactive/locked", 0, status);
    }

    @Test
    public void testKiemTraTrangThai_InvalidPassword() {
        System.out.println("Testing kiemTraTrangThai - Invalid Password");
        int status = taiKhoanDAO.kiemTraTrangThai(EXISTING_ACTIVE_USER, "wrongpassword");

        assertEquals("Status should indicate failure", 0, status);
    }

    @Test
    public void testKiemTraTrangThai_InvalidUser() {
        System.out.println("Testing kiemTraTrangThai - Invalid User");
        int status = taiKhoanDAO.kiemTraTrangThai(NON_EXISTENT_USER, EXISTING_USER_PASSWORD);

        assertEquals("Status should indicate failure", 0, status);
    }

    @Test
    public void testKiemTraTrangThai_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            int result = taiKhoanDAO.kiemTraTrangThai(EXISTING_ACTIVE_USER, "abc");
            assertNull("Khi gặp SQLException, kiemTraTrangThai() phải trả về null", result);
        } catch (Exception e) {
            fail("kiemTraTrangThai() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test cho phương thức kiemTraPhanQuyen ---

    @Test
    public void testKiemTraPhanQuyen_ValidAdminLogin() {
        System.out.println("Testing kiemTraPhanQuyen - Valid Admin Login");
        String role = taiKhoanDAO.kiemTraPhanQuyen(EXISTING_ADMIN_USER, EXISTING_ADMIN_PASS);
        assertEquals("Role should be Admin", ROLE_ADMIN, role);
    }

    @Test
    public void testKiemTraPhanQuyen_ValidUserLogin() {
        System.out.println("Testing kiemTraPhanQuyen - Valid User Login");
        String role = taiKhoanDAO.kiemTraPhanQuyen(EXISTING_ACTIVE_USER, EXISTING_USER_PASSWORD);
        assertEquals("Role should be User", ROLE_USER, role);
    }

    @Test
    public void testKiemTraPhanQuyen_InvalidPassword() {
        System.out.println("Testing kiemTraPhanQuyen - Invalid Password");
        String role = taiKhoanDAO.kiemTraPhanQuyen(EXISTING_ACTIVE_USER, "wrongpassword");

        assertEquals("Role should be empty for failed login", null, role);
    }

    @Test
    public void testKiemTraPhanQuyen_InvalidUser() {
        System.out.println("Testing kiemTraPhanQuyen - Invalid User");
        String role = taiKhoanDAO.kiemTraPhanQuyen(NON_EXISTENT_USER, EXISTING_USER_PASSWORD);

        assertEquals("Role should be empty for failed login", null, role);
    }

    @Test
    public void testKiemTraPhanQuyen_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            String result = taiKhoanDAO.kiemTraPhanQuyen(EXISTING_ACTIVE_USER, EXISTING_USER_PASSWORD);
            assertNull("Khi gặp SQLException, kiemTraPhanQuyen() phải trả về null", result);
        } catch (Exception e) {
            fail("kiemTraPhanQuyen() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }
}