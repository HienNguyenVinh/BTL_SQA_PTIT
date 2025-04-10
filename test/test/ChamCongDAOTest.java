package test;

import Model.ChamCong;
import dao.ChamCongDAO;
import database.JDBCUtil;
import org.junit.*;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;

import static org.junit.Assert.*;

public class ChamCongDAOTest {

    private static Connection connection;
    private ChamCongDAO chamCongDAO;

    private static final String EXISTING_MANV = "NV01";
    private static final String EXISTING_TENNV = "Hồ Việt Trung";
    private static final String NON_EXISTING_MANV = "NV999";
    private static final String NON_EXISTING_TENNV = "Somebody";
    private static final Date EXISTING_DATE;
    private static final Date NON_EXISTING_DATE;
    private static final int EXPECTED_COUNT_FOR_EXISTING_MANV = 0;
    private static final int EXPECTED_TOTAL_RECORDS = 1;

    static {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        EXISTING_DATE = new Date(cal.getTimeInMillis());

        cal.add(Calendar.YEAR, 10);
        NON_EXISTING_DATE = new Date(cal.getTimeInMillis());
    }


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.out.println("Starting ChamCongDAO Tests...");
        try {
            connection = JDBCUtil.getConnection();
            if (connection == null) {
                throw new SQLException("Failed to get initial connection.");
            }
            prepareTestData();
            connection.close();
        } catch (SQLException e) {
            System.err.println("Database connection error during setup: " + e.getMessage());
            Assume.assumeTrue("Database connection required for tests", false);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        System.out.println("Finished ChamCongDAO Tests.");
    }

    @Before
    public void setUp() throws Exception {
        try {
            connection = JDBCUtil.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            System.err.println("Failed to get connection for test: " + e.getMessage());
            Assume.assumeTrue("Database connection required for this test", false);
        }
        chamCongDAO = ChamCongDAO.getInstance();
        assertNotNull("ChamCongDAO instance should not be null", chamCongDAO);
    }

    @After
    public void tearDown() throws Exception {
        // Rollback transaction
        if (connection != null) {
            try {
                connection.rollback();
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error during rollback/close: " + e.getMessage());
            }
        }
    }

    private static void prepareTestData() {
        System.out.println("Preparing test data ...");
        String insertSql = "INSERT INTO CHAMCONG (MANHANVIEN, NGAYLAMVIEC) VALUES (?, ?)";
        try (Connection conn = JDBCUtil.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(insertSql);

            pstmt.setString(1, EXISTING_MANV);
            pstmt.setDate(2, EXISTING_DATE);
            pstmt.executeUpdate();

//            conn.commit();
            System.out.println("Test data prepared.");
        } catch (SQLException e) {
            System.err.println("Failed to prepare test data: " + e.getMessage());
        }
    }


    // --- Test Cases ---

    @Test
    public void testGetInstance() {
        ChamCongDAO instance1 = ChamCongDAO.getInstance();
        ChamCongDAO instance2 = ChamCongDAO.getInstance();
        assertNotNull("getInstance() should return a non-null instance", instance1);
        assertNotSame("getInstance() currently returns new instances", instance1, instance2);
    }

    @Test
    public void testGetAll_Success() {
        ArrayList<ChamCong> result = chamCongDAO.getAll();
        assertNotNull("getAll() should not return null on success", result);
        assertTrue("getAll() should return some records (check test data)", result.size() > 0);
        assertEquals("getAll() should return all expected records", EXPECTED_TOTAL_RECORDS, result.size());
        System.out.println("testGetAll_Success: Found " + result.size() + " records.");
    }

