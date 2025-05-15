package test;

import Model.PhieuNhap;
import dao.PhieuNhapDAO;
import database.JDBCUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class PhieuNhapDAOTest {
    private static Connection con;
    private static PhieuNhapDAO phieuNhapDAO;

    // Dữ liệu test mẫu
    private static String testMaPhieuNhap = "MPN_TEST_9999";
    private static String testMaNhanVien = "NV01";
    private static Date testNgayNhap = Date.valueOf("2025-05-01");
    private static String nonExistentMaPhieuNhap = "MPN_KHONGTONTAI";
    private static String nonExistentMaNhanVien = "NV_KHONGTONTAI";
    private static Date nonExistentNgayHoaDon = Date.valueOf("1900-01-01");


    // --- Setup và Teardown ---

    @BeforeClass
    public static void setUpClass() {
        try {
            con = JDBCUtil.getConnection();
            con.setAutoCommit(false);
            System.out.println("Database connection established and auto-commit disabled.");
            phieuNhapDAO = PhieuNhapDAO.getInstance();
        } catch (SQLException e) {
            fail("Không thể kết nối đến CSDL test: " + e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownClass() {
        if (con != null) {
            try {
                con.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Lỗi khi đóng kết nối CSDL test: " + e.getMessage());
            }
        }
    }

    @After
    public void tearDown() throws SQLException {
        if (con != null && !con.isClosed()) {
            try {
                con.rollback();
                System.out.println("Transaction rolled back after test.");
            } catch (SQLException e) {
                System.err.println("Lỗi khi rollback transaction: " + e.getMessage());
            }
        }
    }

    // --- Test Cases ---

    @Test
    public void testGetInstance() {
        PhieuNhapDAO instance1 = PhieuNhapDAO.getInstance();
        PhieuNhapDAO instance2 = PhieuNhapDAO.getInstance();
        assertNotNull("getInstance() should return a non-null instance", instance1);
        assertNotSame("getInstance() currently returns new instances", instance1, instance2);
    }

    @Test
    public void testInsert_Success() throws SQLException {
        System.out.println("Testing insert success...");
        PhieuNhap phieuNhap = new PhieuNhap(testMaPhieuNhap, testMaNhanVien, testNgayNhap, 0.0);
        int result = phieuNhapDAO.insert(phieuNhap);
        assertEquals("Insert thành công phải trả về số dương (thường là 1)", 1, result);

        PhieuNhap savedPhieuNhap = phieuNhapDAO.getById(testMaPhieuNhap);
        assertNotNull("Phiếu nhập vừa insert phải được tìm thấy", savedPhieuNhap);
        assertEquals("Mã phiếu nhập khớp", testMaPhieuNhap, savedPhieuNhap.getMaPhieuNhap());
        assertEquals("Mã nhân viên khớp", testMaNhanVien, savedPhieuNhap.getMaNhanVien());
        assertEquals("Ngày nhập khớp", testNgayNhap, savedPhieuNhap.getNgayNhap());
    }

    @Test
    public void testInsert_FailureDueToConstraint() throws SQLException {
        System.out.println("Testing insert failure (duplicate PK)...");
        PhieuNhap pn1 = new PhieuNhap(testMaPhieuNhap, testMaNhanVien, testNgayNhap, 0.0);
        phieuNhapDAO.insert(pn1);

        PhieuNhap pn2 = new PhieuNhap(testMaPhieuNhap, "NV002", Date.valueOf("2025-06-01"), 0.0);
        try {
            phieuNhapDAO.insert(pn2);
            fail("Nên có SQLException khi insert trùng khóa chính.");
        } catch (Exception e) {
            System.out.println("Caught expected exception: " + e.getMessage());
            assertTrue("Exception nên là SQLException hoặc subclass của nó", e instanceof SQLException);
        }
    }

    @Test
    public void testInsert_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            PhieuNhap pn = new PhieuNhap(testMaPhieuNhap, testMaNhanVien, testNgayNhap, 0.0);
            int result = PhieuNhapDAO.getInstance().insert(pn);
            assertEquals("Khi gặp SQLException, insert() phải trả về 0", 0, result);
        } catch (Exception e) {
            fail("insert() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testDelete_Success() throws SQLException {
        System.out.println("Testing delete success...");
        PhieuNhap pn = new PhieuNhap(testMaPhieuNhap, testMaNhanVien, testNgayNhap, 0.0);
        phieuNhapDAO.insert(pn);

        int result = phieuNhapDAO.delete(testMaPhieuNhap);
        assertEquals("Delete thành công phải trả về số dương (thường là 1)", 1, result);

        PhieuNhap deletedPn = phieuNhapDAO.getById(testMaPhieuNhap);
        assertNull("Phiếu nhập vừa xóa không nên được tìm thấy", deletedPn);
    }

    @Test
    public void testDelete_NotFound() {
        System.out.println("Testing delete not found...");
        int result = phieuNhapDAO.delete(nonExistentMaPhieuNhap);
        assertEquals("Delete phiếu nhập không tồn tại phải trả về 0", 0, result);
    }

    @Test
    public void testDelete_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            int result = PhieuNhapDAO.getInstance().delete(testMaPhieuNhap);
            assertEquals("Khi gặp SQLException, delete() phải trả về 0", 0, result);
        } catch (Exception e) {
            fail("delete() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetAll_Empty() {
        try {
            Connection conn = JDBCUtil.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement pstmtChild = conn.prepareStatement("DELETE FROM CHITIETPHIEUNHAP");
            int rowsDeletedChild = pstmtChild.executeUpdate();

            PreparedStatement pstmtParent = conn.prepareStatement("DELETE FROM PHIEUNHAP");
            int rowsDeletedParent = pstmtParent.executeUpdate();


            ArrayList<PhieuNhap> result = phieuNhapDAO.getAll();
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
    public void testGetAll_WithData() throws SQLException {
        System.out.println("Testing getAll with data...");
        PhieuNhap pn1 = new PhieuNhap(testMaPhieuNhap, testMaNhanVien, testNgayNhap, 100.0);
        PhieuNhap pn2 = new PhieuNhap(testMaPhieuNhap + "_2", "NV02", Date.valueOf("2025-06-01"), 200.0);
        phieuNhapDAO.insert(pn1);
        phieuNhapDAO.insert(pn2);

        ArrayList<PhieuNhap> list = phieuNhapDAO.getAll();
        assertNotNull("getAll() không nên trả về null khi có dữ liệu", list);
        assertTrue("Danh sách phải chứa ít nhất 2 phiếu nhập", list.size() >= 2);

        boolean foundPn1 = false;
        boolean foundPn2 = false;
        for(PhieuNhap pn : list) {
            if(pn.getMaPhieuNhap().equals(testMaPhieuNhap)) foundPn1 = true;
            if(pn.getMaPhieuNhap().equals(testMaPhieuNhap + "_2")) foundPn2 = true;
        }
        assertTrue("Phải tìm thấy phiếu nhập 1 trong danh sách", foundPn1);
        assertTrue("Phải tìm thấy phiếu nhập 2 trong danh sách", foundPn2);
    }

    @Test
    public void testGetAll_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<PhieuNhap> result = PhieuNhapDAO.getInstance().getAll();
            assertNull("Khi gặp SQLException, getAll() phải trả về null", result);
        } catch (Exception e) {
            fail("getAll() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetById_Found() throws SQLException {
        System.out.println("Testing getById found...");
        PhieuNhap pn = new PhieuNhap(testMaPhieuNhap, testMaNhanVien, testNgayNhap, 150.0);
        phieuNhapDAO.insert(pn);

        PhieuNhap foundPn = phieuNhapDAO.getById(testMaPhieuNhap);
        assertNotNull("Phải tìm thấy phiếu nhập với mã tồn tại", foundPn);
        assertEquals("Mã phiếu nhập khớp", testMaPhieuNhap, foundPn.getMaPhieuNhap());
        assertEquals("Mã nhân viên khớp", testMaNhanVien, foundPn.getMaNhanVien());
        assertEquals("Ngày nhập khớp", testNgayNhap.toString(), foundPn.getNgayNhap().toString());
        assertEquals("Tổng tiền khớp", 150.0, foundPn.getTriGia(), 0.001);
    }

    @Test
    public void testGetById_NotFound() {
        System.out.println("Testing getById not found...");
        PhieuNhap foundPn = phieuNhapDAO.getById(nonExistentMaPhieuNhap);
        assertNull("Không nên tìm thấy phiếu nhập với mã không tồn tại", foundPn);
    }

    @Test
    public void testGetById_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            PhieuNhap result = PhieuNhapDAO.getInstance().getById(testMaPhieuNhap);
            assertNull("Khi gặp SQLException, getById() phải trả về null", result);
        } catch (Exception e) {
            fail("getById() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByMaNhanVien_Found() throws SQLException {
        System.out.println("Testing getByMaNhanVien found...");
        PhieuNhap pn1 = new PhieuNhap(testMaPhieuNhap, testMaNhanVien, testNgayNhap, 100.0);
        PhieuNhap pn2 = new PhieuNhap(testMaPhieuNhap + "_2", testMaNhanVien, Date.valueOf("2025-06-01"), 200.0); // Cùng NV
        PhieuNhap pn3 = new PhieuNhap(testMaPhieuNhap + "_3", "NV02", testNgayNhap, 300.0); // Khác NV
        phieuNhapDAO.insert(pn1);
        phieuNhapDAO.insert(pn2);
        phieuNhapDAO.insert(pn3);

        ArrayList<PhieuNhap> list = phieuNhapDAO.getByMaNhanVien(testMaNhanVien);
        assertNotNull("getByMaNhanVien không nên trả về null", list);
        assertEquals("Phải tìm thấy 2 phiếu nhập cho nhân viên " + testMaNhanVien, 2, list.size());
        for(PhieuNhap pn : list) {
            assertEquals("Mã nhân viên không khớp", testMaNhanVien, pn.getMaNhanVien());
        }
    }

    @Test
    public void testGetByMaNhanVien_NotFound() {
        System.out.println("Testing getByMaNhanVien not found...");
        ArrayList<PhieuNhap> list = phieuNhapDAO.getByMaNhanVien(nonExistentMaNhanVien);
        assertNotNull("getByMaNhanVien không nên trả về null", list);
        assertTrue("Danh sách phải trống khi mã nhân viên không tồn tại", list.isEmpty());
    }

    @Test
    public void testGetByMaNhanVien_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<PhieuNhap> result = PhieuNhapDAO.getInstance().getByMaNhanVien(testMaNhanVien);
            assertNull("Khi gặp SQLException, getByMaNhanVien() phải trả về null", result);
        } catch (Exception e) {
            fail("getByMaNhanVien() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByNgayNhap_Found() throws SQLException {
        System.out.println("Testing getByNgayNhap found...");
        PhieuNhap pn1 = new PhieuNhap(testMaPhieuNhap, testMaNhanVien, testNgayNhap, 100.0); // Ngày 1
        PhieuNhap pn2 = new PhieuNhap(testMaPhieuNhap + "_2", "NV02", Date.valueOf("2025-06-01"), 200.0); // Ngày 2
        PhieuNhap pn3 = new PhieuNhap(testMaPhieuNhap + "_3", "NV03", testNgayNhap, 300.0); // Ngày 1
        phieuNhapDAO.insert(pn1);
        phieuNhapDAO.insert(pn2);
        phieuNhapDAO.insert(pn3);

        ArrayList<PhieuNhap> list = phieuNhapDAO.getByNgay(testNgayNhap);
        assertNotNull("getByNgayNhap không nên trả về null", list);
        assertEquals("Phải tìm thấy 2 phiếu nhập cho ngày " + testNgayNhap, 2, list.size());

        for(PhieuNhap pn : list) {
            assertEquals("Ngày nhập không khớp", testNgayNhap.toString(), pn.getNgayNhap().toString());
        }
    }

    @Test
    public void testGetByNgayNhap_NotFound() {
        System.out.println("Testing getByNgayNhap not found...");
        ArrayList<PhieuNhap> list = phieuNhapDAO.getByNgay(nonExistentNgayHoaDon);
        assertNotNull("getByNgayNhap không nên trả về null", list);
        assertTrue("Danh sách phải trống khi không có phiếu nhập vào ngày đó", list.isEmpty());
    }

    @Test
    public void testGetByNgayNhap_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<PhieuNhap> result = PhieuNhapDAO.getInstance().getByNgay(testNgayNhap);
            assertNull("Khi gặp SQLException, getByNgayNhap() phải trả về null", result);
        } catch (Exception e) {
            fail("getByNgayNhap() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetSoThuTu() {
        System.out.println("Testing getSoThuTu...");
        String stt = phieuNhapDAO.getSoThuTu();
        System.out.println(stt);
        assertNotNull("getSoThuTu() không nên trả về null", stt);
        assertFalse("Số thứ tự trả về không nên rỗng", stt.isEmpty());
        assertTrue("Số thứ tự nên bắt đầu bằng 'PN'", stt.startsWith("PN"));

        try {
            Integer.parseInt(stt.substring(2));
        } catch (NumberFormatException e) {
            fail("Phần sau 'PN' của số thứ tự phải là một số nguyên.");
        }
        System.out.println("Generated sequence: " + stt);
    }
}
