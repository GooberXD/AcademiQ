package com.academiq.grading;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;

public final class GradingPolicyFactory {

    private static final Gson GSON = new Gson();

    private GradingPolicyFactory() {}

    public static GradingPolicy fromTypeAndConfig(String gradingType, String gradingConfig) {
        if (gradingType == null) {
            throw new IllegalArgumentException("gradingType is null");
        }
        switch (gradingType) {
            case "weighted": {
                Type mapType = new TypeToken<Map<String, Double>>() {}.getType();
                JsonObject obj = GSON.fromJson(gradingConfig, JsonObject.class);
                Map<String, Double> weights = GSON.fromJson(obj.get("categoryWeights"), mapType);
                return new WeightedGrading(weights);
            }
            case "points": {
                JsonObject obj = GSON.fromJson(gradingConfig, JsonObject.class);
                double total = obj.get("totalPossible").getAsDouble();
                return new PointsBasedGrading(total);
            }
            case "curved": {
                JsonObject obj = GSON.fromJson(gradingConfig, JsonObject.class);
                double curve = obj.get("curveAmount").getAsDouble();
                String baseType = obj.get("baseType").getAsString();
                String baseConfig = obj.get("baseConfig").toString();
                GradingPolicy base = fromTypeAndConfig(baseType, baseConfig);
                return new CurvedGrading(curve, base);
            }
            default:
                throw new IllegalArgumentException("Unknown grading type: " + gradingType);
        }
    }

    public static String toType(GradingPolicy policy) {
        if (policy instanceof WeightedGrading) return "weighted";
        if (policy instanceof PointsBasedGrading) return "points";
        if (policy instanceof CurvedGrading) return "curved";
        throw new IllegalArgumentException("Unknown grading policy: " + policy.getClass().getName());
    }

    public static String toConfig(GradingPolicy policy) {
        if (policy instanceof WeightedGrading) {
            Map<String, Double> weights = readField(policy, "categoryWeights");
            JsonObject obj = new JsonObject();
            obj.add("categoryWeights", GSON.toJsonTree(weights));
            return obj.toString();
        }
        if (policy instanceof PointsBasedGrading) {
            double total = (double) readField(policy, "totalPossible");
            JsonObject obj = new JsonObject();
            obj.addProperty("totalPossible", total);
            return obj.toString();
        }
        if (policy instanceof CurvedGrading) {
            double curve = (double) readField(policy, "curveAmount");
            GradingPolicy base = readField(policy, "basePolicy");
            JsonObject obj = new JsonObject();
            obj.addProperty("curveAmount", curve);
            obj.addProperty("baseType", toType(base));
            obj.add("baseConfig", GSON.fromJson(toConfig(base), JsonObject.class));
            return obj.toString();
        }
        throw new IllegalArgumentException("Unknown grading policy: " + policy.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private static <T> T readField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read field " + fieldName + " on " + target.getClass(), e);
        }
    }
}
