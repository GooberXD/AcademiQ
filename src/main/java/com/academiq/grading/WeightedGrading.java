package com.academiq.grading;

import com.academiq.model.Assessment;

import java.util.List;
import java.util.Map;

public class WeightedGrading implements GradingPolicy {

    private final Map<String, Double> categoryWeights;

    public WeightedGrading(Map<String, Double> categoryWeights) {
        this.categoryWeights = categoryWeights;
    }

    @Override
    public double computeFinalGrade(List<Assessment> assessments) {
        if (assessments == null || assessments.isEmpty()) {
            return 0.0;
        }

        double weightedSum = 0.0;
        double includedWeight = 0.0;

        for (Map.Entry<String, Double> entry : categoryWeights.entrySet()) {
            String category = entry.getKey();
            double categoryWeight = entry.getValue();

            double pctWeightSum = 0.0;
            double weightSum = 0.0;
            for (Assessment a : assessments) {
                if (a.isGraded() && category.equals(a.getCategory())) {
                    pctWeightSum += a.getPercentage() * a.getWeight();
                    weightSum += a.getWeight();
                }
            }

            if (weightSum == 0.0) {
                continue;
            }

            double categoryAvg = pctWeightSum / weightSum;
            weightedSum += categoryAvg * categoryWeight;
            includedWeight += categoryWeight;
        }

        if (includedWeight == 0.0) {
            return 0.0;
        }

        double finalGrade = weightedSum / includedWeight;
        return Math.max(0.0, Math.min(100.0, finalGrade));
    }

    @Override
    public String getBreakdown(List<Assessment> assessments) {
        StringBuilder sb = new StringBuilder();
        double weightedSum = 0.0;
        double includedWeight = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<String, Double> entry : categoryWeights.entrySet()) {
            String category = entry.getKey();
            double categoryWeight = entry.getValue();
            totalWeight += categoryWeight;

            double pctWeightSum = 0.0;
            double weightSum = 0.0;
            if (assessments != null) {
                for (Assessment a : assessments) {
                    if (a.isGraded() && category.equals(a.getCategory())) {
                        pctWeightSum += a.getPercentage() * a.getWeight();
                        weightSum += a.getWeight();
                    }
                }
            }

            double pctLabel = categoryWeight * 100.0;
            if (weightSum == 0.0) {
                sb.append(String.format("%s (%.1f%%): no graded assessments%n", category, pctLabel));
            } else {
                double categoryAvg = pctWeightSum / weightSum;
                sb.append(String.format("%s (%.1f%%): %.2f%n", category, pctLabel, categoryAvg));
                weightedSum += categoryAvg * categoryWeight;
                includedWeight += categoryWeight;
            }
        }

        sb.append("---").append(System.lineSeparator());
        if (includedWeight == 0.0) {
            sb.append("Final Grade: 0.00 (no graded assessments)");
        } else {
            double finalGrade = Math.max(0.0, Math.min(100.0, weightedSum / includedWeight));
            double coveragePct = (totalWeight == 0.0) ? 0.0 : (includedWeight / totalWeight) * 100.0;
            sb.append(String.format("Final Grade: %.2f (based on %.1f%% of total weight)", finalGrade, coveragePct));
        }

        return sb.toString();
    }

    @Override
    public double projectNeeded(List<Assessment> assessments, double targetGrade) {
        if (assessments == null || assessments.isEmpty()) {
            return -1.0;
        }

        double knownContribution = 0.0;
        double unknownCoefficient = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<String, Double> entry : categoryWeights.entrySet()) {
            String category = entry.getKey();
            double categoryWeight = entry.getValue();

            double gradedPctWeight = 0.0;
            double gradedWeight = 0.0;
            double ungradedWeight = 0.0;

            for (Assessment a : assessments) {
                if (!category.equals(a.getCategory())) {
                    continue;
                }
                if (a.isGraded()) {
                    gradedPctWeight += a.getPercentage() * a.getWeight();
                    gradedWeight += a.getWeight();
                } else {
                    ungradedWeight += a.getWeight();
                }
            }

            double catTotalWeight = gradedWeight + ungradedWeight;
            if (catTotalWeight == 0.0) {
                continue;
            }

            totalWeight += categoryWeight;
            knownContribution += categoryWeight * (gradedPctWeight / catTotalWeight);
            unknownCoefficient += categoryWeight * (ungradedWeight / catTotalWeight);
        }

        if (totalWeight == 0.0) {
            return -1.0;
        }

        if (unknownCoefficient == 0.0) {
            double currentGrade = knownContribution / totalWeight;
            return currentGrade < targetGrade ? -1.0 : 0.0;
        }

        double x = (targetGrade * totalWeight - knownContribution) / unknownCoefficient;

        if (x > 100.0) {
            return -1.0;
        }
        if (x < 0.0) {
            return 0.0;
        }
        return x;
    }
}
