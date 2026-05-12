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

## Grading policy storage strategy

`courses.grading_type` is a discriminator string; `courses.grading_config` is a JSON blob whose shape depends on the discriminator. On load, `GradingPolicyFactory` reads `grading_type`, picks the concrete `GradingPolicy` class, and deserializes `grading_config` into that class's config.

| `grading_type` | Java class           | `grading_config` JSON shape                                                                       |
| -------------- | -------------------- | ------------------------------------------------------------------------------------------------- |
| `"weighted"`   | `WeightedGrading`    | `{"categoryWeights": {"Exams": 0.60, "Homework": 0.40}}`                                          |
| `"points"`     | `PointsBasedGrading` | `{"totalPossible": 500.0}`                                                                        |
| `"curved"`     | `CurvedGrading`      | `{"curveAmount": 5.0, "baseType": "weighted", "baseConfig": {"categoryWeights": {"Exams": 1.0}}}` |

- `CurvedGrading` is a decorator. Its `grading_config` nests the wrapped policy's discriminator (`baseType`) and config (`baseConfig`) recursively, so `GradingPolicyFactory` rebuilds the inner policy first, then wraps it.
- Serialization and deserialization of `grading_config` use **Gson**, which is already a project dependency. The JSON is stored verbatim in the `TEXT` column.

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

All cascades are verified by `SqliteDataStoreTest` (see AQ-023).

---

## ID generation

- All `*_id` columns (`student_id`, `term_id`, `course_id`, `assessment_id`, `time_slot_id`) are application-generated `TEXT` primary keys — **not** SQLite `AUTOINCREMENT`.
- IDs are generated using `java.util.UUID.randomUUID().toString()` at object construction time in the Java model layer.
- Rationale: avoids coupling to SQLite's internal `rowid`, simplifies upsert logic (the ID is known before the row exists, so `ON CONFLICT(id) DO UPDATE` works without a round-trip), and makes future export/import to other stores portable.

---

## Runtime pragmas

These pragmas are connection-scoped in SQLite and must be set on every new `Connection`:

- `PRAGMA foreign_keys = ON;` — required for the `ON DELETE CASCADE` / `ON UPDATE CASCADE` clauses to fire. SQLite's default is `OFF`. Currently set in `SqliteDataStore`'s constructor.
- `PRAGMA journal_mode = WAL;` — recommended for concurrent read/write workloads (writers don't block readers). **Not yet enabled in code**; documented here as the target configuration.
