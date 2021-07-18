package org.vitrivr.cineast.core.util.ocrhelpers;

public class RecognitionEvaluationResult {
    double iou, jaccardTrigramDistance;

    /**
     *
     * @param iou  The intersection over union distance of single letters
     * @param jaccardTrigramDistance
     */
    RecognitionEvaluationResult(double iou, double jaccardTrigramDistance) {
        this.iou = iou;
        this.jaccardTrigramDistance = jaccardTrigramDistance;
    }
}

