package org.vitrivr.cineast.core.util.ocrhelpers;

import java.util.List;

public class RecognitionEvaluationResult {
    double avgIOU;
    double avgJaccardTrigramDistance;
    int numberOfConsideredGroundTruths;
    List<TextboxWithText> textboxesWithText;

    /**
     *
     * @param avgIOU The intersection over union of single letters
     * @param avgJaccardTrigramDistance The average jaccard trigram distance
     * @param numberOfConsideredGroundTruths The number of textboxes who had text assigned in the GT
     * @param textboxesWithText A list of TextboxWithText objects containing textbox, and both text in ground truth and text found
     */
    RecognitionEvaluationResult(double avgIOU, double avgJaccardTrigramDistance, int numberOfConsideredGroundTruths, List<TextboxWithText> textboxesWithText) {
        this.avgIOU = avgIOU;
        this.avgJaccardTrigramDistance = avgJaccardTrigramDistance;
        this.numberOfConsideredGroundTruths = numberOfConsideredGroundTruths;
        this.textboxesWithText = textboxesWithText;
    }
}