    @Test
    public void testGetAll_Empty() {
        try (Connection conn = JDBCUtil.getConnection()) {
            conn.setAutoCommit(false);

            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM CHAMCONG WHERE MANHANVIEN = ?");

            pstmt.setString(1, EXISTING_MANV);
            int rowsDeleted = pstmt.executeUpdate();

            ArrayList<ChamCong> result = chamCongDAO.getAll();
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
            ArrayList<ChamCong> result = ChamCongDAO.getInstance().getAll();
            assertNull("Khi gặp SQLException, getAll() phải trả về null", result);
        } catch (Exception e) {
            fail("getAll() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }


    @Test
    public void testGetByMaNhanVien_Found() {
        ArrayList<ChamCong> result = chamCongDAO.getByMaNhanVien(EXISTING_MANV);
        assertNotNull("getByMaNhanVien (found) should not return null", result);
        assertTrue("getByMaNhanVien (found) should return records", result.size() > 0);
        for (ChamCong cc : result) {
            assertEquals("All records should have the correct MANHANVIEN", EXISTING_MANV, cc.getMaNhanVien());
        }
        System.out.println("testGetByMaNhanVien_Found: Found " + result.size() + " records for " + EXISTING_MANV);
    }

    @Test
    public void testGetByMaNhanVien_NotFound() {
        ArrayList<ChamCong> result = chamCongDAO.getByMaNhanVien(NON_EXISTING_MANV);
        assertNotNull("getByMaNhanVien (not found) should return empty list, not null", result);
        assertEquals("getByMaNhanVien (not found) should return empty list", 0, result.size());
        System.out.println("testGetByMaNhanVien_NotFound: Found " + result.size() + " records for " + NON_EXISTING_MANV);
    }

    @Test
    public void testGetByMaNhanVien_NullInput() {
        try {
            ArrayList<ChamCong> result = chamCongDAO.getByMaNhanVien(null);
            assertNotNull("getByMaNhanVien (null input) should maybe return empty list", result);
            assertEquals("getByMaNhanVien (null input) should maybe return empty list", 0, result.size());
            System.out.println("testGetByMaNhanVien_NullInput: Handled gracefully.");
        } catch (Exception e) {
            fail("getByMaNhanVien(null) should not throw unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testGetByMaNhanVien_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<ChamCong> result = ChamCongDAO.getInstance().getByMaNhanVien("NV001");
            assertNull("Khi gặp SQLException, getByMaNhanVien() phải trả về null", result);
        } catch (Exception e) {
            fail("getByMaNhanVien() không được ném exception ra ngoài: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }


    @Test
    public void testGetByTenNhanVien_Found() {
        ArrayList<ChamCong> result = chamCongDAO.getByTenNhanVien(EXISTING_TENNV);
        assertNotNull("getByTenNhanVien (found) should not return null", result);
        assertTrue("getByTenNhanVien (found) should return records", result.size() > 0);

        System.out.println("testGetByTenNhanVien_Found: Found " + result.size() + " records for " + EXISTING_TENNV);
    }

    @Test
    public void testGetByTenNhanVien_NotFound() {
        ArrayList<ChamCong> result = chamCongDAO.getByTenNhanVien(NON_EXISTING_TENNV);
        assertNotNull("getByTenNhanVien (not found) should return empty list, not null", result);
        assertEquals("getByTenNhanVien (not found) should return empty list", 0, result.size());
        System.out.println("testGetByTenNhanVien_NotFound: Found " + result.size() + " records for " + NON_EXISTING_TENNV);
    }

    @Test
    public void testGetByTenNhanVien_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<ChamCong> result = ChamCongDAO.getInstance().getByTenNhanVien("Nguyen Van A");
            assertNull("Khi gặp SQLException, getByTenNhanVien() phải trả về null", result);
        } catch (Exception e) {
            fail("getByTenNhanVien() không được ném exception ra ngoài: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }


    @Test
    public void testGetByNgayLamViec_Found() {
        ArrayList<ChamCong> result = chamCongDAO.getByNgayLamViec(EXISTING_DATE);
        assertNotNull("getByNgayLamViec (found) should not return null", result);
        assertTrue("getByNgayLamViec (found) should return records", result.size() > 0);
        for (ChamCong cc : result) {
            assertEquals("All records should have the correct NGAYLAMVIEC", EXISTING_DATE.toString(), cc.getNgayLamViec().toString());
        }
        System.out.println("testGetByNgayLamViec_Found: Found " + result.size() + " records for " + EXISTING_DATE);
    }

    @Test
    public void testGetByNgayLamViec_NotFound() {
        ArrayList<ChamCong> result = chamCongDAO.getByNgayLamViec(NON_EXISTING_DATE);
        assertNotNull("getByNgayLamViec (not found) should return empty list, not null", result);
        assertEquals("getByNgayLamViec (not found) should return empty list", 0, result.size());
        System.out.println("testGetByNgayLamViec_NotFound: Found " + result.size() + " records for " + NON_EXISTING_DATE);
    }

    @Test
    public void testGetByNgayLamViec_NullInput() {
        try {
            ArrayList<ChamCong> result = chamCongDAO.getByNgayLamViec(null);
            assertNotNull("getByNgayLamViec (null input) should maybe return empty list", result);
            assertEquals("getByNgayLamViec (null input) should maybe return empty list", 0, result.size());
            System.out.println("testGetByNgayLamViec_NullInput: Handled gracefully.");
        } catch (Exception e) {
            fail("getByNgayLamViec(null) should not throw unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testGetByNgayLamViec_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<ChamCong> result = ChamCongDAO.getInstance().getByNgayLamViec(Date.valueOf("2024-01-01"));
            assertNull("Khi gặp SQLException, getByNgayLamViec() phải trả về null", result);
        } catch (Exception e) {
            fail("getByNgayLamViec() không được ném exception ra ngoài: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }


    @Test
    public void testGetSoNgayLamViecThang_Found() {
        int result = chamCongDAO.getSoNgayLamViecThang(EXISTING_MANV);
        assertTrue("getSoNgayLamViecThang (found) should return a non-negative count", result >= 0);
        assertEquals("getSoNgayLamViecThang (found) should return correct count", EXPECTED_COUNT_FOR_EXISTING_MANV, result);
        System.out.println("testGetSoNgayLamViecThang_Found: Count for " + EXISTING_MANV + " is " + result);
    }

    @Test
    public void testGetSoNgayLamViecThang_NotFound() {
        int result = chamCongDAO.getSoNgayLamViecThang(NON_EXISTING_MANV);
        assertEquals("getSoNgayLamViecThang (not found) should return 0", 0, result);
        System.out.println("testGetSoNgayLamViecThang_NotFound: Count for " + NON_EXISTING_MANV + " is " + result);
    }

    @Test
    public void testGetSoNgayLamViecThang_NullInput() {
        try {
            int result = chamCongDAO.getSoNgayLamViecThang(null);
            assertEquals("getSoNgayLamViecThang (null input) should return 0", 0, result);
            System.out.println("testGetSoNgayLamViecThang_NullInput: Handled gracefully.");
        } catch (Exception e) {
            fail("getSoNgayLamViecThang(null) should not throw unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testGetSoNgayLamViecThang_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            int result = ChamCongDAO.getInstance().getSoNgayLamViecThang("NV001");
            assertEquals("Khi gặp SQLException, getSoNgayLamViecThang() phải trả về 0", 0, result);
        } catch (Exception e) {
            fail("getSoNgayLamViecThang() không được ném exception ra ngoài: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }


}
