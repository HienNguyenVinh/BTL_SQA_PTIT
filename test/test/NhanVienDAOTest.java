/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package test;

import Model.ChamCong;
import Model.NhanVien;
import Model.SanPham;
import Model.TienLuong;
import dao.NhanVienDAO;
import database.JDBCUtil;
import org.junit.*;
import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;


public class NhanVienDAOTest {

    private static Connection connection;
    private NhanVienDAO nhanVienDAO;


    private static final String EXISTING_MANV_FOR_UPDATE_DELETE = "NV02";
    private static final String NON_EXISTING_MANV = "NV999";
    private static final String EXISTING_TENNV = "Tên Đã Được Cập Nhật";
    private static final String GENDER_MALE = "nam";
    private static final String GENDER_FEMALE = "nữ";
    private static final String EXISTING_CHUCVU = "Nhân viên bán hàng";
    private static final String NON_EXISTING_CHUCVU = "Giám đốc vũ trụ";
    private static final int TEST_MONTH = 4;
    private static final int TEST_YEAR = 2025;

    private NhanVien sampleNhanVien;
    private String SAMPLE_ID = "NV9999";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.out.println("Starting NhanVienDAO Tests...");
        try (Connection conn = JDBCUtil.getConnection()) {
            assertNotNull("Failed to get initial DB connection for setup.", conn);
        } catch (SQLException e) {
            System.err.println("Database connection error during class setup: " + e.getMessage());
            Assume.assumeTrue("Database connection required for tests", false);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        System.out.println("Finished NhanVienDAO Tests.");
    }

    @Before
    public void setUp() throws Exception {
        nhanVienDAO = NhanVienDAO.getInstance();
        assertNotNull("NhanVienDAO instance should not be null", nhanVienDAO);

        sampleNhanVien = createSampleNhanVien(SAMPLE_ID);

        try {
            connection = JDBCUtil.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            System.err.println("Failed to get connection or set autoCommit=false for test: " + e.getMessage());
            Assume.assumeTrue("Database connection required for this test", false);
        }
    }

    @After
    public void tearDown() throws Exception {
        // ROLLBACK TRANSACTION
        if (connection != null) {
            try {
                System.out.println("Rolling back transaction...");
                connection.rollback();
                connection.close();
                System.out.println("Transaction rolled back and connection closed.");
            } catch (SQLException e) {
                System.err.println("Error during rollback/close: " + e.getMessage());
            }
        }
        SAMPLE_ID = null;
    }

    private NhanVien createSampleNhanVien(String maNV) {
        long randomCccd = ThreadLocalRandom.current().nextLong(100000000000L, 999999999999L);
        String randomPhone = "09" + ThreadLocalRandom.current().nextInt(10000000, 99999999);
        Date birthDate = new Date(System.currentTimeMillis() - (30L * 365 * 24 * 60 * 60 * 1000));
        Date joinDate = new Date(System.currentTimeMillis() - (1L * 365 * 24 * 60 * 60 * 1000)); // ~1 năm trước

        NhanVien nv = new NhanVien();
        if (maNV != null) nv.setMaNhanVien(maNV);
        nv.setTenNhanVien("Nhân Viên Test " + System.currentTimeMillis());
        nv.setDiaChi("123 Đường Test, Quận Test");
        nv.setSoDienThoai(randomPhone);
        nv.setEmail("test" + System.currentTimeMillis() + "@example.com");
        nv.setCCCD(randomCccd);
        nv.setGioiTinh(GENDER_MALE);
        nv.setNgaySinh(birthDate);
        nv.setNgayVaoLam(joinDate);
        nv.setChucVu("Tester");

        return nv;
    }

    // --- Test Cases ---

    @Test
    public void testGetInstance() {
        NhanVienDAO instance1 = NhanVienDAO.getInstance();
        NhanVienDAO instance2 = NhanVienDAO.getInstance();
        assertNotNull("getInstance() should return a non-null instance", instance1);
        assertNotSame("getInstance() currently returns new instances", instance1, instance2);
    }

    @Test
    public void testInsert_Success() throws SQLException {
        System.out.println("Testing Insert Success...");
        int result = nhanVienDAO.insert(sampleNhanVien);
        assertEquals("insert() should return > 0 on successful insertion", 1, result);

        ArrayList<NhanVien> allNV = nhanVienDAO.getByMaNhanVien(SAMPLE_ID);
        assertNotNull(allNV);
        boolean found = false;
        for(NhanVien nv : allNV) {
            if (nv.getCCCD() == sampleNhanVien.getCCCD() && nv.getTenNhanVien().equals(sampleNhanVien.getTenNhanVien())) {
                found = true;
                SAMPLE_ID = nv.getMaNhanVien();
                System.out.println("Found inserted employee with MANV: " + SAMPLE_ID);
                break;
            }
        }
        assertTrue("Newly inserted employee should be found via getAll() before rollback", found);

        System.out.println("Insert Success Test Passed.");
    }

