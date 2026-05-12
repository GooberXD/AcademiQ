package com.academiq.persistence;

import com.academiq.model.Student;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Concrete SQLite persistence — no interface by design.
 * GradingPolicy already demonstrates Strategy pattern;
 * a DataStore interface with one implementation would be a code smell.
 */
public class SqliteDataStore {

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
            }

            createTables();

            System.out.println("Connected Successfully");

        } catch (SQLException e) {
            System.out.print("Database connection error: ");
            e.printStackTrace();
        }
    }

    private void createTables() {
        try(Statement stmt = connection.createStatement()){

            //Create table for the students
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS students (
                    student_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL
                );
            """);

            //Create table for terms
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

            //Create table for courses
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

            //Create table for assessments
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

            //Create table for time slots
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
            System.out.print("Table Creation Error: ");
            e.printStackTrace();
        }
    }

    public void save(Student student) {
        if (connection == null) {
            System.out.println("Save error: no database connection.");
            return;
        }

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

            System.out.println("Student saved successfully.");
        }
        catch (SQLException e) {
            System.out.print("Save error: ");
            e.printStackTrace();
        }
    }

    public Student load() {
        // TODO: reconstruct Student → Term → Course → Assessment from DB
        // Use GradingPolicyFactory to rebuild polymorphic GradingPolicy from
        // grading_type + grading_config columns
        return null;
    }

    public void close() {
        try{
            if(connection != null && !connection.isClosed()){
                connection.close();
                System.out.println("Database connection closed.");
            }
        }
        catch(SQLException e){
            System.out.print("Close error: ");
            e.printStackTrace();
        }
        
    }
}
