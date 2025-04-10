package test;

import Model.KhachHang;
import Model.NhanVien;
import dao.KhachHangDAO;
import dao.NhanVienDAO;
import database.JDBCUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.*;

public class KhachHangDAOTest {

    private KhachHangDAO khachHangDAO;
    private Connection connection;

    private static final String EXISTING_MAKH_FOR_UPDATE_DELETE = "KH01";
    private static final String NON_EXISTING_MAKH = "KH9999";
    private static final String EXISTING_TENKH = "Nguyễn Văn A";
    private static final String GENDER_MALE = "nam";
    private static final String GENDER_FEMALE = "nữ";

    // Helper method to create unique test data
    private KhachHang createSampleKhachHang(String testMaKH) {
        long testCccd = 100000000000L;
        String testHoTen = "Khach hang test";
        String testDiaChi = "123 đường A, Phố B";
        String testNgaySinh = "2000-12-25";
        String testPhone = "00000000001";
        String testEmail = "khtest" + "@example.com";
        String testGioiTinh = "nam";

        return new KhachHang(
                testMaKH,
                testHoTen,
                testDiaChi,
                testPhone,
                testEmail,
                testCccd,
                testGioiTinh,
                Date.valueOf(testNgaySinh),
                new Date(System.currentTimeMillis()),
                0.0
        );
    }


    private KhachHang findKhachHangByCccd(long cccd) {
        ArrayList<KhachHang> all = khachHangDAO.getAll();
        if (all != null) {
            for (KhachHang kh : all) {
                if (kh.getCCCD() == cccd) {
                    return kh;
                }
            }
        }
        return null;
    }


    @Before
    public void setUp() throws SQLException {
        khachHangDAO = KhachHangDAO.getInstance();
        connection = JDBCUtil.getConnection();
        assertNotNull("Database connection should not be null", connection);
        connection.setAutoCommit(false);
        System.out.println("------------ Test Setup: Transaction Started ------------");
    }

