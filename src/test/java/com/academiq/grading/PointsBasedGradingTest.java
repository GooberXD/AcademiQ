package com.academiq.grading;

import com.academiq.model.Assessment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PointsBasedGradingTest {

    private static final LocalDate DATE = LocalDate.of(2025, 1, 1);
    private static final String CAT = "General";
    private static final double W = 1.0;

    private Assessment a(String title, double score, double max) {
        return new Assessment(title, CAT, score, max, W, DATE);
    }

    @Test
    void normalCalculation() {
        PointsBasedGrading policy = new PointsBasedGrading(500);
        List<Assessment> list = Arrays.asList(
                a("A1", 50, 50),
                a("A2", 80, 100),
                a("A3", 45, 50)
        );
        assertEquals(87.5, policy.computeFinalGrade(list), 1e-9);
    }

    @Test
    void zeroTotalPossible() {
        PointsBasedGrading policy = new PointsBasedGrading(0);
        assertEquals(0.0, policy.computeFinalGrade(Collections.emptyList()), 1e-9);
    }

    @Test
    void emptyList() {
        PointsBasedGrading policy = new PointsBasedGrading(100);
        assertEquals(0.0, policy.computeFinalGrade(Collections.emptyList()), 1e-9);
    }

    @Test
    void singleAssessment() {
        PointsBasedGrading policy = new PointsBasedGrading(100);
        List<Assessment> list = Arrays.asList(a("A1", 72, 100));
        assertEquals(72.0, policy.computeFinalGrade(list), 1e-9);
    }

    @Test
    void perfectScore() {
        PointsBasedGrading policy = new PointsBasedGrading(300);
        List<Assessment> list = Arrays.asList(
                a("A1", 100, 100),
                a("A2", 100, 100),
                a("A3", 100, 100)
        );
        assertEquals(100.0, policy.computeFinalGrade(list), 1e-9);
    }

    @Test
    void allZeroScores() {
        PointsBasedGrading policy = new PointsBasedGrading(200);
        List<Assessment> list = Arrays.asList(
                a("A1", 0, 100),
                a("A2", 0, 100)
        );
        assertEquals(0.0, policy.computeFinalGrade(list), 1e-9);
    }

    @Test
    void mixedGradedAndUngraded() {
        PointsBasedGrading policy = new PointsBasedGrading(500);
        List<Assessment> list = Arrays.asList(
                a("A1", 80, 100),
                a("A2", 90, 100),
                a("A3", -1, 100)
        );
        assertEquals(85.0, policy.computeFinalGrade(list), 1e-9);
    }

    @Test
    void projectionBasicCase() {
        PointsBasedGrading policy = new PointsBasedGrading(400);
        List<Assessment> list = Arrays.asList(
                a("A1", 80, 100),
                a("A2", 80, 100)
        );
        assertEquals(100.0, policy.projectNeeded(list, 90.0), 1e-9);
    }

    @Test
    void projectionImpossible() {
        PointsBasedGrading policy = new PointsBasedGrading(300);
        List<Assessment> list = Arrays.asList(
                a("A1", 50, 100),
                a("A2", 50, 100)
        );
        assertEquals(-1.0, policy.projectNeeded(list, 90.0), 1e-9);
    }

    @Test
    void projectionAlreadyAchieved() {
        PointsBasedGrading policy = new PointsBasedGrading(200);
        List<Assessment> list = Arrays.asList(
                a("A1", 95, 100),
                a("A2", 95, 100)
        );
        assertEquals(0.0, policy.projectNeeded(list, 80.0), 1e-9);
    }

    @Test
    void breakdownShowsTotals() {
        PointsBasedGrading policy = new PointsBasedGrading(500);
        List<Assessment> list = Arrays.asList(
                a("Quiz 1", 50, 50),
                a("Midterm", 80, 100)
        );
        String breakdown = policy.getBreakdown(list);
        assertTrue(breakdown.contains("Quiz 1"));
        assertTrue(breakdown.contains("Midterm"));
        assertTrue(breakdown.contains("50"));
        assertTrue(breakdown.contains("80"));
        assertTrue(breakdown.contains("Total"));
        assertTrue(breakdown.contains("500"));
    }
}
