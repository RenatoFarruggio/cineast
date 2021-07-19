package org.vitrivr.cineast.core.util.ocrhelpers;

import ai.djl.modality.cv.output.DetectedObjects;

import java.util.*;

public class DetectionEvaluationResult {
    double avgIOU;
    private final Set<DetectedObjects.DetectedObject> tp, fn, fp;
    private final Map<DetectedObjects.DetectedObject, DetectedObjects.DetectedObject> mostFittingGroundTruths;

    DetectionEvaluationResult(double avgIOU, Set<DetectedObjects.DetectedObject> tp, Set<DetectedObjects.DetectedObject> fn, Set<DetectedObjects.DetectedObject> fp, HashMap<DetectedObjects.DetectedObject, DetectedObjects.DetectedObject> mostFittingGroundTruths) {
        this.avgIOU = avgIOU;
        this.tp = tp;
        this.fn = fn;
        this.fp = fp;
        this.mostFittingGroundTruths = mostFittingGroundTruths;
    }

    Set<DetectedObjects.DetectedObject> getTp() {
        return this.tp;
    }

    Set<DetectedObjects.DetectedObject> getFp() {
        return this.fp;
    }

    Set<DetectedObjects.DetectedObject> getFn() {
        return this.fn;
    }

    Set<DetectedObjects.DetectedObject> getGtDetected() {
        return new HashSet<>(mostFittingGroundTruths.values());
    }

    DetectedObjects.DetectedObject getMostFittingGroundTruth(DetectedObjects.DetectedObject foundObject) {
        return mostFittingGroundTruths.get(foundObject);
    }

}
