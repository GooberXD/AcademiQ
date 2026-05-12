package com.academiq.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SqliteDataStoreTest {

    private SqliteDataStore store;

    @BeforeEach
    void setUp() {
        store = new SqliteDataStore(":memory:");
    }

    @AfterEach
    void tearDown() {
        if (store != null) {
            store.close();
        }
    }

    @Test
    void testConnectionOpensSuccessfully() {
        assertTrue(store.isConnected());
    }

    @Test
    void testTablesExistAfterInit() throws Exception {
        Set<String> expected = Set.of("students", "terms", "courses", "assessments", "time_slots");
        Set<String> found = new HashSet<>();

        try (Statement stmt = store.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table';")) {
            while (rs.next()) {
                found.add(rs.getString("name"));
            }
        }

        assertTrue(found.containsAll(expected),
            "Missing tables. Expected: " + expected + " Found: " + found);
    }

    @Test
    void testForeignKeysEnabled() throws Exception {
        try (Statement stmt = store.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys;")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void testCloseReleasesConnection() {
        store.close();
        assertFalse(store.isConnected());
    }

    @Test
    void testConstructorWithBadPathThrows() {
        assertThrows(RuntimeException.class,
            () -> new SqliteDataStore("/nonexistent/path/that/cannot/exist/db.db"));
    }
}
