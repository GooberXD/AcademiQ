package com.academiq.grading;

import com.academiq.model.Assessment;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GradingPolicyFactoryTest {

    @Test
    void testWeightedRoundTrip() {
        WeightedGrading original = new WeightedGrading(Map.of("Exams", 0.6, "HW", 0.4));
        String type = GradingPolicyFactory.toType(original);
        String config = GradingPolicyFactory.toConfig(original);

        assertEquals("weighted", type);

        GradingPolicy restored = GradingPolicyFactory.fromTypeAndConfig(type, config);
        assertTrue(restored instanceof WeightedGrading);

        List<Assessment> assessments = List.of(
                new Assessment("Midterm", "Exams", 85.0, 100.0, 1.0, LocalDate.of(2026, 3, 1)),
                new Assessment("HW1", "HW", 95.0, 100.0, 1.0, LocalDate.of(2026, 2, 1))
        );
        assertEquals(original.computeFinalGrade(assessments),
                restored.computeFinalGrade(assessments), 1e-9);
    }

    @Test
    void testPointsRoundTrip() {
        PointsBasedGrading original = new PointsBasedGrading(500.0);
        String type = GradingPolicyFactory.toType(original);
        String config = GradingPolicyFactory.toConfig(original);

        assertEquals("points", type);

        GradingPolicy restored = GradingPolicyFactory.fromTypeAndConfig(type, config);
        assertTrue(restored instanceof PointsBasedGrading);

        List<Assessment> assessments = List.of(
                new Assessment("Quiz", "Q", 90.0, 100.0, 1.0, LocalDate.of(2026, 3, 1))
        );
        assertEquals(original.computeFinalGrade(assessments),
                restored.computeFinalGrade(assessments), 1e-9);
    }

    @Test
    void testCurvedRoundTrip() throws Exception {
        WeightedGrading base = new WeightedGrading(Map.of("Exams", 1.0));
        CurvedGrading original = new CurvedGrading(5.0, base);

        String type = GradingPolicyFactory.toType(original);
        String config = GradingPolicyFactory.toConfig(original);

        assertEquals("curved", type);

        GradingPolicy restored = GradingPolicyFactory.fromTypeAndConfig(type, config);
        assertTrue(restored instanceof CurvedGrading);

        Field baseField = CurvedGrading.class.getDeclaredField("basePolicy");
        baseField.setAccessible(true);
        GradingPolicy restoredBase = (GradingPolicy) baseField.get(restored);
        assertTrue(restoredBase instanceof WeightedGrading);

        Field curveField = CurvedGrading.class.getDeclaredField("curveAmount");
        curveField.setAccessible(true);
        assertEquals(5.0, (double) curveField.get(restored), 1e-9);
    }

    @Test
    void testUnknownTypeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> GradingPolicyFactory.fromTypeAndConfig("unknown", "{}"));
    }
}
