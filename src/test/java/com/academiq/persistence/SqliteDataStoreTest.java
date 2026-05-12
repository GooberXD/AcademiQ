package com.academiq.persistence;

import com.academiq.grading.CurvedGrading;
import com.academiq.grading.GradingPolicy;
import com.academiq.grading.PointsBasedGrading;
import com.academiq.grading.WeightedGrading;
import com.academiq.model.Assessment;
import com.academiq.model.Course;
import com.academiq.model.Student;
import com.academiq.model.Term;
import com.academiq.model.TimeSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    private Student buildSampleStudent() {
        Student s = new Student("Alice", "stu-1");
        Term term = new Term("Fall 2026", 2026, "Fall");

        Course weightedCourse = new Course("Math", "MATH101", 3,
                new WeightedGrading(Map.of("Exams", 0.6, "HW", 0.4)));
        weightedCourse.addAssessment(new Assessment("Midterm", "Exams", 80.0, 100.0, 1.0, LocalDate.of(2026, 3, 1)));
        weightedCourse.addAssessment(new Assessment("HW1", "HW", 90.0, 100.0, 1.0, LocalDate.of(2026, 2, 1)));
        weightedCourse.addTimeSlot(new TimeSlot(DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 0), "R101"));

        Course pointsCourse = new Course("CS", "CS101", 4,
                new PointsBasedGrading(500.0));
        pointsCourse.addAssessment(new Assessment("Quiz1", "Quiz", 45.0, 50.0, 1.0, LocalDate.of(2026, 2, 15)));
        pointsCourse.addTimeSlot(new TimeSlot(DayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(14, 30), "R202"));

        term.addCourse(weightedCourse);
        term.addCourse(pointsCourse);
        s.addTerm(term);
        return s;
    }

    @Test
    void testSaveAndLoadFullObjectTree() {
        Student original = buildSampleStudent();
        store.save(original);

        Student loaded = store.loadStudent("stu-1");
        assertNotNull(loaded);
        assertEquals("Alice", loaded.getName());
        assertEquals(1, loaded.getTerms().size());

        Term term = loaded.getTerms().get(0);
        assertEquals("Fall 2026", term.getName());
        assertEquals(2026, term.getYear());
        assertEquals("Fall", term.getSemester());
        assertEquals(2, term.getCourses().size());

        Course weighted = findCourseByCode(term, "MATH101");
        assertNotNull(weighted);
        assertEquals("Math", weighted.getName());
        assertEquals(3, weighted.getUnits());
        assertTrue(weighted.getGradingPolicy() instanceof WeightedGrading);
        assertEquals(2, weighted.getAssessments().size());
        assertEquals(1, weighted.getTimeSlots().size());
        assertEquals(DayOfWeek.MONDAY, weighted.getTimeSlots().get(0).getDayOfWeek());
        assertEquals(LocalTime.of(8, 30), weighted.getTimeSlots().get(0).getStartTime());

        Course points = findCourseByCode(term, "CS101");
        assertNotNull(points);
        assertTrue(points.getGradingPolicy() instanceof PointsBasedGrading);
        assertEquals(1, points.getAssessments().size());
        assertEquals("Quiz1", points.getAssessments().get(0).getTitle());
        assertEquals(45.0, points.getAssessments().get(0).getScore(), 1e-9);
    }

    private Course findCourseByCode(Term term, String code) {
        for (Course c : term.getCourses()) {
            if (c.getCode().equals(code)) return c;
        }
        return null;
    }

    @Test
    void testDeleteStudentCascades() throws Exception {
        store.save(buildSampleStudent());
        store.deleteStudent("stu-1");

        assertNull(store.loadStudent("stu-1"));

        assertEquals(0, countRows("terms"));
        assertEquals(0, countRows("courses"));
        assertEquals(0, countRows("assessments"));
        assertEquals(0, countRows("time_slots"));
    }

    @Test
    void testDeleteTermCascades() throws Exception {
        store.save(buildSampleStudent());

        String termId;
        try (Statement st = store.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT term_id FROM terms WHERE student_id = 'stu-1'")) {
            assertTrue(rs.next());
            termId = rs.getString(1);
        }

        store.deleteTerm(termId);

        assertTrue(store.loadTerms("stu-1").isEmpty());
        assertEquals(0, countRows("courses"));
        assertEquals(0, countRows("assessments"));
        assertEquals(0, countRows("time_slots"));
    }

    @Test
    void testUpsertUpdatesExistingRow() throws Exception {
        store.save(buildSampleStudent());

        Student updated = new Student("Alice Updated", "stu-1");
        Term term = new Term("Fall 2026", 2026, "Fall");
        Course c = new Course("Math", "MATH101", 3, new WeightedGrading(Map.of("Exams", 1.0)));
        term.addCourse(c);
        updated.addTerm(term);
        store.save(updated);

        Student reloaded = store.loadStudent("stu-1");
        assertEquals("Alice Updated", reloaded.getName());
        assertEquals(1, countRowsWhere("students", "student_id = 'stu-1'"));
        assertEquals(1, countRowsWhere("terms", "student_id = 'stu-1'"));
    }

    private int countRows(String table) throws Exception {
        try (Statement st = store.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int countRowsWhere(String table, String where) throws Exception {
        try (Statement st = store.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + where)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void testWeightedGradingRoundTrip() {
        Student s = new Student("Bob", "stu-2");
        Term t = new Term("Spring", 2026, "Spring");
        WeightedGrading wg = new WeightedGrading(Map.of("Exams", 0.7, "HW", 0.3));
        Course c = new Course("Phys", "PHY1", 3, wg);
        Assessment a1 = new Assessment("Mid", "Exams", 85.0, 100.0, 1.0, LocalDate.of(2026, 3, 1));
        Assessment a2 = new Assessment("HW", "HW", 92.0, 100.0, 1.0, LocalDate.of(2026, 2, 1));
        c.addAssessment(a1);
        c.addAssessment(a2);
        t.addCourse(c);
        s.addTerm(t);

        double originalGrade = c.getFinalGrade();
        store.save(s);

        Student loaded = store.loadStudent("stu-2");
        Course lc = loaded.getTerms().get(0).getCourses().get(0);
        assertTrue(lc.getGradingPolicy() instanceof WeightedGrading);
        assertEquals(originalGrade, lc.getFinalGrade(), 1e-9);
    }

    @Test
    void testPointsBasedGradingRoundTrip() {
        Student s = new Student("Carol", "stu-3");
        Term t = new Term("Spring", 2026, "Spring");
        PointsBasedGrading pg = new PointsBasedGrading(500.0);
        Course c = new Course("Chem", "CHM1", 3, pg);
        c.addAssessment(new Assessment("Q1", "Q", 48.0, 50.0, 1.0, LocalDate.of(2026, 2, 1)));
        t.addCourse(c);
        s.addTerm(t);

        double originalGrade = c.getFinalGrade();
        store.save(s);

        Student loaded = store.loadStudent("stu-3");
        Course lc = loaded.getTerms().get(0).getCourses().get(0);
        assertTrue(lc.getGradingPolicy() instanceof PointsBasedGrading);
        assertEquals(originalGrade, lc.getFinalGrade(), 1e-9);
    }

    @Test
    void testCurvedGradingRoundTrip() throws Exception {
        Student s = new Student("Dave", "stu-4");
        Term t = new Term("Spring", 2026, "Spring");
        WeightedGrading base = new WeightedGrading(Map.of("Exams", 1.0));
        CurvedGrading cg = new CurvedGrading(5.0, base);
        Course c = new Course("Bio", "BIO1", 3, cg);
        c.addAssessment(new Assessment("Mid", "Exams", 75.0, 100.0, 1.0, LocalDate.of(2026, 3, 1)));
        t.addCourse(c);
        s.addTerm(t);

        double originalGrade = c.getFinalGrade();
        store.save(s);

        Student loaded = store.loadStudent("stu-4");
        Course lc = loaded.getTerms().get(0).getCourses().get(0);
        GradingPolicy policy = lc.getGradingPolicy();
        assertTrue(policy instanceof CurvedGrading);

        Field baseField = CurvedGrading.class.getDeclaredField("basePolicy");
        baseField.setAccessible(true);
        assertTrue(baseField.get(policy) instanceof WeightedGrading);

        assertEquals(originalGrade, lc.getFinalGrade(), 1e-9);
    }
}
