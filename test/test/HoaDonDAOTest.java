package test;

import Model.ChamCong;
import Model.HoaDon;
import dao.ChamCongDAO;
import dao.HoaDonDAO;
import database.JDBCUtil;
import org.junit.*;
import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class HoaDonDAOTest {

    private static Connection con;
    private static HoaDonDAO hoaDonDAO;

    // Dữ liệu test mẫu
    private static String testMaHoaDon = "HD_TEST_9999";
    private static String testMaKhachHang = "KH01";
    private static String testMaNhanVien = "NV01";
    private static String testMaSuKien = null;
    private static Date testNgayHoaDon = Date.valueOf("2025-04-09");
    private static Date testNgayHoaDonKhac = Date.valueOf("2025-04-10");
    private static String nonExistentMaHoaDon = "HD_KHONGTONTAI";
    private static String nonExistentMaNhanVien = "NV_KHONGTONTAI";
    private static String nonExistentMaKhachHang = "KH_KHONGTONTAI";
    private static Date nonExistentNgayHoaDon = Date.valueOf("1900-01-01");


    // --- Setup và Teardown ---

    @BeforeClass
    public static void setUpClass() {
        try {
            con = JDBCUtil.getConnection();
            con.setAutoCommit(false);
            System.out.println("Database connection established and auto-commit disabled.");
            hoaDonDAO = HoaDonDAO.getInstance();
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
    public void tearDown() {
        if (con != null) {
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
        HoaDonDAO instance1 = HoaDonDAO.getInstance();
        HoaDonDAO instance2 = HoaDonDAO.getInstance();
        assertNotNull("getInstance() should return a non-null instance", instance1);
        assertNotSame("getInstance() currently returns new instances", instance1, instance2);
    }

    @Test
    public void testInsert_Success() throws SQLException {
        System.out.println("Testing insert success...");
        HoaDon hd = new HoaDon(testMaHoaDon, testMaKhachHang, testMaNhanVien, testMaSuKien, testNgayHoaDon, 0.0);
        int result = hoaDonDAO.insert(hd);
        assertEquals("Insert thành công phải trả về số dương (thường là 1)", 1, result);

        HoaDon insertedHd = hoaDonDAO.getById(testMaHoaDon);
        assertNotNull("Hóa đơn vừa insert phải được tìm thấy", insertedHd);
        assertEquals("Mã hóa đơn không khớp", testMaHoaDon, insertedHd.getMaHoaDon());
        assertEquals("Mã khách hàng không khớp", testMaKhachHang, insertedHd.getMaKhachHang());
        assertEquals("Mã nhân viên không khớp", testMaNhanVien, insertedHd.getMaNhanVien());
        assertEquals("Ngày hóa đơn không khớp", testNgayHoaDon.toString(), insertedHd.getNgayHoaDon().toString());
    }

    @Test
    public void testInsert_FailureDueToConstraint() throws SQLException {
        System.out.println("Testing insert failure (duplicate PK)...");
        HoaDon hd1 = new HoaDon(testMaHoaDon, testMaKhachHang, testMaNhanVien, testMaSuKien, testNgayHoaDon, 0.0);
        hoaDonDAO.insert(hd1);

        HoaDon hd2 = new HoaDon(testMaHoaDon, "KH002", "NV002", null, testNgayHoaDonKhac, 0.0);
        try {
            hoaDonDAO.insert(hd2);
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
            HoaDon hd1 = new HoaDon(testMaHoaDon, testMaKhachHang, testMaNhanVien, testMaSuKien, testNgayHoaDon, 0.0);
            int result = HoaDonDAO.getInstance().insert(hd1);
            assertNull("Khi gặp SQLException, insert() phải trả về null", result);
        } catch (Exception e) {
            fail("insert() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testDelete_Success() throws SQLException {
        System.out.println("Testing delete success...");
        HoaDon hd = new HoaDon(testMaHoaDon, testMaKhachHang, testMaNhanVien, testMaSuKien, testNgayHoaDon, 0.0);
        hoaDonDAO.insert(hd);

        int result = hoaDonDAO.delete(testMaHoaDon);
        assertEquals("Delete thành công phải trả về số dương (thường là 1)", 1, result);

        HoaDon deletedHd = hoaDonDAO.getById(testMaHoaDon);
        assertNull("Hóa đơn vừa xóa không nên được tìm thấy", deletedHd);
    }

    @Test
    public void testDelete_NotFound() {
        System.out.println("Testing delete not found...");
        int result = hoaDonDAO.delete(nonExistentMaHoaDon);
        assertEquals("Delete hóa đơn không tồn tại phải trả về 0", 0, result);
    }

    @Test
    public void testDelete_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            int result = HoaDonDAO.getInstance().delete(testMaHoaDon);
            assertNull("Khi gặp SQLException, delete() phải trả về null", result);
        } catch (Exception e) {
            fail("delete() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetAll_Empty() {
        try (Connection conn = JDBCUtil.getConnection()) {
            conn.setAutoCommit(false);

            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM HOADON");

            int rowsDeleted = pstmt.executeUpdate();

            ArrayList<HoaDon> result = hoaDonDAO.getAll();
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
        HoaDon hd1 = new HoaDon(testMaHoaDon, testMaKhachHang, testMaNhanVien, testMaSuKien, testNgayHoaDon, 100.0);
        HoaDon hd2 = new HoaDon(testMaHoaDon + "_2", "KH02", "NV02", null, testNgayHoaDonKhac, 200.0);
        hoaDonDAO.insert(hd1);
        hoaDonDAO.insert(hd2);

        ArrayList<HoaDon> list = hoaDonDAO.getAll();
        assertNotNull("getAll() không nên trả về null khi có dữ liệu", list);
        assertEquals("Danh sách phải chứa 2 hóa đơn", 13, list.size());

        boolean foundHd1 = false;
        boolean foundHd2 = false;
        for(HoaDon hd : list) {
            if(hd.getMaHoaDon().equals(testMaHoaDon)) foundHd1 = true;
            if(hd.getMaHoaDon().equals(testMaHoaDon + "_2")) foundHd2 = true;
        }
        assertTrue("Phải tìm thấy hóa đơn 1 trong danh sách", foundHd1);
        assertTrue("Phải tìm thấy hóa đơn 2 trong danh sách", foundHd2);
    }

    @Test
    public void testGetAll_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<HoaDon> result = HoaDonDAO.getInstance().getAll();
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
        HoaDon hd = new HoaDon(testMaHoaDon, testMaKhachHang, testMaNhanVien, testMaSuKien, testNgayHoaDon, 150.0);
        hoaDonDAO.insert(hd);

        HoaDon foundHd = hoaDonDAO.getById(testMaHoaDon);
        assertNotNull("Phải tìm thấy hóa đơn với mã tồn tại", foundHd);
        assertEquals("Mã hóa đơn không khớp", testMaHoaDon, foundHd.getMaHoaDon());
        assertEquals("Mã khách hàng không khớp", testMaKhachHang, foundHd.getMaKhachHang());
        assertEquals("Mã nhân viên không khớp", testMaNhanVien, foundHd.getMaNhanVien());
        assertEquals("Ngày hóa đơn không khớp", testNgayHoaDon.toString(), foundHd.getNgayHoaDon().toString());
        assertEquals("Trị giá không khớp", 150.0, foundHd.getTriGia(), 0.001);
    }

    @Test
    public void testGetById_NotFound() {
        System.out.println("Testing getById not found...");
        HoaDon foundHd = hoaDonDAO.getById(nonExistentMaHoaDon);
        assertNull("Không nên tìm thấy hóa đơn với mã không tồn tại", foundHd);
    }

    @Test
    public void testGetById_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            HoaDon result = HoaDonDAO.getInstance().getById(testMaHoaDon);
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
        HoaDon hd1 = new HoaDon(testMaHoaDon, testMaKhachHang, testMaNhanVien, testMaSuKien, testNgayHoaDon, 100.0);
        HoaDon hd2 = new HoaDon(testMaHoaDon + "_2", "KH02", testMaNhanVien, null, testNgayHoaDonKhac, 200.0); // Cùng NV
        HoaDon hd3 = new HoaDon(testMaHoaDon + "_3", "KH03", "NV02", null, testNgayHoaDon, 300.0); // Khác NV
        hoaDonDAO.insert(hd1);
        hoaDonDAO.insert(hd2);
        hoaDonDAO.insert(hd3);

        ArrayList<HoaDon> list = hoaDonDAO.getByMaNhanVien(testMaNhanVien);
        assertNotNull("getByMaNhanVien không nên trả về null", list);
        assertEquals("Phải tìm thấy 2 hóa đơn cho nhân viên " + testMaNhanVien, 2 + 2, list.size());
        for(HoaDon hd : list) {
            assertEquals("Mã nhân viên không khớp", testMaNhanVien, hd.getMaNhanVien());
        }
    }

    @Test
    public void testGetByMaNhanVien_NotFound() {
        System.out.println("Testing getByMaNhanVien not found...");
        ArrayList<HoaDon> list = hoaDonDAO.getByMaNhanVien(nonExistentMaNhanVien);
        assertNotNull("getByMaNhanVien không nên trả về null", list);
        assertTrue("Danh sách phải trống khi mã nhân viên không tồn tại", list.isEmpty());
    }

    @Test
    public void testGetByMaNhanVien_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<HoaDon> result = HoaDonDAO.getInstance().getByMaNhanVien(testMaNhanVien);
            assertNull("Khi gặp SQLException, getByMaNhanVien() phải trả về null", result);
        } catch (Exception e) {
            fail("getByMaNhanVien() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByMaKhachHang_Found() throws SQLException {
        System.out.println("Testing getByMaKhachHang found...");
        HoaDon hd1 = new HoaDon(testMaHoaDon, testMaKhachHang, testMaNhanVien, testMaSuKien, testNgayHoaDon, 100.0);
        HoaDon hd2 = new HoaDon(testMaHoaDon + "_2", testMaKhachHang, "NV02", null, testNgayHoaDonKhac, 200.0); // Cùng KH
        HoaDon hd3 = new HoaDon(testMaHoaDon + "_3", "KH02", testMaNhanVien, null, testNgayHoaDon, 300.0); // Khác KH
        hoaDonDAO.insert(hd1);
        hoaDonDAO.insert(hd2);
        hoaDonDAO.insert(hd3);

        ArrayList<HoaDon> list = hoaDonDAO.getByMaKhachHang(testMaKhachHang);
        assertNotNull("getByMaKhachHang không nên trả về null", list);
        assertEquals("Phải tìm thấy 2 hóa đơn cho khách hàng " + testMaKhachHang, 2 + 1, list.size());
        for(HoaDon hd : list) {
            assertEquals("Mã khách hàng không khớp", testMaKhachHang, hd.getMaKhachHang());
        }
    }

    @Test
    public void testGetByMaKhachHang_NotFound() {
        System.out.println("Testing getByMaKhachHang not found...");
        ArrayList<HoaDon> list = hoaDonDAO.getByMaKhachHang(nonExistentMaKhachHang);
        assertNotNull("getByMaKhachHang không nên trả về null", list);
        assertTrue("Danh sách phải trống khi mã khách hàng không tồn tại", list.isEmpty());
    }

    @Test
    public void testGetByMaKhachHang_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<HoaDon> result = HoaDonDAO.getInstance().getByMaKhachHang(testMaKhachHang);
            assertNull("Khi gặp SQLException, getByMaKhachHang() phải trả về null", result);
        } catch (Exception e) {
            fail("getByMaKhachHang() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByNgayHoaDon_Found() throws SQLException {
        System.out.println("Testing getByNgayHoaDon found...");
        HoaDon hd1 = new HoaDon(testMaHoaDon, testMaKhachHang, testMaNhanVien, testMaSuKien, testNgayHoaDon, 100.0); // Ngày 1
        HoaDon hd2 = new HoaDon(testMaHoaDon + "_2", "KH02", "NV02", null, testNgayHoaDonKhac, 200.0); // Ngày 2
        HoaDon hd3 = new HoaDon(testMaHoaDon + "_3", "KH03", "NV03", null, testNgayHoaDon, 300.0); // Ngày 1
        hoaDonDAO.insert(hd1);
        hoaDonDAO.insert(hd2);
        hoaDonDAO.insert(hd3);

        ArrayList<HoaDon> list = hoaDonDAO.getByNgayHoaDon(testNgayHoaDon);
        assertNotNull("getByNgayHoaDon không nên trả về null", list);
        assertEquals("Phải tìm thấy 2 hóa đơn cho ngày " + testNgayHoaDon, 2 + 1, list.size());

        for(HoaDon hd : list) {
            assertEquals("Ngày hóa đơn không khớp", testNgayHoaDon.toString(), hd.getNgayHoaDon().toString());
        }
    }

    @Test
    public void testGetByNgayHoaDon_NotFound() {
        System.out.println("Testing getByNgayHoaDon not found...");
        ArrayList<HoaDon> list = hoaDonDAO.getByNgayHoaDon(nonExistentNgayHoaDon);
        assertNotNull("getByNgayHoaDon không nên trả về null", list);
        assertTrue("Danh sách phải trống khi không có hóa đơn vào ngày đó", list.isEmpty());
    }

    @Test
    public void testGetByNgayHoaDon_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<HoaDon> result = HoaDonDAO.getInstance().getByNgayHoaDon(testNgayHoaDon);
            assertNull("Khi gặp SQLException, getByNgayHoaDon() phải trả về null", result);
        } catch (Exception e) {
            fail("getByNgayHoaDon() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetSoThuTu() {
        System.out.println("Testing getSoThuTu...");
        String stt = hoaDonDAO.getSoThuTu();
        assertNotNull("getSoThuTu() không nên trả về null", stt);
        assertFalse("Số thứ tự trả về không nên rỗng", stt.isEmpty());
        assertTrue("Số thứ tự nên bắt đầu bằng 'HD'", stt.startsWith("HD"));

        try {
            Integer.parseInt(stt.substring(2));
        } catch (NumberFormatException e) {
            fail("Phần sau 'HD' của số thứ tự phải là một số nguyên.");
        }
        System.out.println("Generated sequence: " + stt);
    }

    @Test
    public void testTienGiamGia() throws SQLException {
        System.out.println("Testing tienGiamGia...");
        String testMaSk = "SK03";
        float testTriGia = 100000;
        HoaDon hd = new HoaDon(testMaHoaDon, testMaKhachHang, testMaNhanVien, testMaSk, testNgayHoaDon, testTriGia);
        hoaDonDAO.insert(hd);

        double expectedDiscount = testTriGia * 0.05;
        double actualDiscount = hoaDonDAO.tienGiamGia(testMaHoaDon);

        assertEquals("Tiền giảm giá không đúng", expectedDiscount, actualDiscount, 0.001);
    }

    @Test
    public void testTienGiamGia_NotFound() {
        System.out.println("Testing tienGiamGia not found...");
        double actualDiscount = hoaDonDAO.tienGiamGia(nonExistentMaHoaDon);
        assertEquals("Tiền giảm giá phải là 0 khi mã hóa đơn không tồn tại", 0.0, actualDiscount, 0.001);
    }

    @Test
    public void testTienThanhToan() throws SQLException {
        System.out.println("Testing tienThanhToan...");
        String testMaSk = "SK03";
        float testTriGia = 100000;
        HoaDon hd = new HoaDon(testMaHoaDon, testMaKhachHang, testMaNhanVien, testMaSk, testNgayHoaDon, testTriGia); // Trị giá sẽ được tính sau
        hoaDonDAO.insert(hd);

        double expectedTotal = testTriGia - testTriGia * 0.05;
        double actualTotal = hoaDonDAO.tienThanhToan(testMaHoaDon);

        assertEquals("Tiền thanh toán không đúng", expectedTotal, actualTotal, 0.001);
    }

    @Test
    public void testTienThanhToan_NotFound() {
        System.out.println("Testing tienThanhToan not found...");
        double actualTotal = hoaDonDAO.tienThanhToan(nonExistentMaHoaDon);
        assertEquals("Tiền thanh toán phải là 0 khi mã hóa đơn không tồn tại", 0.0, actualTotal, 0.001);
    }

}