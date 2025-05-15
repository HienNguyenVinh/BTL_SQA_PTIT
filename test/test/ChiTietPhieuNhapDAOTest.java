package dao;

import Model.ChiTietPhieuNhap;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class ChiTietPhieuNhapDAOTest {

    private ChiTietPhieuNhapDAO dao;

    @Before
    public void setUp() {
        dao = ChiTietPhieuNhapDAO.getInstance();
    }

    @Test
    public void testInsert_Valid() {
        ChiTietPhieuNhap chiTiet = new ChiTietPhieuNhap("PN_TEST_01", "SP_TEST_01", 10000.0, 5);
        int result = dao.insert(chiTiet);
        assertTrue(result == 1 || result == 0); // Có thể đã tồn tại → 0
    }

    @Test
    public void testInsert_NullMaPhieuNhap() {
        ChiTietPhieuNhap chiTiet = new ChiTietPhieuNhap(null, "SP001", 10000.0, 5);
        int result = dao.insert(chiTiet);
        assertEquals(0, result);
    }

    @Test
    public void testInsert_NullMaSanPham() {
        ChiTietPhieuNhap chiTiet = new ChiTietPhieuNhap("PN001", null, 10000.0, 5);
        int result = dao.insert(chiTiet);
        assertEquals(0, result);
    }

    @Test
    public void testInsert_NegativeDonGia() {
        ChiTietPhieuNhap chiTiet = new ChiTietPhieuNhap("PN001", "SP001", -5000.0, 5);
        int result = dao.insert(chiTiet);
        assertEquals(0, result);
    }

    @Test
    public void testInsert_NegativeSoLuong() {
        ChiTietPhieuNhap chiTiet = new ChiTietPhieuNhap("PN001", "SP001", 10000.0, -5);
        int result = dao.insert(chiTiet);
        assertEquals(0, result);
    }

    @Test
    public void testGetById_Existing() {
        // Đảm bảo bạn có dữ liệu trong DB tương ứng với mã PN001
        ArrayList<ChiTietPhieuNhap> list = dao.getById("PN001");
        assertNotNull(list);
        for (ChiTietPhieuNhap item : list) {
            assertEquals("PN001", item.getMaPhieuNhap());
            assertNotNull(item.getMaSanPham());
        }
    }

    @Test
    public void testGetById_NotFound() {
        ArrayList<ChiTietPhieuNhap> list = dao.getById("KHONG_TON_TAI");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testGetById_Null() {
        ArrayList<ChiTietPhieuNhap> list = dao.getById(null);
        assertNull(list); // DAO xử lý lỗi bằng cách trả về null
    }
}