    @Test
    public void testInsert_Fail_DuplicateConstraint() {
        NhanVien nv1 = createSampleNhanVien("NVtest1");
        int result1 = nhanVienDAO.insert(nv1);
        Assume.assumeTrue("First insert must succeed for duplicate test", result1 > 0);

        NhanVien nv2 = createSampleNhanVien("NVtest2");
        nv2.setCCCD(nv1.getCCCD()); // Trùng CCCD
        int result2 = nhanVienDAO.insert(nv2);

        assertEquals("insert() should return 0 when violating unique constraint (e.g., CCCD)", 0, result2);
        System.out.println("Insert Duplicate Constraint Test Passed (returned 0).");
    }

    @Test
    public void testInsert_Fail_NullData() {
        NhanVien invalidNV = createSampleNhanVien(SAMPLE_ID);
        invalidNV.setTenNhanVien(null);

        int result = nhanVienDAO.insert(invalidNV);
        assertEquals("insert() should return 0 if required data is null (DB constraint)", 0, result);
        System.out.println("Insert Null Data Test Passed (returned 0).");
    }

    @Test
    public void testInsert_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            int result = NhanVienDAO.getInstance().insert(sampleNhanVien);
            assertNull("Khi gặp SQLException, insert() phải trả về null", result);
        } catch (Exception e) {
            fail("insert() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testUpdate_Success() {
        NhanVien existingNV = nhanVienDAO.getById(EXISTING_MANV_FOR_UPDATE_DELETE);
        Assume.assumeNotNull("Existing employee NV01 must be found for update test", existingNV);

        NhanVien updatedNV = createSampleNhanVien(EXISTING_MANV_FOR_UPDATE_DELETE);
        updatedNV.setTenNhanVien("Hồ Việt 2");
        updatedNV.setDiaChi("abc ");
        updatedNV.setCCCD(existingNV.getCCCD());
        updatedNV.setNgaySinh(existingNV.getNgaySinh());
        updatedNV.setNgayVaoLam(existingNV.getNgayVaoLam());
        updatedNV.setGioiTinh(existingNV.getGioiTinh());


        int result = nhanVienDAO.update(updatedNV);
        assertEquals("update() should return > 0 for successful update", 1, result);

        NhanVien verifiedNV = nhanVienDAO.getById(EXISTING_MANV_FOR_UPDATE_DELETE);
        assertNotNull("Updated employee should still exist", verifiedNV);
        assertEquals("Employee name should be updated", "Hồ Việt 2", verifiedNV.getTenNhanVien());
        assertEquals("Employee address should be updated", "abc ", verifiedNV.getDiaChi());
        System.out.println("Update Success Test Passed.");
    }

    @Test
    public void testUpdate_Fail_NotFound() {
        NhanVien nonExistingNV = createSampleNhanVien(NON_EXISTING_MANV);
        int result = nhanVienDAO.update(nonExistingNV);
        assertEquals("update() should return 0 if MANHANVIEN does not exist", 0, result);
        System.out.println("Update Not Found Test Passed (returned 0).");
    }

    @Test
    public void testUpdate_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            int result = NhanVienDAO.getInstance().update(sampleNhanVien);
            assertNull("Khi gặp SQLException, update() phải trả về null", result);
        } catch (Exception e) {
            fail("update() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testDelete_Success() {
        NhanVien nvToDelete = createSampleNhanVien(SAMPLE_ID);
        int insertResult = nhanVienDAO.insert(nvToDelete);
        Assume.assumeTrue("Insert must succeed for delete test", insertResult > 0);
        ArrayList<NhanVien> allNV = nhanVienDAO.getAll();
        String manvToDelete = null;
        for(NhanVien nv : allNV) {
            if (nv.getCCCD() == nvToDelete.getCCCD()) {
                manvToDelete = nv.getMaNhanVien();
                break;
            }
        }
        Assume.assumeNotNull("Could not find MANV of the newly inserted employee to delete", manvToDelete);

        int deleteResult = nhanVienDAO.delete(manvToDelete);
        assertEquals("delete() should return > 0 for successful deletion", 1, deleteResult);

        NhanVien verifiedNV = nhanVienDAO.getById(manvToDelete);
        assertNull("Deleted employee should not be found by getById", verifiedNV);
        System.out.println("Delete Success Test Passed.");
    }

    @Test
    public void testDelete_Fail_NotFound() {
        int result = nhanVienDAO.delete(NON_EXISTING_MANV);
        assertEquals("delete() should return 0 if MANHANVIEN does not exist", 0, result);
        System.out.println("Delete Not Found Test Passed (returned 0).");
    }

    @Test
    public void testDelete_Fail_ForeignKeyConstraint() {
        String manvWithFK = "NV02";
        int result = nhanVienDAO.delete(manvWithFK);
        assertEquals("delete() should return 0 if FK constraint prevents deletion", 0, result);
        System.out.println("Delete Foreign Key Test: Skipped (requires specific data setup).");
    }

    @Test
    public void testDelete_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            int result = NhanVienDAO.getInstance().delete(sampleNhanVien.getMaNhanVien());
            assertNull("Khi gặp SQLException, delete() phải trả về null", result);
        } catch (Exception e) {
            fail("delete() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetAll_Success() {
        ArrayList<NhanVien> result = nhanVienDAO.getAll();
        assertNotNull("getAll() should not return null on success", result);
        assertTrue("getAll() should return a list (possibly empty)", result instanceof ArrayList);
        System.out.println("testGetAll_Success: Found " + result.size() + " employees.");
    }

    @Test
    public void testGetAll_Empty() {
        try (Connection conn = JDBCUtil.getConnection()) {
            conn.setAutoCommit(false);

            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM NHANVIEN;");

            int rowsDeleted = pstmt.executeUpdate();
            ArrayList<NhanVien> result = nhanVienDAO.getAll();
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
            ArrayList<NhanVien> result = NhanVienDAO.getInstance().getAll();
            assertNull("Khi gặp SQLException, getAll() phải trả về null", result);
        } catch (Exception e) {
            fail("getAll() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetById_Found() {
        NhanVien result = nhanVienDAO.getById(EXISTING_MANV_FOR_UPDATE_DELETE);
        assertNotNull("getById() should return an object for existing ID", result);
        assertEquals("Returned object should have the correct MANHANVIEN", EXISTING_MANV_FOR_UPDATE_DELETE, result.getMaNhanVien());

        assertEquals("Returned object should have the correct TenNV", EXISTING_TENNV, result.getTenNhanVien());
        System.out.println("testGetById_Found: Found employee " + result.getMaNhanVien());
    }

    @Test
    public void testGetById_NotFound() {
        NhanVien result = nhanVienDAO.getById(NON_EXISTING_MANV);
        assertNull("getById() should return null for non-existing ID", result);
        System.out.println("testGetById_NotFound: Correctly returned null for " + NON_EXISTING_MANV);
    }

    @Test
    public void testGetById_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            NhanVien result = NhanVienDAO.getInstance().getById(sampleNhanVien.getMaNhanVien());
            assertNull("Khi gặp SQLException, getById() phải trả về null", result);
        } catch (Exception e) {
            fail("getById() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByMaNhanVien_Found() {
        ArrayList<NhanVien> result = nhanVienDAO.getByMaNhanVien(EXISTING_MANV_FOR_UPDATE_DELETE);
        assertNotNull("getByMaNhanVien() should not return null", result);
        assertEquals("getByMaNhanVien() should return list size 1 for existing unique ID", 1, result.size());
        assertEquals("The employee in list should have correct MANV", EXISTING_MANV_FOR_UPDATE_DELETE, result.get(0).getMaNhanVien());
        System.out.println("testGetByMaNhanVien_Found: Found 1 employee in list for " + EXISTING_MANV_FOR_UPDATE_DELETE);
    }

    @Test
    public void testGetByMaNhanVien_NotFound() {
        ArrayList<NhanVien> result = nhanVienDAO.getByMaNhanVien(NON_EXISTING_MANV);
        assertNotNull("getByMaNhanVien() should return empty list, not null, for non-existing ID", result);
        assertEquals("getByMaNhanVien() should return list size 0 for non-existing ID", 0, result.size());
        System.out.println("testGetByMaNhanVien_NotFound: Correctly returned empty list for " + NON_EXISTING_MANV);
    }

    @Test
    public void testGetByMaNhanVien_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<NhanVien> result = NhanVienDAO.getInstance().getByMaNhanVien(sampleNhanVien.getMaNhanVien());
            assertNull("Khi gặp SQLException, getByMaNhanVien() phải trả về null", result);
        } catch (Exception e) {
            fail("getByMaNhanVien() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByTen_Found() {
        ArrayList<NhanVien> result = nhanVienDAO.getByTen(EXISTING_TENNV);
        assertNotNull("getByTen() should not return null", result);
        assertTrue("getByTen() should find at least one employee for existing name", result.size() > 0);
        for (NhanVien nv : result) {
            assertEquals("Employee name should match", EXISTING_TENNV, nv.getTenNhanVien());
        }
        System.out.println("testGetByTen_Found: Found " + result.size() + " employees with name " + EXISTING_TENNV);
    }

    @Test
    public void testGetByTen_NotFound() {
        String nonExistingName = "Tên Không Có Thật";
        ArrayList<NhanVien> result = nhanVienDAO.getByTen(nonExistingName);
        assertNotNull("getByTen() should return empty list, not null, for non-existing name", result);
        assertEquals("getByTen() should return list size 0 for non-existing name", 0, result.size());
        System.out.println("testGetByTen_NotFound: Correctly returned empty list for " + nonExistingName);
    }

    @Test
    public void testGetByTen_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<NhanVien> result = NhanVienDAO.getInstance().getByTen(sampleNhanVien.getTenNhanVien());
            assertNull("Khi gặp SQLException, getByTen() phải trả về null", result);
        } catch (Exception e) {
            fail("getByTen() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetDanhSachLuong_Success() {
        ArrayList<TienLuong> result = nhanVienDAO.getDanhSachLuong(TEST_MONTH, TEST_YEAR);
        assertNotNull("getDanhSachLuong() should not return null", result);
        System.out.println("testGetDanhSachLuong_Success: Found " + result.size() + " salary records for " + TEST_MONTH + "/" + TEST_YEAR);
    }

    @Test
    public void testGetDanhSachLuong_NoData() {
        int futureYear = TEST_YEAR + 100;
        ArrayList<TienLuong> result = nhanVienDAO.getDanhSachLuong(TEST_MONTH, futureYear);
        assertNotNull("getDanhSachLuong() should return empty list, not null, when no data", result);
        assertEquals("getDanhSachLuong() should return size 0 when no data", 0, result.size());
        System.out.println("testGetDanhSachLuong_NoData: Correctly returned empty list for " + TEST_MONTH + "/" + futureYear);
    }

    @Test
    public void testGetDanhSachLuong_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<TienLuong> result = NhanVienDAO.getInstance().getDanhSachLuong(10, 2021);
            assertNull("Khi gặp SQLException, getDanhSachLuong() phải trả về null", result);
        } catch (Exception e) {
            fail("getDanhSachLuong() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByGioiTinh_Found() {
        ArrayList<NhanVien> result = nhanVienDAO.getByGioiTinh(GENDER_MALE);
        assertNotNull("getByGioiTinh() should not return null", result);
        assertTrue("getByGioiTinh() should find male employees", result.size() > 0);
        for (NhanVien nv : result) {
            assertEquals("Employee gender should be male", GENDER_MALE, nv.getGioiTinh());
        }
        System.out.println("testGetByGioiTinh_Found: Found " + result.size() + " male employees.");
    }

    @Test
    public void testGetByGioiTinh_NotFound() {
        String invalidGender = "Không xác định";
        ArrayList<NhanVien> result = nhanVienDAO.getByGioiTinh(invalidGender);
        assertNotNull("getByGioiTinh() should return empty list, not null, for non-matching gender", result);
        assertEquals("getByGioiTinh() should return size 0 for non-matching gender", 0, result.size());
        System.out.println("testGetByGioiTinh_NotFound: Correctly returned empty list for gender: " + invalidGender);
    }

    @Test
    public void testGetByGioiTinh_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<NhanVien> result = NhanVienDAO.getInstance().getByGioiTinh(sampleNhanVien.getGioiTinh());
            assertNull("Khi gặp SQLException, getByGioiTinh() phải trả về null", result);
        } catch (Exception e) {
            fail("getByGioiTinh() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByChucVu_Found() {
        ArrayList<NhanVien> result = nhanVienDAO.getByChucVu(EXISTING_CHUCVU);
        assertNotNull("getByChucVu() should not return null", result);
        assertTrue("getByChucVu() should find employees with existing position", result.size() > 0);
        for (NhanVien nv : result) {
            assertEquals("Employee position should match", EXISTING_CHUCVU, nv.getChucVu());
        }
        System.out.println("testGetByChucVu_Found: Found " + result.size() + " employees with position: " + EXISTING_CHUCVU);
    }

    @Test
    public void testGetByChucVu_NotFound() {
        ArrayList<NhanVien> result = nhanVienDAO.getByChucVu(NON_EXISTING_CHUCVU);
        assertNotNull("getByChucVu() should return empty list, not null, for non-existing position", result);
        assertEquals("getByChucVu() should return size 0 for non-existing position", 0, result.size());
        System.out.println("testGetByChucVu_NotFound: Correctly returned empty list for position: " + NON_EXISTING_CHUCVU);
    }

    @Test
    public void testGetByChucVu_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<NhanVien> result = NhanVienDAO.getInstance().getByChucVu(sampleNhanVien.getChucVu());
            assertNull("Khi gặp SQLException, getByChucVu() phải trả về null", result);
        } catch (Exception e) {
            fail("getByChucVu() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }
}
