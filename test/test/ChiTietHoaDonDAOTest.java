package test;

import Model.ChamCong;
import Model.ChiTietHoaDon;
import Model.HoaDon;
import dao.ChamCongDAO;
import dao.ChiTietHoaDonDAO;
import dao.HoaDonDAO;
import database.JDBCUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.sql.*;
import java.util.ArrayList;

public class ChiTietHoaDonDAOTest {

    private Connection connection;
    private ChiTietHoaDonDAO chiTietHoaDonDAO;

    private static final String MA_HD_TEST = "HD_TEST999";
    private static final String MA_SP_TEST_1 = "SP001";
    private static final String MA_SP_TEST_2 = "SP002";
    private static final String MA_HD_NON_EXISTENT = "HD_KHONGTONTAI"; // Mã hóa đơn không tồn tại
    private static final String MA_SP_NON_EXISTENT = "SP_KHONGTONTAI"; // Mã sản phẩm không tồn tại

    @Before
    public void setUp() throws SQLException {
        connection = JDBCUtil.getConnection();
        assertNotNull("Không thể kết nối đến cơ sở dữ liệu.", connection);

        chiTietHoaDonDAO = ChiTietHoaDonDAO.getInstance();
        connection.setAutoCommit(false);
        try (PreparedStatement pstmt = connection.prepareStatement("INSERT INTO CHITIETHOADON (MAHOADON, MASANPHAM, SOLUONG) VALUES (?, ?, ?)")) {
            pstmt.setString(1, MA_HD_TEST);
            pstmt.setString(2, MA_SP_TEST_1);
            pstmt.setInt(3, 10);
            pstmt.executeUpdate();
        }

        System.out.println("Setup hoàn tất cho test.");
    }

    @After
    public void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            try {
                connection.rollback();
                System.out.println("Transaction rollback.");
            } catch (SQLException e) {
                System.err.println("Lỗi khi rollback transaction: " + e.getMessage());
            } finally {
                try {
                    connection.close();
                    System.out.println("Connection closed.");
                } catch (SQLException e) {
                    System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
                }
            }
        }
    }

    public void insert_hoadon() throws SQLException {
        HoaDon t = new HoaDon(MA_HD_TEST, "KH01", "NV01", "SK01", Date.valueOf("2025-04-09"), 10000);
        HoaDonDAO hoaDonDAO = HoaDonDAO.getInstance();

        hoaDonDAO.insert(t);
    }

    @Test
    public void testGetInstance() {
        ChiTietHoaDonDAO instance1 = ChiTietHoaDonDAO.getInstance();
        ChiTietHoaDonDAO instance2 = ChiTietHoaDonDAO.getInstance();
        assertNotNull(instance1);
        assertNotSame(instance1, instance2);
    }

    // Test cho phương thức insert()
    @Test
    public void testInsert_Success() throws SQLException {
        insert_hoadon();
        ChiTietHoaDon cthd = new ChiTietHoaDon(MA_HD_TEST, MA_SP_TEST_1, 5);
        int result = chiTietHoaDonDAO.insert(cthd);
        assertTrue("Insert thành công phải trả về số dòng bị ảnh hưởng > 0", result > 0);
        ArrayList<ChiTietHoaDon> details = chiTietHoaDonDAO.getById(MA_HD_TEST);
        assertNotNull(details);
        assertEquals("Phải có 1 chi tiết hóa đơn sau khi insert", 1, details.size());
        assertEquals(MA_SP_TEST_1, details.get(0).getMaSanPham());
        assertEquals(5, details.get(0).getSoLuong());
    }

    @Test
    public void testInsert_Failure_ForeignKeyViolation_MaHoaDonNotExist() {
        ChiTietHoaDon cthd = new ChiTietHoaDon(MA_HD_NON_EXISTENT, MA_SP_TEST_1, 2);

        int result = chiTietHoaDonDAO.insert(cthd);

        assertEquals("Insert thất bại do vi phạm ràng buộc khóa ngoại (MaHoaDon) phải trả về 0", 0, result);
    }

    @Test
    public void testInsert_Failure_ForeignKeyViolation_MaSanPhamNotExist() {
        ChiTietHoaDon cthd = new ChiTietHoaDon(MA_HD_TEST, MA_SP_NON_EXISTENT, 3);

        int result = chiTietHoaDonDAO.insert(cthd);

        assertEquals("Insert thất bại do vi phạm ràng buộc khóa ngoại (MaSanPham) phải trả về 0", 0, result);
    }

     @Test
     public void testInsert_Failure_PrimaryKeyViolation() throws SQLException {
         ChiTietHoaDon cthd1 = new ChiTietHoaDon(MA_HD_TEST, MA_SP_TEST_1, 1);
         chiTietHoaDonDAO.insert(cthd1);

         ChiTietHoaDon cthd2 = new ChiTietHoaDon(MA_HD_TEST, MA_SP_TEST_1, 2);

         int result = chiTietHoaDonDAO.insert(cthd2);

         assertEquals("Insert thất bại do trùng khóa chính phải trả về 0", 0, result);
     }

    @Test
    public void testInsert_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ChiTietHoaDon cthd = new ChiTietHoaDon(MA_HD_TEST, MA_SP_NON_EXISTENT, 3);

            int result = chiTietHoaDonDAO.insert(cthd);
            assertNull("Khi gặp SQLException, insert() phải trả về null", result);
        } catch (Exception e) {
            fail("insert() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // Test cho phương thức getById()
    @Test
    public void testGetById_Success() throws SQLException {

        ArrayList<ChiTietHoaDon> result = chiTietHoaDonDAO.getById(MA_HD_TEST);

        assertNotNull("Kết quả getById không được là null", result);
        assertEquals("Phải tìm thấy 1 chi tiết hóa đơn", 1, result.size());

        ChiTietHoaDon detail = result.get(0);
        assertEquals(MA_HD_TEST, detail.getMaHoaDon());
        assertEquals(MA_SP_TEST_1, detail.getMaSanPham());
        assertEquals(5, detail.getSoLuong());
    }

    @Test
    public void testGetById_NotFound_HoaDonExistsButNoDetails() {
        ArrayList<ChiTietHoaDon> result = chiTietHoaDonDAO.getById(MA_HD_TEST);

        assertNotNull("Kết quả getById không được là null ngay cả khi không tìm thấy", result);
        assertTrue("Danh sách chi tiết phải rỗng khi không có dữ liệu", result.isEmpty());
    }

    @Test
    public void testGetById_NotFound_HoaDonNonExistent() {
        String nonExistentMaHD = "HD_IMPOSSIBLE_ID_12345";

        ArrayList<ChiTietHoaDon> result = chiTietHoaDonDAO.getById(nonExistentMaHD);

        assertNotNull("Kết quả getById không được là null ngay cả khi mã hóa đơn không tồn tại", result);
        assertTrue("Danh sách chi tiết phải rỗng khi mã hóa đơn không tồn tại", result.isEmpty());
    }

    @Test
    public void testGetById_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<ChiTietHoaDon> result = ChiTietHoaDonDAO.getInstance().getById(MA_HD_TEST);
            assertNull("Khi gặp SQLException, getById() phải trả về null", result);
        } catch (Exception e) {
            fail("getById() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }
}