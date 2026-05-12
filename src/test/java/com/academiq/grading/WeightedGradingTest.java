package com.academiq.grading;

import com.academiq.model.Assessment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedGradingTest {

    private static final LocalDate D = LocalDate.of(2025, 1, 1);
    private static final double DELTA = 0.01;

    private static Map<String, Double> weights(String k1, double v1, String k2, double v2) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private static Map<String, Double> weights(String k1, double v1) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put(k1, v1);
        return m;
    }

    @Test
    void normalWeightedCalculation() {
        WeightedGrading wg = new WeightedGrading(weights("Exams", 0.60, "Homework", 0.40));
        List<Assessment> assessments = Arrays.asList(
                new Assessment("Midterm", "Exams", 80, 100, 1.0, D),
                new Assessment("HW1", "Homework", 90, 100, 1.0, D)
        );

        assertEquals(84.0, wg.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void zeroScore() {
        WeightedGrading wg = new WeightedGrading(weights("Exams", 0.60, "Homework", 0.40));
        List<Assessment> assessments = Arrays.asList(
                new Assessment("Midterm", "Exams", 0, 100, 1.0, D),
                new Assessment("HW1", "Homework", 0, 100, 1.0, D)
        );

        assertEquals(0.0, wg.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void zeroWeightCategory() {
        WeightedGrading wg = new WeightedGrading(weights("Exams", 1.0, "Extra Credit", 0.0));
        List<Assessment> assessments = Arrays.asList(
                new Assessment("Midterm", "Exams", 85, 100, 1.0, D),
                new Assessment("Bonus", "Extra Credit", 100, 100, 1.0, D)
        );

        assertEquals(85.0, wg.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void emptyAssessmentList() {
        WeightedGrading wg = new WeightedGrading(weights("Exams", 1.0));
        assertEquals(0.0, wg.computeFinalGrade(Collections.emptyList()), DELTA);
    }

    @Test
    void singleAssessment() {
        WeightedGrading wg = new WeightedGrading(weights("Exams", 1.0));
        List<Assessment> assessments = Collections.singletonList(
                new Assessment("Midterm", "Exams", 75, 100, 1.0, D)
        );

        assertEquals(75.0, wg.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void missingCategoryRedistributesWeight() {
        WeightedGrading wg = new WeightedGrading(weights("Exams", 0.50, "Homework", 0.50));
        List<Assessment> assessments = Collections.singletonList(
                new Assessment("Midterm", "Exams", 90, 100, 1.0, D)
        );

        assertEquals(90.0, wg.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void multipleAssessmentsPerCategory() {
        WeightedGrading wg = new WeightedGrading(weights("Exams", 0.50, "Homework", 0.50));
        List<Assessment> assessments = Arrays.asList(
                new Assessment("Midterm", "Exams", 70, 100, 0.4, D),
                new Assessment("Final", "Exams", 90, 100, 0.6, D),
                new Assessment("HW1", "Homework", 100, 100, 1.0, D)
        );

        assertEquals(91.0, wg.computeFinalGrade(assessments), DELTA);
    }

    @Test
    void projectionBasicCase() {
        WeightedGrading wg = new WeightedGrading(weights("Exams", 0.50, "Homework", 0.50));
        double target = 85.0;
        List<Assessment> assessments = Arrays.asList(
                new Assessment("Midterm", "Exams", 80, 100, 1.0, D),
                new Assessment("Final", "Homework", -1, 100, 1.0, D)
        );

        double needed = wg.projectNeeded(assessments, target);
        assertTrue(needed >= 0.0 && needed <= 100.0,
                "projected score should be feasible, got " + needed);

        // Feed the projected score back in and verify it hits the target.
        List<Assessment> filled = Arrays.asList(
                new Assessment("Midterm", "Exams", 80, 100, 1.0, D),
                new Assessment("Final", "Homework", needed, 100, 1.0, D)
        );
        assertEquals(target, wg.computeFinalGrade(filled), DELTA);
    }

    @Test
    void projectionImpossible() {
        WeightedGrading wg = new WeightedGrading(weights("Exams", 0.50, "Homework", 0.50));
        // Exams already done at 40. To reach 95 final, Homework would need 150.
        List<Assessment> assessments = Arrays.asList(
                new Assessment("Midterm", "Exams", 40, 100, 1.0, D),
                new Assessment("HW1", "Homework", -1, 100, 1.0, D)
        );

        assertEquals(-1.0, wg.projectNeeded(assessments, 95.0), DELTA);
    }

    @Test
    void breakdownContainsCategories() {
        WeightedGrading wg = new WeightedGrading(weights("Exams", 0.60, "Homework", 0.40));
        List<Assessment> assessments = Arrays.asList(
                new Assessment("Midterm", "Exams", 80, 100, 1.0, D),
                new Assessment("HW1", "Homework", 90, 100, 1.0, D)
        );

        String breakdown = wg.getBreakdown(assessments);
        assertTrue(breakdown.contains("Exams"), "breakdown should mention Exams");
        assertTrue(breakdown.contains("Homework"), "breakdown should mention Homework");
        assertTrue(breakdown.contains("Final Grade"), "breakdown should include a final grade line");
    }
}
