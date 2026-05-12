package com.academiq.grading;

import com.academiq.model.Assessment;

import java.util.List;

public class PointsBasedGrading implements GradingPolicy {

    private final double totalPossible;

    public PointsBasedGrading(double totalPossible) {
        this.totalPossible = totalPossible;
    }

    @Override
    public double computeFinalGrade(List<Assessment> assessments) {
        if (assessments == null || assessments.isEmpty()) {
            return 0.0;
        }
        double totalEarned = 0.0;
        double totalMaxScore = 0.0;
        for (Assessment a : assessments) {
            if (a.isGraded()) {
                totalEarned += a.getScore();
                totalMaxScore += a.getMaxScore();
            }
        }
        if (totalMaxScore == 0) {
            return 0.0;
        }
        return (totalEarned / totalMaxScore) * 100;
    }

    @Override
    public String getBreakdown(List<Assessment> assessments) {
        StringBuilder sb = new StringBuilder();
        double totalEarned = 0.0;
        double totalMaxScore = 0.0;
        if (assessments != null) {
            for (Assessment a : assessments) {
                if (a.isGraded()) {
                    sb.append(String.format("%s: %.1f / %.1f%n", a.getTitle(), a.getScore(), a.getMaxScore()));
                    totalEarned += a.getScore();
                    totalMaxScore += a.getMaxScore();
                }
            }
        }
        sb.append("---").append(System.lineSeparator());
        double pct = totalMaxScore == 0 ? 0.0 : (totalEarned / totalMaxScore) * 100;
        sb.append(String.format("Total: %.1f / %.1f (%.2f%%)%n", totalEarned, totalMaxScore, pct));
        sb.append(String.format("Course total possible: %.1f", totalPossible));
        return sb.toString();
    }

    @Override
    public double projectNeeded(List<Assessment> assessments, double targetGrade) {
        if (assessments == null || assessments.isEmpty()) {
            return -1.0;
        }
        if (totalPossible <= 0) {
            return -1.0;
        }
        double earnedSoFar = 0.0;
        double maxScoreSoFar = 0.0;
        for (Assessment a : assessments) {
            if (a.isGraded()) {
                earnedSoFar += a.getScore();
                maxScoreSoFar += a.getMaxScore();
            }
        }
        double remainingMaxScore = totalPossible - maxScoreSoFar;
        double requiredPoints = (targetGrade / 100.0) * totalPossible - earnedSoFar;

        if (remainingMaxScore <= 0) {
            return requiredPoints <= 0 ? 0.0 : -1.0;
        }

        double requiredPercentage = (requiredPoints / remainingMaxScore) * 100;
        if (requiredPercentage > 100) {
            return -1.0;
        }
        if (requiredPercentage < 0) {
            return 0.0;
        }
        return requiredPercentage;
    }
}
