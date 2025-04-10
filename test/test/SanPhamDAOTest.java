package test;

import Model.ChiTietHoaDon;
import Model.NhanVien;
import dao.ChiTietHoaDonDAO;
import dao.SanPhamDAO;
import database.JDBCUtil;
import Model.SanPham;
import org.junit.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class SanPhamDAOTest {

    private static Connection connection;
    private SanPhamDAO sanPhamDAO;
    private SanPham testSanPham;
    private String generatedTestSanPhamId = null;

    private static final String EXISTING_MA_SAN_PHAM = "SP002";
    private static final String EXISTING_MA_DANH_MUC = "DM01";
    private static final String NON_EXISTENT_ID = "NONEXISTENT999";
    private static final String EXISTING_TEN_SAN_PHAM = "GIÀY CẦU LÔNG KAWASAKI";
    private static final String EXISTING_HANG_SAN_XUAT = "NIKE";
    private static final String EXISTING_MAU_SAC = "ĐỎ";
    private static final String EXISTING_MON_THE_THAO = "CẦU LÔNG";
    private static final String TEST_INSERT_TEN_SAN_PHAM = "Product Insert";
    private static final String TEST_UPDATE_TEN_SAN_PHAM = "Product Updated";

    @BeforeClass
    public static void setUpClass() {
        try {
            connection = JDBCUtil.getConnection();
            connection.setAutoCommit(false);
            System.out.println("Database connection established and auto-commit disabled.");

        } catch (SQLException e) {
            System.err.println("FATAL: Could not connect to database or disable auto-commit. Tests will likely fail.");
            e.printStackTrace();
            connection = null;
        }
    }

    @AfterClass
    public static void tearDownClass() {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                System.err.println("Error rolling back final transaction.");
                e.printStackTrace();
            }
            try {
                // Close the connection
                connection.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing database connection.");
                e.printStackTrace();
            }
        }
    }

    @Before
    public void setUp() {
        Assume.assumeNotNull("Database connection must be available for tests", connection);

        sanPhamDAO = SanPhamDAO.getInstance();
        testSanPham = new SanPham(
                null,
                TEST_INSERT_TEN_SAN_PHAM + System.nanoTime(),
                10,
                "M",
                "test_image.jpg",
                "JUnit Test Description",
                "JUnit Test Manufacturer",
                "JUnit Test Color",
                100.0,
                120.0,
                "JUnit Test Sport",
                EXISTING_MA_DANH_MUC,
                365
        );
        generatedTestSanPhamId = null;
        try {
            ArrayList<SanPham> leftovers = sanPhamDAO.getByTen(testSanPham.getTenSanPham().substring(0, TEST_INSERT_TEN_SAN_PHAM.length()));
            for (SanPham leftover : leftovers) {
                System.out.println("Cleaning up leftover test product: " + leftover.getMaSanPham());
                sanPhamDAO.delete(leftover.getMaSanPham());
            }

        } catch (Exception e) {
            System.err.println("Warning: Error during pre-test cleanup: " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        if (connection != null) {
            try {
                connection.rollback();
                System.out.println("Transaction rolled back for test");
            } catch (SQLException e) {
                System.err.println("Error rolling back transaction after test.");
                e.printStackTrace();
                Assert.fail("Rollback failed, test environment potentially dirty.");
            }
        }
        sanPhamDAO = null;
        testSanPham = null;
    }

    // --- Test Cases ---

    @Test
    public void testGetInstance() {
        assertNotNull("getInstance() should not return null", sanPhamDAO);
        SanPhamDAO instance1 = SanPhamDAO.getInstance();
        SanPhamDAO instance2 = SanPhamDAO.getInstance();
        assertNotNull("Second call to getInstance() should not return null", instance2);
        assertNotSame(instance1, instance2);
    }

    // --- INSERT TESTS ---
    @Test
    public void testInsert_Success() {
        System.out.println("Testing insert (Success)...");
        int result = sanPhamDAO.insert(testSanPham);
        assertEquals("Insert should return 1 on success", 1, result);

        ArrayList<SanPham> foundList = sanPhamDAO.getByTen(testSanPham.getTenSanPham());
        assertNotNull("Search after insert should not return null list", foundList);
        assertEquals("Should find exactly one product by unique name after insert", 1, foundList.size());
        SanPham insertedProduct = foundList.get(0);
        assertNotNull("Inserted product should be found", insertedProduct);
        assertEquals("Inserted product name should match", testSanPham.getTenSanPham(), insertedProduct.getTenSanPham());
        assertEquals("Inserted product manufacturer should match", testSanPham.getHangSanXuat(), insertedProduct.getHangSanXuat());
    }

    @Test
    public void testInsert_Failure_InvalidForeignKey() {
        System.out.println("Testing insert (Failure - Invalid MaDanhMuc)...");

        SanPham invalidFkSanPham = new SanPham(
                null,
                "JUnit FK Fail Product" + System.nanoTime(),
                10, "S", "fk_fail.jpg", "Desc", "Manu", "Color",
                50.0, 60.0, "Sport",
                "INVALID_DM_999", // Non-existent MaDanhMuc
                90
        );

        int result = sanPhamDAO.insert(invalidFkSanPham);
        assertEquals("Insert with invalid foreign key should return 0", 0, result);
    }

    // Helper method to insert a test product and return its ID
    private String insertTestProductForOtherTests() {
        sanPhamDAO.insert(testSanPham);
        ArrayList<SanPham> foundList = sanPhamDAO.getByTen(testSanPham.getTenSanPham());
        if (foundList != null && !foundList.isEmpty()) {
            generatedTestSanPhamId = foundList.get(0).getMaSanPham();
            System.out.println("Helper inserted product with ID: " + generatedTestSanPhamId);
            return generatedTestSanPhamId;
        } else {
            Assert.fail("Failed to insert or find the test product needed for subsequent tests.");
            return null;
        }
    }

    @Test
    public void testInsert_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            int result = sanPhamDAO.insert(testSanPham);
            assertNull("Khi gặp SQLException, insert() phải trả về null", result);
        } catch (Exception e) {
            fail("insert() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- UPDATE TESTS ---
    @Test
    public void testUpdate_Success() {
        System.out.println("Testing update (Success)...");
        String idToUpdate = insertTestProductForOtherTests();
        assertNotNull("Need a valid ID to test update", idToUpdate);

        SanPham updatedSanPham = new SanPham(
                idToUpdate,
                TEST_UPDATE_TEN_SAN_PHAM + System.nanoTime(),
                50,
                "XL",
                "updated_image.png",
                "Updated JUnit Description",
                "Updated Manufacturer",
                "Updated Color",
                150.0,
                180.0,
                "Updated Sport",
                EXISTING_MA_DANH_MUC,
                730 // New Warranty
        );

        int result = sanPhamDAO.update(updatedSanPham);
        assertTrue("Update should return > 0 on success (usually 1)", result > 0);

        SanPham fetchedAfterUpdate = sanPhamDAO.getById(idToUpdate);
        assertNotNull("Product should still exist after update", fetchedAfterUpdate);
        assertEquals("Product name should be updated", updatedSanPham.getTenSanPham(), fetchedAfterUpdate.getTenSanPham());
        assertEquals("Product quantity should be updated", updatedSanPham.getSoLuong(), fetchedAfterUpdate.getSoLuong());
        assertEquals("Product size should be updated", updatedSanPham.getKichThuoc(), fetchedAfterUpdate.getKichThuoc());
        assertEquals("Product warranty should be updated", updatedSanPham.getSoNgayBaoHanh(), fetchedAfterUpdate.getSoNgayBaoHanh());
    }

    @Test
    public void testUpdate_Failure_NotFound() {
        System.out.println("Testing update (Failure - ID Not Found)...");
        SanPham nonExistentUpdate = new SanPham(
                NON_EXISTENT_ID,
                "Update Fail Name", 5, "L", "img.jpg", "Desc", "Manu", "Color",
                10.0, 12.0, "Sport", EXISTING_MA_DANH_MUC, 30
        );

        int result = sanPhamDAO.update(nonExistentUpdate);
        assertEquals("Update with non-existent ID should return 0", 0, result);
    }

    @Test
    public void testUpdate_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            int result = sanPhamDAO.update(testSanPham);
            assertNull("Khi gặp SQLException, update() phải trả về null", result);
        } catch (Exception e) {
            fail("update() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- DELETE TESTS ---
    @Test
    public void testDelete_Success() {
        System.out.println("Testing delete (Success)...");
        String idToDelete = insertTestProductForOtherTests();
        assertNotNull("Need a valid ID to test delete", idToDelete);

        int result = sanPhamDAO.delete(idToDelete);
        assertEquals("Delete should return 1 (as per DAO code, assuming no SQL error)", 1, result);

        SanPham fetchedAfterDelete = sanPhamDAO.getById(idToDelete);
        assertNull("Product should not be found after delete", fetchedAfterDelete);
    }

    @Test
    public void testDeleteNotFound() {
        int result = sanPhamDAO.delete("NON_EXISTENT_ID");

        assertEquals("Delete should return 1 even if ID is not found (no SQLException)", 1, result);
    }

     @Test
     public void testDeleteFailureConstraint() {
         ChiTietHoaDonDAO chiTietHoaDonDAO = ChiTietHoaDonDAO.getInstance();
         ChiTietHoaDon cthd = new ChiTietHoaDon("MA_HD_TEST", EXISTING_MA_SAN_PHAM, 5);

         int res = chiTietHoaDonDAO.insert(cthd);
         int result = sanPhamDAO.delete(EXISTING_MA_SAN_PHAM);

         assertEquals("Delete should return 0 when a constraint prevents it", 0, result);
     }

    @Test
    public void testDelete_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            int result = sanPhamDAO.delete(EXISTING_MA_SAN_PHAM);
            assertNull("Khi gặp SQLException, delete() phải trả về null", result);
        } catch (Exception e) {
            fail("delete() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- GET ALL TESTS ---
    @Test
    public void testGetAll_Success_DataExists() {
        System.out.println("Testing getAll (Success - Data Exists)...");
        insertTestProductForOtherTests();

        ArrayList<SanPham> results = sanPhamDAO.getAll();
        assertNotNull("getAll should not return null", results);
        assertFalse("getAll should return a non-empty list if data exists", results.isEmpty());

        if (!results.isEmpty()) {
            assertTrue("List should contain SanPham objects", results.get(0) instanceof SanPham);
        }
    }


    @Test
    public void testGetAllEmpty() throws SQLException {
        try (Connection conn = JDBCUtil.getConnection()) {
            conn.setAutoCommit(false);

            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM SANPHAM");

            int rowsDeleted = pstmt.executeUpdate();
            ArrayList<SanPham> result = sanPhamDAO.getAll();
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
            ArrayList<SanPham> result = sanPhamDAO.getAll();
            assertNull("Khi gặp SQLException, getAll() phải trả về null", result);
        } catch (Exception e) {
            fail("getAll() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- GET BY ID TESTS ---
    @Test
    public void testGetById_Found() {
        System.out.println("Testing getById (Success - Found)...");
        String idToFind = insertTestProductForOtherTests();
        assertNotNull("Need a valid ID to test getById", idToFind);

        SanPham result = sanPhamDAO.getById(idToFind);
        assertNotNull("getById should return a SanPham object for existing ID", result);
        assertEquals("Returned SanPham should have the correct ID", idToFind, result.getMaSanPham());
        assertEquals("Returned SanPham should have the correct name", testSanPham.getTenSanPham(), result.getTenSanPham());
    }

    @Test
    public void testGetById_NotFound() {
        System.out.println("Testing getById (Success - Not Found)...");
        SanPham result = sanPhamDAO.getById(NON_EXISTENT_ID);
        assertNull("getById should return null for non-existent ID", result);
    }


    @Test
    public void testGetById_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            SanPham result = sanPhamDAO.getById(EXISTING_MA_SAN_PHAM);
            assertNull("Khi gặp SQLException, getById() phải trả về null", result);
        } catch (Exception e) {
            fail("getById() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- GET BY ... TESTS ---

    @Test
    public void testGetByTen_Success_Found() {
        System.out.println("Testing getByTen (Success - Found)...");
        insertTestProductForOtherTests();

        ArrayList<SanPham> results = sanPhamDAO.getByTen(testSanPham.getTenSanPham());
        assertNotNull("getByTen should not return null list", results);
        assertEquals("Should find exactly one product by unique test name", 1, results.size());
        assertEquals("Found product should have the correct name", testSanPham.getTenSanPham(), results.get(0).getTenSanPham());
    }


    @Test
    public void testGetByTen_Success_NotFound() {
        System.out.println("Testing getByTen (Success - Not Found)...");
        ArrayList<SanPham> results = sanPhamDAO.getByTen("NameThatDoesNotExist99999");
        assertNotNull("getByTen should not return null list even if not found", results);
        assertTrue("getByTen should return an empty list for non-existent name", results.isEmpty());
    }

    @Test
    public void testGetByTen_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList <SanPham> result = sanPhamDAO.getByTen(testSanPham.getTenSanPham());
            assertNull("Khi gặp SQLException, getByTen() phải trả về null", result);
        } catch (Exception e) {
            fail("getByTen() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByMaSanPhamFound() {
        assertNotNull("Setup should have provided an ID for testSanPham1", EXISTING_MA_SAN_PHAM);
        ArrayList<SanPham> list = sanPhamDAO.getByMaSanPham(EXISTING_MA_SAN_PHAM);
        assertNotNull("getByMaSanPham should not return null", list);
        assertEquals("getByMaSanPham should find exactly 1 match for the ID", 1, list.size());
        assertEquals("The found item should have the correct ID", EXISTING_MA_SAN_PHAM, list.get(0).getMaSanPham());
    }

    @Test
    public void testGetByMaSanPhamNotFound() {
        ArrayList<SanPham> list = sanPhamDAO.getByMaSanPham("NON_EXISTENT_ID");
        assertNotNull("getByMaSanPham should not return null even if not found", list);
        assertTrue("getByMaSanPham list should be empty when ID is not found", list.isEmpty());
    }

    @Test
    public void testGetByMaSanPham_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList <SanPham> result = sanPhamDAO.getByMaSanPham(testSanPham.getMaSanPham());
            assertNull("Khi gặp SQLException, getByMaSanPham() phải trả về null", result);
        } catch (Exception e) {
            fail("getByMaSanPham() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByHangSanXuat_Success_Found() {
        System.out.println("Testing getByHangSanXuat (Success - Found)...");
        insertTestProductForOtherTests();

        ArrayList<SanPham> results = sanPhamDAO.getByHangSanXuat(EXISTING_HANG_SAN_XUAT);
        assertNotNull("getByHangSanXuat should not return null", results);
        assertTrue(results.stream().anyMatch(p -> p.getHangSanXuat().equals(EXISTING_HANG_SAN_XUAT)));
    }

    @Test
    public void testGetByHangSanXuatNotFound() {
        ArrayList<SanPham> list = sanPhamDAO.getByHangSanXuat("NonExistentHangSX");
        assertNotNull(list);
        assertTrue("List should be empty for non-existent HangSanXuat", list.isEmpty());
    }

    @Test
    public void testGetByHangSanXuat_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList <SanPham> result = sanPhamDAO.getByHangSanXuat(EXISTING_HANG_SAN_XUAT);
            assertNull("Khi gặp SQLException, getByHangSanXuat() phải trả về null", result);
        } catch (Exception e) {
            fail("getByHangSanXuat() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByMauSac_Success_Found() {
        System.out.println("Testing getByMauSac (Success - Found)...");
        insertTestProductForOtherTests();

        ArrayList<SanPham> results = sanPhamDAO.getByMauSac(EXISTING_MAU_SAC);
        assertNotNull("getByMauSac should not return null", results);
        assertFalse("getByMauSac should find products for test color", results.isEmpty());
        assertTrue(results.stream().anyMatch(p -> p.getMauSac().equals(EXISTING_MAU_SAC)));
    }


    @Test
    public void testGetByMauSac_NotFound() {
        ArrayList<SanPham> list = sanPhamDAO.getByMauSac("NonExistentMau");
        assertNotNull(list);
        assertTrue("List should be empty for non-existent MauSac", list.isEmpty());
    }

    @Test
    public void testGetByMauSac_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList <SanPham> result = sanPhamDAO.getByMauSac(EXISTING_MAU_SAC);
            assertNull("Khi gặp SQLException, getByMauSac() phải trả về null", result);
        } catch (Exception e) {
            fail("getByMauSac() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByMonTheThao_Success_Found() {
        System.out.println("Testing getByMonTheThao (Success - Found)...");
        insertTestProductForOtherTests();

        ArrayList<SanPham> results = sanPhamDAO.getByMonTheThao(EXISTING_MON_THE_THAO);
        assertNotNull("getByMonTheThao should not return null", results);
        assertFalse("getByMonTheThao should find products for test sport", results.isEmpty());
        assertTrue(results.stream().anyMatch(p -> p.getMonTheThao().equals(EXISTING_MON_THE_THAO)));
    }

    @Test
    public void testGetByMonTheThao_NotFound() {
        ArrayList<SanPham> list = sanPhamDAO.getByMonTheThao("NonExistentMonTheThao");
        assertNotNull(list);
        assertTrue("List should be empty for non-existent MonTheThao", list.isEmpty());
    }

    @Test
    public void testGetByMonTheThao_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList <SanPham> result = sanPhamDAO.getByMonTheThao(EXISTING_MON_THE_THAO);
            assertNull("Khi gặp SQLException, getByMonTheThao() phải trả về null", result);
        } catch (Exception e) {
            fail("getByMonTheThao() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    @Test
    public void testGetByMaDanhMuc_Success_Found() {
        System.out.println("Testing getByMaDanhMuc (Success - Found)...");
        insertTestProductForOtherTests();

        ArrayList<SanPham> results = sanPhamDAO.getByMaDanhMuc(EXISTING_MA_DANH_MUC);
        assertNotNull("getByMaDanhMuc should not return null", results);
        assertFalse("getByMaDanhMuc should find products for existing category ID", results.isEmpty());
        assertTrue(results.stream().anyMatch(p -> p.getMaDanhMuc().equals(EXISTING_MA_DANH_MUC)));

        assertTrue("Results should contain the newly inserted test product",
                results.stream().anyMatch(p -> p.getMaSanPham() != null && p.getMaSanPham().equals(generatedTestSanPhamId)));
    }

    @Test
    public void testGetByMaDanhMucNotFound() {
        ArrayList<SanPham> list = sanPhamDAO.getByMaDanhMuc("NonExistentDM");
        assertNotNull(list);
        assertTrue("List should be empty for non-existent MaDanhMuc", list.isEmpty());
    }

    @Test
    public void testGetByMaDanhMuc_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList <SanPham> result = sanPhamDAO.getByMaDanhMuc(EXISTING_MA_DANH_MUC);
            assertNull("Khi gặp SQLException, getByMaDanhMuc() phải trả về null", result);
        } catch (Exception e) {
            fail("getByMaDanhMuc() không được ném exception ra ngoài. Lỗi: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

}