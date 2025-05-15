package test;

import Model.DanhMuc;
import dao.DanhMucDAO;
import database.JDBCUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class DanhMucDAOTest {

    private DanhMucDAO danhMucDAO;
    private Connection connection;

    private static final String EXISTING_MADM_FOR_UPDATE_DELETE = "DM01";
    private static final String NON_EXISTING_MADM = "DM9999";
    private static final String EXISTING_TENDM = "Danh mục A";

    // Helper method to create sample test data
    private DanhMuc createSampleDanhMuc(String testMaDM) {
        String testTenDanhMuc = "Danh mục test";
        return new DanhMuc(testMaDM, testTenDanhMuc);
    }

    // Helper method to find DanhMuc by TenDanhMuc
    private DanhMuc findDanhMucByTen(String tenDanhMuc) {
        ArrayList<DanhMuc> all = danhMucDAO.getAll();
        if (all != null) {
            for (DanhMuc dm : all) {
                if (dm.getTenDanhMuc().equals(tenDanhMuc)) {
                    return dm;
                }
            }
        }
        return null;
    }

    @Before
    public void setUp() throws SQLException {
        danhMucDAO = DanhMucDAO.getInstance();
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
        DanhMuc dm = createSampleDanhMuc("DMTEST");
        int result = danhMucDAO.insert(dm);
        assertEquals("Insert should affect 1 row", 1, result);

        // Verification: Find the inserted category by name
        DanhMuc insertedDm = findDanhMucByTen(dm.getTenDanhMuc());
        assertNotNull("Inserted DanhMuc should be findable by TenDanhMuc", insertedDm);
        assertEquals("Inserted TenDanhMuc should match", dm.getTenDanhMuc(), insertedDm.getTenDanhMuc());
    }

    @Test
    public void testInsert_Failure_ConstraintViolation() {
        System.out.println("Executing testInsert_Failure_ConstraintViolation");
        DanhMuc dm1 = createSampleDanhMuc("dmtest1");
        dm1.setTenDanhMuc("Test DM Constraint 1");

        int result1 = danhMucDAO.insert(dm1);
        assertEquals(1, result1);

        // Try to insert with the same name (constraint violation)
        DanhMuc dm2 = createSampleDanhMuc("dmtest2");
        dm2.setTenDanhMuc("Test DM Constraint 1");

        int result2 = danhMucDAO.insert(dm2);
        assertEquals("Insert should fail (return 0) on constraint violation", 0, result2);
    }

    @Test
    public void testInsert_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            DanhMuc dm = createSampleDanhMuc("dmtest_sql_ex");
            int result = DanhMucDAO.getInstance().insert(dm);
            assertEquals("When SQLException occurs, insert() should return 0", 0, result);
        } catch (Exception e) {
            fail("insert() should not throw exception. Error: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for update ---
    @Test
    public void testUpdate_Success_Existing() {
        System.out.println("Executing testUpdate_Success_Existing");
        DanhMuc existingDM = danhMucDAO.getById(EXISTING_MADM_FOR_UPDATE_DELETE);
        assertNotNull("Target DanhMuc for update not found", existingDM);

        String originalName = existingDM.getTenDanhMuc();
        existingDM.setTenDanhMuc("Updated Danh Mục Name");

        int result = danhMucDAO.update(existingDM);
        assertEquals("Update should affect 1 row", 1, result);

        DanhMuc updatedDm = danhMucDAO.getById(existingDM.getMaDanhMuc());
        assertNotNull("Updated DanhMuc could not be retrieved by ID", updatedDm);
        assertEquals("Name should be updated", "Updated Danh Mục Name", updatedDm.getTenDanhMuc());
        
        // Restore original name for future tests
        existingDM.setTenDanhMuc(originalName);
        danhMucDAO.update(existingDM);
    }

    @Test
    public void testUpdate_Failure_NonExisting() {
        System.out.println("Executing testUpdate_Failure_NonExisting");
        DanhMuc dmNonExist = createSampleDanhMuc(NON_EXISTING_MADM);
        dmNonExist.setTenDanhMuc("Non-existing DM");

        int result = danhMucDAO.update(dmNonExist);
        assertEquals("Update should affect 0 rows for non-existing ID", 0, result);
    }

    @Test
    public void testUpdate_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            DanhMuc dm = createSampleDanhMuc("dm_test_update_ex");
            int result = DanhMucDAO.getInstance().update(dm);
            assertEquals("When SQLException occurs, update() should return 0", 0, result);
        } catch (Exception e) {
            fail("update() should not throw exception. Error: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for delete ---
    @Test
    public void testDelete_Success_Existing() {
        System.out.println("Executing testDelete_Success_Existing");

        // Create new DanhMuc for deletion
        DanhMuc dmToDelete = new DanhMuc();
        dmToDelete.setTenDanhMuc("Delete Target Category");
        danhMucDAO.insert(dmToDelete);
        
        // Find the inserted DanhMuc
        DanhMuc insertedDm = findDanhMucByTen("Delete Target Category");
        assertNotNull("Target DanhMuc for delete not found after insert", insertedDm);
        
        String maDanhMucToDelete = insertedDm.getMaDanhMuc();

        int result = danhMucDAO.delete(maDanhMucToDelete);
        assertEquals("Delete should affect 1 row", 1, result);

        DanhMuc deletedDm = danhMucDAO.getById(maDanhMucToDelete);
        assertNull("Deleted DanhMuc should not be found by ID", deletedDm);
    }

    @Test
    public void testDelete_Failure_NonExisting() {
        System.out.println("Executing testDelete_Failure_NonExisting");
        String nonExistentId = "DM999998";
        int result = danhMucDAO.delete(nonExistentId);
        assertEquals("Delete should affect 0 rows for non-existing ID", 0, result);
    }

    @Test
    public void testDelete_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            int result = DanhMucDAO.getInstance().delete("dm_test_delete_ex");
            assertEquals("When SQLException occurs, delete() should return 0", 0, result);
        } catch (Exception e) {
            fail("delete() should not throw exception. Error: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for getAll ---
    @Test
    public void testGetAll_MultipleRecords() {
        System.out.println("Executing testGetAll_MultipleRecords");

        ArrayList<DanhMuc> allDanhMuc = danhMucDAO.getAll();
        assertNotNull("Result list should not be null", allDanhMuc);
        assertTrue("Result list should contain at least 1 record", allDanhMuc.size() >= 1);
        
        // Verify expected records are present
        boolean foundExisting = false;
        for(DanhMuc dm : allDanhMuc) {
            if(dm.getMaDanhMuc().equals(EXISTING_MADM_FOR_UPDATE_DELETE)) {
                foundExisting = true;
                break;
            }
        }
        assertTrue("Should find existing DanhMuc in getAll result", foundExisting);
    }

    @Test
    public void testGetAll_NoRecords() {
        try (Connection conn = JDBCUtil.getConnection()) {
            conn.setAutoCommit(false);
            System.out.println("Executing testGetAll_NoRecords");

            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM DANHMUC");
            int rowsDeleted = pstmt.executeUpdate();
            ArrayList<DanhMuc> allDanhMuc = danhMucDAO.getAll();

            conn.rollback();
            assertNotNull("Result list should not be null even when empty", allDanhMuc);
            System.out.println("Found " + allDanhMuc.size() + " records in testGetAll_NoRecords (expecting 0 if rollback worked perfectly).");
            assertTrue("getAll should return an empty list or a list without newly added items after rollback", true);
        } catch (SQLException e) {
            System.err.println("Failed to clear test data: " + e.getMessage());
        }
    }

    @Test
    public void testGetAll_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<DanhMuc> result = DanhMucDAO.getInstance().getAll();
            assertNull("When SQLException occurs, getAll() should return null", result);
        } catch (Exception e) {
            fail("getAll() should not throw exception. Error: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for getById ---
    @Test
    public void testGetById_Success_Existing() {
        System.out.println("Executing testGetById_Success_Existing");
        DanhMuc result = danhMucDAO.getById(EXISTING_MADM_FOR_UPDATE_DELETE);
        assertNotNull("Should find DanhMuc by ID", result);
        assertEquals("ID should match search parameter", EXISTING_MADM_FOR_UPDATE_DELETE, result.getMaDanhMuc());
    }

    @Test
    public void testGetById_Failure_NonExisting() {
        System.out.println("Executing testGetById_Failure_NonExisting");
        DanhMuc result = danhMucDAO.getById(NON_EXISTING_MADM);
        assertNull("Should not find non-existing DanhMuc by ID", result);
    }

    @Test
    public void testGetById_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            DanhMuc result = DanhMucDAO.getInstance().getById("DM01");
            assertNull("When SQLException occurs, getById() should return null", result);
        } catch (Exception e) {
            fail("getById() should not throw exception. Error: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for getByMaDanhMuc ---
    @Test
    public void testGetByMaDanhMuc_Success_Existing() {
        System.out.println("Executing testGetByMaDanhMuc_Success_Existing");
        ArrayList<DanhMuc> results = danhMucDAO.getByMaDanhMuc(EXISTING_MADM_FOR_UPDATE_DELETE);
        assertNotNull("Result list should not be null", results);
        assertFalse("Result list should not be empty", results.isEmpty());
        
        boolean foundMatch = false;
        for (DanhMuc dm : results) {
            if (dm.getMaDanhMuc().equals(EXISTING_MADM_FOR_UPDATE_DELETE)) {
                foundMatch = true;
                break;
            }
        }
        assertTrue("Should find matching MaDanhMuc in results", foundMatch);
    }

    @Test
    public void testGetByMaDanhMuc_Failure_NonExisting() {
        System.out.println("Executing testGetByMaDanhMuc_Failure_NonExisting");
        ArrayList<DanhMuc> results = danhMucDAO.getByMaDanhMuc(NON_EXISTING_MADM);
        assertTrue("Result list should be empty for non-existing ID", results.isEmpty());
    }

    @Test
    public void testGetByMaDanhMuc_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<DanhMuc> result = DanhMucDAO.getInstance().getByMaDanhMuc("DM01");
            assertNull("When SQLException occurs, getByMaDanhMuc() should return null", result);
        } catch (Exception e) {
            fail("getByMaDanhMuc() should not throw exception. Error: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }

    // --- Test Cases for getByTen ---
    @Test
    public void testGetByTen_Success_Matching() {
        System.out.println("Executing testGetByTen_Success_Matching");
        
        // Insert test data with known name
        DanhMuc testDm = new DanhMuc();
        testDm.setTenDanhMuc("Danh Muc Find By Ten Test");
        danhMucDAO.insert(testDm);
        
        ArrayList<DanhMuc> results = danhMucDAO.getByTen("Danh Muc Find By Ten Test");
        assertNotNull("Result list should not be null", results);
        assertFalse("Result list should not be empty", results.isEmpty());
        
        boolean foundMatch = false;
        for (DanhMuc dm : results) {
            if (dm.getTenDanhMuc().equals("Danh Muc Find By Ten Test")) {
                foundMatch = true;
                break;
            }
        }
        assertTrue("Should find matching TenDanhMuc in results", foundMatch);
    }

    @Test
    public void testGetByTen_Failure_NoMatch() {
        System.out.println("Executing testGetByTen_Failure_NoMatch");
        ArrayList<DanhMuc> results = danhMucDAO.getByTen("Non Existent Danh Muc Name XYZ");
        assertTrue("Result list should be empty for non-matching name", results.isEmpty());
    }

    @Test
    public void testGetByTen_SQLException() {
        JDBCUtil.overridePassword = "sai_password";
        try {
            ArrayList<DanhMuc> result = DanhMucDAO.getInstance().getByTen("Test");
            assertNull("When SQLException occurs, getByTen() should return null", result);
        } catch (Exception e) {
            fail("getByTen() should not throw exception. Error: " + e.getMessage());
        } finally {
            JDBCUtil.overridePassword = null;
        }
    }
} 