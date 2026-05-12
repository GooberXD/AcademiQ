package com.academiq.persistence;

import com.academiq.grading.GradingPolicy;
import com.academiq.grading.GradingPolicyFactory;
import com.academiq.model.Assessment;
import com.academiq.model.Course;
import com.academiq.model.Student;
import com.academiq.model.Term;
import com.academiq.model.TimeSlot;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Concrete SQLite persistence — no interface by design.
 * GradingPolicy already demonstrates Strategy pattern;
 * a DataStore interface with one implementation would be a code smell.
 */
public class SqliteDataStore implements AutoCloseable {

    private static final String DB_FILE = "academiq.db";
    private Connection connection;

    public SqliteDataStore() {
        this(DB_FILE);
    }

    public SqliteDataStore(String dbPath) {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;");
            }

            createTables();

        } catch (SQLException e) {
            throw new RuntimeException("Database connection error: " + e.getMessage(), e);
        }
    }

    private void createTables() {
        try(Statement stmt = connection.createStatement()){

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS students (
                    student_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS terms (
                    term_id TEXT PRIMARY KEY,
                    student_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    semester TEXT NOT NULL,

                    FOREIGN KEY (student_id)
                        REFERENCES students(student_id)
                        ON DELETE CASCADE
                        ON UPDATE CASCADE
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS courses (
                    course_id TEXT PRIMARY KEY,
                    term_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    code TEXT NOT NULL,
                    units INTEGER NOT NULL,
                    grading_type TEXT NOT NULL,
                    grading_config TEXT NOT NULL,

                    FOREIGN KEY (term_id)
                        REFERENCES terms(term_id)
                        ON DELETE CASCADE
                        ON UPDATE CASCADE
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS assessments (
                    assessment_id TEXT PRIMARY KEY,
                    course_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    score REAL NOT NULL,
                    max_score REAL NOT NULL,
                    weight REAL NOT NULL,
                    date TEXT NOT NULL,

                    FOREIGN KEY (course_id)
                        REFERENCES courses(course_id)
                        ON DELETE CASCADE
                        ON UPDATE CASCADE
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS time_slots (
                    time_slot_id TEXT PRIMARY KEY,
                    course_id TEXT NOT NULL,
                    day_of_week TEXT NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    room TEXT NOT NULL,

                    FOREIGN KEY (course_id)
                        REFERENCES courses(course_id)
                        ON DELETE CASCADE
                        ON UPDATE CASCADE
                );
            """);
        }
        catch(SQLException e){
            throw new RuntimeException("Table creation error: " + e.getMessage(), e);
        }
    }

    public void insertStudent(Student student) {
        String sql = """
                INSERT INTO students(student_id, name)
                VALUES(?, ?)
                ON CONFLICT(student_id) DO UPDATE SET
                    name = excluded.name
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, student.getId());
            ps.setString(2, student.getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insertStudent error: " + e.getMessage(), e);
        }
    }

    public void insertTerm(String studentId, Term term, String termId) {
        String sql = """
                INSERT INTO terms(term_id, student_id, name, year, semester)
                VALUES(?, ?, ?, ?, ?)
                ON CONFLICT(term_id) DO UPDATE SET
                    student_id = excluded.student_id,
                    name = excluded.name,
                    year = excluded.year,
                    semester = excluded.semester
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, termId);
            ps.setString(2, studentId);
            ps.setString(3, term.getName());
            ps.setInt(4, term.getYear());
            ps.setString(5, term.getSemester());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insertTerm error: " + e.getMessage(), e);
        }
    }

    public void insertCourse(String termId, Course course, String courseId) {
        String sql = """
                INSERT INTO courses(course_id, term_id, name, code, units, grading_type, grading_config)
                VALUES(?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(course_id) DO UPDATE SET
                    term_id = excluded.term_id,
                    name = excluded.name,
                    code = excluded.code,
                    units = excluded.units,
                    grading_type = excluded.grading_type,
                    grading_config = excluded.grading_config
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, courseId);
            ps.setString(2, termId);
            ps.setString(3, course.getName());
            ps.setString(4, course.getCode());
            ps.setInt(5, course.getUnits());
            ps.setString(6, GradingPolicyFactory.toType(course.getGradingPolicy()));
            ps.setString(7, GradingPolicyFactory.toConfig(course.getGradingPolicy()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insertCourse error: " + e.getMessage(), e);
        }
    }

    public void insertAssessment(String courseId, Assessment assessment, String assessmentId) {
        String sql = """
                INSERT INTO assessments(assessment_id, course_id, title, category, score, max_score, weight, date)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(assessment_id) DO UPDATE SET
                    course_id = excluded.course_id,
                    title = excluded.title,
                    category = excluded.category,
                    score = excluded.score,
                    max_score = excluded.max_score,
                    weight = excluded.weight,
                    date = excluded.date
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, assessmentId);
            ps.setString(2, courseId);
            ps.setString(3, assessment.getTitle());
            ps.setString(4, assessment.getCategory());
            ps.setDouble(5, assessment.getScore());
            ps.setDouble(6, assessment.getMaxScore());
            ps.setDouble(7, assessment.getWeight());
            ps.setString(8, assessment.getDate().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insertAssessment error: " + e.getMessage(), e);
        }
    }

    public void insertTimeSlot(String courseId, TimeSlot slot, String timeSlotId) {
        String sql = """
                INSERT INTO time_slots(time_slot_id, course_id, day_of_week, start_time, end_time, room)
                VALUES(?, ?, ?, ?, ?, ?)
                ON CONFLICT(time_slot_id) DO UPDATE SET
                    course_id = excluded.course_id,
                    day_of_week = excluded.day_of_week,
                    start_time = excluded.start_time,
                    end_time = excluded.end_time,
                    room = excluded.room
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, timeSlotId);
            ps.setString(2, courseId);
            ps.setString(3, slot.getDayOfWeek().name());
            ps.setString(4, slot.getStartTime().toString());
            ps.setString(5, slot.getEndTime().toString());
            ps.setString(6, slot.getRoom());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insertTimeSlot error: " + e.getMessage(), e);
        }
    }

    public void save(Student student) {
        if (connection == null) {
            throw new IllegalStateException("Save error: no database connection.");
        }

        boolean originalAutoCommit;
        try {
            originalAutoCommit = connection.getAutoCommit();
        } catch (SQLException e) {
            throw new RuntimeException("Save error: " + e.getMessage(), e);
        }

        try {
            connection.setAutoCommit(false);

            insertStudent(student);

            List<Term> terms = student.getTerms();
            for (int ti = 0; ti < terms.size(); ti++) {
                Term term = terms.get(ti);
                String termId = deterministicId(student.getId(), ti);
                insertTerm(student.getId(), term, termId);

                List<Course> courses = term.getCourses();
                for (int ci = 0; ci < courses.size(); ci++) {
                    Course course = courses.get(ci);
                    String courseId = deterministicId(termId, ci);
                    insertCourse(termId, course, courseId);

                    List<Assessment> assessments = course.getAssessments();
                    for (int ai = 0; ai < assessments.size(); ai++) {
                        String assessmentId = deterministicId(courseId + ":a", ai);
                        insertAssessment(courseId, assessments.get(ai), assessmentId);
                    }

                    List<TimeSlot> slots = course.getTimeSlots();
                    for (int si = 0; si < slots.size(); si++) {
                        String slotId = deterministicId(courseId + ":t", si);
                        insertTimeSlot(courseId, slots.get(si), slotId);
                    }
                }
            }

            connection.commit();
        } catch (SQLException | RuntimeException e) {
            try {
                connection.rollback();
            } catch (SQLException ignore) {
            }
            throw new RuntimeException("Save error: " + e.getMessage(), e);
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException ignore) {
            }
        }
    }

    private static String deterministicId(String parentId, int childIndex) {
        String key = parentId + ":" + childIndex;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public Student loadStudent(String studentId) {
        String sql = "SELECT name FROM students WHERE student_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Student student = new Student(rs.getString("name"), studentId);
                for (Term term : loadTerms(studentId)) {
                    student.addTerm(term);
                }
                return student;
            }
        } catch (SQLException e) {
            throw new RuntimeException("loadStudent error: " + e.getMessage(), e);
        }
    }

    public List<Term> loadTerms(String studentId) {
        List<Term> result = new ArrayList<>();
        String sql = "SELECT term_id, name, year, semester FROM terms WHERE student_id = ? ORDER BY term_id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String termId = rs.getString("term_id");
                    Term term = new Term(rs.getString("name"), rs.getInt("year"), rs.getString("semester"));
                    for (Course course : loadCourses(termId)) {
                        term.addCourse(course);
                    }
                    result.add(term);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("loadTerms error: " + e.getMessage(), e);
        }
        return result;
    }

    public List<Course> loadCourses(String termId) {
        List<Course> result = new ArrayList<>();
        String sql = "SELECT course_id, name, code, units, grading_type, grading_config FROM courses WHERE term_id = ? ORDER BY course_id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, termId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String courseId = rs.getString("course_id");
                    GradingPolicy policy = GradingPolicyFactory.fromTypeAndConfig(
                            rs.getString("grading_type"),
                            rs.getString("grading_config"));
                    Course course = new Course(rs.getString("name"), rs.getString("code"), rs.getInt("units"), policy);
                    for (Assessment a : loadAssessments(courseId)) {
                        course.addAssessment(a);
                    }
                    for (TimeSlot t : loadTimeSlots(courseId)) {
                        course.addTimeSlot(t);
                    }
                    result.add(course);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("loadCourses error: " + e.getMessage(), e);
        }
        return result;
    }

    public List<Assessment> loadAssessments(String courseId) {
        List<Assessment> result = new ArrayList<>();
        String sql = "SELECT title, category, score, max_score, weight, date FROM assessments WHERE course_id = ? ORDER BY assessment_id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Assessment(
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getDouble("score"),
                            rs.getDouble("max_score"),
                            rs.getDouble("weight"),
                            LocalDate.parse(rs.getString("date"))));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("loadAssessments error: " + e.getMessage(), e);
        }
        return result;
    }

    public List<TimeSlot> loadTimeSlots(String courseId) {
        List<TimeSlot> result = new ArrayList<>();
        String sql = "SELECT day_of_week, start_time, end_time, room FROM time_slots WHERE course_id = ? ORDER BY time_slot_id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new TimeSlot(
                            DayOfWeek.valueOf(rs.getString("day_of_week")),
                            LocalTime.parse(rs.getString("start_time")),
                            LocalTime.parse(rs.getString("end_time")),
                            rs.getString("room")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("loadTimeSlots error: " + e.getMessage(), e);
        }
        return result;
    }

    public void deleteStudent(String studentId) {
        executeDelete("DELETE FROM students WHERE student_id = ?", studentId);
    }

    public void deleteTerm(String termId) {
        executeDelete("DELETE FROM terms WHERE term_id = ?", termId);
    }

    public void deleteCourse(String courseId) {
        executeDelete("DELETE FROM courses WHERE course_id = ?", courseId);
    }

    public void deleteAssessment(String assessmentId) {
        executeDelete("DELETE FROM assessments WHERE assessment_id = ?", assessmentId);
    }

    public void deleteTimeSlot(String timeSlotId) {
        executeDelete("DELETE FROM time_slots WHERE time_slot_id = ?", timeSlotId);
    }

    private void executeDelete(String sql, String id) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete error: " + e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        try{
            if(connection != null && !connection.isClosed()){
                connection.close();
            }
        }
        catch(SQLException e){
            System.err.println("Close error: " + e.getMessage());
        }
    }
}
