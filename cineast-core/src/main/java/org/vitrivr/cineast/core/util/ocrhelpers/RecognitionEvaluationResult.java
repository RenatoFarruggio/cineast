package org.vitrivr.cineast.core.util.ocrhelpers;

public class RecognitionEvaluationResult {
    double avgIOU, avgJaccardTrigramDistance;
    int numberOfConsideredGroundTruths;

    /**
     *
     * @param avgIOU The intersection over union of single letters
     * @param avgJaccardTrigramDistance The average jaccard trigram distance
     * @param numberOfConsideredGroundTruths The number of textboxes who had text assigned in the GT
     */
    RecognitionEvaluationResult(double avgIOU, double avgJaccardTrigramDistance, int numberOfConsideredGroundTruths) {
        this.avgIOU = avgIOU;
        this.avgJaccardTrigramDistance = avgJaccardTrigramDistance;
        this.numberOfConsideredGroundTruths = numberOfConsideredGroundTruths;
    }
}