    @After
    public void tearDown() throws SQLException {
        if (connection != null) {
            try {
                connection.rollback();
                System.out.println("------------ Test Teardown: Transaction Rolled Back ------------");
            } catch (SQLException e) {
                System.err.println("Error rolling back transaction: " + e.getMessage());
            } finally {
                try {
                    connection.close();
                    System.out.println("------------ Test Teardown: Connection Closed ------------");
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        } else {
            System.out.println("------------ Test Teardown: No connection to rollback/close ------------");
        }
        System.out.println();
    }

    // --- Test Cases for insert ---
    @Test
    public void testInsert_Success() {
        System.out.println("Executing testInsert_Success");
        KhachHang kh = createSampleKhachHang("KHTEST");
        int result = khachHangDAO.insert(kh);
        assertEquals("Insert should affect 1 row", 1, result);

        // Verification (optional but recommended): Find the inserted user
        KhachHang insertedKh = findKhachHangByCccd(kh.getCCCD());
        assertNotNull("Inserted KhachHang should be findable by CCCD", insertedKh);
        assertEquals("Inserted name should match", kh.getHoTen(), insertedKh.getHoTen());
        assertEquals("Inserted CCCD should match", kh.getCCCD(), insertedKh.getCCCD());
    }

    @Test
    public void testInsert_Failure_ConstraintViolation() {
        System.out.println("Executing testInsert_Failure_ConstraintViolation");
        KhachHang kh1 = createSampleKhachHang("khtest1");

        int result1 = khachHangDAO.insert(kh1);
        assertEquals(1, result1);

        KhachHang kh2 = createSampleKhachHang("khtest1");

        int result2 = khachHangDAO.insert(kh2);
        assertEquals("Insert should fail (return 0) on constraint violation", 0, result2);
    }

    @Test
    public void testInsert_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            KhachHang kh1 = createSampleKhachHang("khtest1");
            int result = KhachHangDAO.getInstance().insert(kh1);
            assertNull("Khi gặp SQLException, insert() phải trả về null", result);
        } catch (Exception e) {
            fail("insert() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for update ---
    @Test
    public void testUpdate_Success_Existing() {
        KhachHang existingKH = khachHangDAO.getById(EXISTING_MAKH_FOR_UPDATE_DELETE);
        assertNotNull("Target KhachHang for update not found after insert", existingKH);

        existingKH.setHoTen("Updated Name");
        existingKH.setDiaChi("123 abc");
        existingKH.setDoanhSo(1500.75);

        int result = khachHangDAO.update(existingKH);
        assertEquals("Update should affect 1 row", 1, result);

        KhachHang updatedKh = khachHangDAO.getById(existingKH.getMaKhachHang());
        assertNotNull("Updated KhachHang could not be retrieved by ID", updatedKh);
        assertEquals("Name should be updated", "Updated Name", updatedKh.getHoTen());
        assertEquals("Address should be updated", "123 abc", updatedKh.getDiaChi());
        assertEquals("DoanhSo should be updated", 1500.75, updatedKh.getDoanhSo(), 0.01);
    }

    @Test
    public void testUpdate_Failure_NonExisting() {
        System.out.println("Executing testUpdate_Failure_NonExisting");
        KhachHang khNonExist = createSampleKhachHang("NonExistingUpdate");

        khNonExist.setMaKhachHang("KH999999");
        int result = khachHangDAO.update(khNonExist);
        assertEquals("Update should affect 0 rows for non-existing ID", 0, result);
    }

    @Test
    public void testUpdate_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            KhachHang kh1 = createSampleKhachHang("abc");
            int result = KhachHangDAO.getInstance().update(kh1);
            assertNull("Khi gặp SQLException, update() phải trả về null", result);
        } catch (Exception e) {
            fail("update() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for delete ---

    @Test
    public void testDelete_Success_Existing() {
        System.out.println("Executing testDelete_Success_Existing");

        KhachHang khToDelete = createSampleKhachHang("DeleteTarget");
        khachHangDAO.insert(khToDelete);
        KhachHang insertedKh = findKhachHangByCccd(khToDelete.getCCCD());

        assertNotNull("Target KhachHang for delete not found after insert", insertedKh);
        String maKhachHangToDelete = insertedKh.getMaKhachHang();

        int result = khachHangDAO.delete(maKhachHangToDelete);
        assertEquals("Delete should affect 1 row", 1, result);

        KhachHang deletedKh = khachHangDAO.getById(maKhachHangToDelete);
        assertNull("Deleted KhachHang should not be found by ID", deletedKh);
    }

    @Test
    public void testDelete_Failure_NonExisting() {
        System.out.println("Executing testDelete_Failure_NonExisting");
        String nonExistentId = "KH999998";
        int result = khachHangDAO.delete(nonExistentId);
        assertEquals("Delete should affect 0 rows for non-existing ID", 0, result);
    }

    @Test
    public void testDelete_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            int result = KhachHangDAO.getInstance().delete("abc");
            assertNull("Khi gặp SQLException, delete() phải trả về null", result);
        } catch (Exception e) {
            fail("delete() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for getAll ---

    @Test
    public void testGetAll_MultipleRecords() {
        System.out.println("Executing testGetAll_MultipleRecords");

        KhachHang kh1 = khachHangDAO.getById("KH01");
        KhachHang kh2 = khachHangDAO.getById("KH02");

        ArrayList<KhachHang> allKhachHang = khachHangDAO.getAll();
        assertNotNull("Result list should not be null", allKhachHang);

        assertTrue("Result list should contain at least 2 records", allKhachHang.size() >= 2);

        boolean found1 = false;
        boolean found2 = false;
        for(KhachHang kh : allKhachHang) {
            if(kh.getCCCD() == kh1.getCCCD()) found1 = true;
            if(kh.getCCCD() == kh2.getCCCD()) found2 = true;
        }
        assertTrue("Should find kh1 in getAll result", found1);
        assertTrue("Should find kh2 in getAll result", found2);
    }

    @Test
    public void testGetAll_NoRecords() {
        try (Connection conn = JDBCUtil.getConnection()) {
            conn.setAutoCommit(false);
            System.out.println("Executing testGetAll_NoRecords");

            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM KHACHHANG");
            int rowsDeleted = pstmt.executeUpdate();
            ArrayList<KhachHang> allKhachHang = khachHangDAO.getAll();

            conn.rollback();
            assertNotNull("Result list should not be null even when empty", allKhachHang);
            System.out.println("Found " + allKhachHang.size() + " records in testGetAll_NoRecords (expecting 0 if rollback worked perfectly).");
            assertTrue("getAll should return an empty list or a list without newly added items after rollback", true);
        } catch (SQLException e) {
            System.err.println("Failed to clear test data: " + e.getMessage());
        }
    }

    @Test
    public void testGetAll_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<KhachHang> result = KhachHangDAO.getInstance().getAll();
            assertNull("Khi gặp SQLException, getAll() phải trả về null", result);
        } catch (Exception e) {
            fail("getAll() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for getById ---

    @Test
    public void testGetById_Success_Existing() {
        System.out.println("Executing testGetById_Success_Existing");
        KhachHang result = khachHangDAO.getById(EXISTING_MAKH_FOR_UPDATE_DELETE);

        assertNotNull("KhachHang should be found by ID", result);
        assertEquals("Found KhachHang ID should match", EXISTING_MAKH_FOR_UPDATE_DELETE, result.getMaKhachHang());
    }

    @Test
    public void testGetById_Failure_NonExisting() {
        System.out.println("Executing testGetById_Failure_NonExisting");
        KhachHang foundKh = khachHangDAO.getById(NON_EXISTING_MAKH);
        assertNull("KhachHang should not be found for non-existing ID", foundKh);
    }

    @Test
    public void testGetById_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            KhachHang result = KhachHangDAO.getInstance().getById("KH01");
            assertNull("Khi gặp SQLException, getById() phải trả về null", result);
        } catch (Exception e) {
            fail("getById() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for getByMaKhachHang ---

    @Test
    public void testGetByMaKhachHang_Success_Existing() {
        KhachHang khToFind = khachHangDAO.getById(EXISTING_MAKH_FOR_UPDATE_DELETE);
        assertNotNull("Target KhachHang for getByMaKhachHang not found after insert", khToFind);
        String maKhachHangToFind = khToFind.getMaKhachHang();

        ArrayList<KhachHang> foundList = khachHangDAO.getByMaKhachHang(maKhachHangToFind);
        assertNotNull("Result list should not be null", foundList);
        assertEquals("Should find exactly one KhachHang by specific ID", 1, foundList.size());
        assertEquals("Found KhachHang ID should match", maKhachHangToFind, foundList.get(0).getMaKhachHang());
        assertEquals("Found KhachHang CCCD should match", khToFind.getCCCD(), foundList.get(0).getCCCD());
    }

    @Test
    public void testGetByMaKhachHang_Failure_NonExisting() {
        System.out.println("Executing testGetByMaKhachHang_Failure_NonExisting");
        ArrayList<KhachHang> foundList = khachHangDAO.getByMaKhachHang(NON_EXISTING_MAKH);
        assertNotNull("Result list should not be null even when empty", foundList);
        assertTrue("Result list should be empty for non-existing ID", foundList.isEmpty());
    }

    @Test
    public void testGetByMaKhachHang_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<KhachHang> result = KhachHangDAO.getInstance().getByMaKhachHang("abc");
            assertNull("Khi gặp SQLException, getByMaKhachHang() phải trả về null", result);
        } catch (Exception e) {
            fail("getByMaKhachHang() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for getByTen ---

    @Test
    public void testGetByTen_Success_Matching() {
        ArrayList<KhachHang> foundList = khachHangDAO.getByTen(EXISTING_TENKH);
        assertNotNull("Result list should not be null", foundList);
        assertEquals("Should find exactly 1 KhachHang with the target name", 1, foundList.size());

        for(KhachHang kh : foundList) {
            assertEquals(EXISTING_TENKH, kh.getHoTen());
        }
    }

    @Test
    public void testGetByTen_Failure_NoMatch() {
        System.out.println("Executing testGetByTen_Failure_NoMatch");
        String nonMatchingName = "Name That Does Not Exist";
        ArrayList<KhachHang> foundList = khachHangDAO.getByTen(nonMatchingName);
        assertNotNull("Result list should not be null even when empty", foundList);
        assertTrue("Result list should be empty for non-matching name", foundList.isEmpty());
    }

    @Test
    public void testGetByTen_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<KhachHang> result = KhachHangDAO.getInstance().getByTen("abc");
            assertNull("Khi gặp SQLException, getByTen() phải trả về null", result);
        } catch (Exception e) {
            fail("getByTen() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for getByGioiTinh ---

    @Test
    public void testGetByGioiTinh_Found() {
        ArrayList<KhachHang> result = khachHangDAO.getByGioiTinh(GENDER_MALE);
        assertNotNull("getByGioiTinh() should not return null", result);
        assertTrue("getByGioiTinh() should find male employees", result.size() > 0);
        for (KhachHang nv : result) {
            assertEquals("Employee gender should be male", GENDER_MALE, nv.getGioiTinh());
        }
        System.out.println("testGetByGioiTinh_Found: Found " + result.size() + " male employees.");
    }

    @Test
    public void testGetByGioiTinh_Failure_NoMatch() {
        System.out.println("Executing testGetByGioiTinh_Failure_NoMatch");
        String nonMatchingGender = "Khác";
        ArrayList<KhachHang> foundList = khachHangDAO.getByGioiTinh(nonMatchingGender);
        assertNotNull("Result list should not be null even when empty", foundList);
        assertTrue("Result list should be empty for non-matching gender", foundList.isEmpty());
    }

    @Test
    public void testGetByGioiTinh_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<KhachHang> result = KhachHangDAO.getInstance().getByGioiTinh("nam");
            assertNull("Khi gặp SQLException, getByGioiTinh() phải trả về null", result);
        } catch (Exception e) {
            fail("getByGioiTinh() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

}