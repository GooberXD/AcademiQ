# AcademiQ SQLite Schema

The schema below is the single source of truth and mirrors `SqliteDataStore.createTables()`. All IDs are application-supplied `TEXT` primary keys (SQLite only supports `AUTOINCREMENT` on `INTEGER PRIMARY KEY`). All foreign keys cascade on `DELETE` and `UPDATE`; `PRAGMA foreign_keys = ON` is enabled per-connection.

## 1. `students`

Top-level entity. One row per student.

| Column     | Type | Constraint  | Description       |
| ---------- | ---- | ----------- | ----------------- |
| student_id | TEXT | PRIMARY KEY | Unique student ID |
| name       | TEXT | NOT NULL    | Student full name |

---

## 2. `terms`

A term belongs to a student.

| Column     | Type    | Constraint           | Description                       |
| ---------- | ------- | -------------------- | --------------------------------- |
| term_id    | TEXT    | PRIMARY KEY          | Unique term ID                    |
| student_id | TEXT    | NOT NULL, FOREIGN KEY | References `students.student_id` |
| name       | TEXT    | NOT NULL             | Term name (e.g. "1st Semester")   |
| year       | INTEGER | NOT NULL             | Academic year                     |
| semester   | TEXT    | NOT NULL             | Semester label                    |

```sql
FOREIGN KEY (student_id) REFERENCES students(student_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
```

---

## 3. `courses`

A course belongs to a term. `grading_type` + `grading_config` are used by `GradingPolicyFactory` on load to rebuild the polymorphic `GradingPolicy`.

| Column         | Type    | Constraint           | Description                  |
| -------------- | ------- | -------------------- | ---------------------------- |
| course_id      | TEXT    | PRIMARY KEY          | Unique course ID             |
| term_id        | TEXT    | NOT NULL, FOREIGN KEY | References `terms.term_id`  |
| name           | TEXT    | NOT NULL             | Course title                 |
| code           | TEXT    | NOT NULL             | Course code (e.g. "IT101")   |
| units          | INTEGER | NOT NULL             | Credit units                 |
| grading_type   | TEXT    | NOT NULL             | Grading policy discriminator |
| grading_config | TEXT    | NOT NULL             | JSON-encoded grading config  |

```sql
FOREIGN KEY (term_id) REFERENCES terms(term_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
```

---

## 4. `assessments`

An assessment belongs to a course.

| Column        | Type | Constraint           | Description                       |
| ------------- | ---- | -------------------- | --------------------------------- |
| assessment_id | TEXT | PRIMARY KEY          | Unique assessment ID              |
| course_id     | TEXT | NOT NULL, FOREIGN KEY | References `courses.course_id`   |
| title         | TEXT | NOT NULL             | Assessment title                  |
| category      | TEXT | NOT NULL             | Category (Quiz, Exam, Project, …) |
| score         | REAL | NOT NULL             | Score earned                      |
| max_score     | REAL | NOT NULL             | Maximum possible score            |
| weight        | REAL | NOT NULL             | Weight (per grading policy)       |
| date          | TEXT | NOT NULL             | ISO-8601 date                     |

```sql
FOREIGN KEY (course_id) REFERENCES courses(course_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
```

---

## 5. `time_slots`

A time slot belongs to a course.

| Column       | Type | Constraint           | Description                     |
| ------------ | ---- | -------------------- | ------------------------------- |
| time_slot_id | TEXT | PRIMARY KEY          | Unique time slot ID             |
| course_id    | TEXT | NOT NULL, FOREIGN KEY | References `courses.course_id` |
| day_of_week  | TEXT | NOT NULL             | DayOfWeek name (e.g. "MONDAY")  |
| start_time   | TEXT | NOT NULL             | Start time (ISO-8601 local)     |
| end_time     | TEXT | NOT NULL             | End time (ISO-8601 local)       |
| room         | TEXT | NOT NULL             | Room / location                 |

```sql
FOREIGN KEY (course_id) REFERENCES courses(course_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
```

---

## Cascade summary

| Relationship                | Effect                                     |
| --------------------------- | ------------------------------------------ |
| `students` → `terms`        | Delete student = delete related terms      |
| `terms` → `courses`         | Delete term = delete related courses       |
| `courses` → `assessments`   | Delete course = delete related assessments |
| `courses` → `time_slots`    | Delete course = delete related time slots  |
