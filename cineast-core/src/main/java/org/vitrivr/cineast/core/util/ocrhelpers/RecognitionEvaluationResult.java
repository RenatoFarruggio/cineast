package org.vitrivr.cineast.core.util.ocrhelpers;

import java.util.List;

public class RecognitionEvaluationResult {
    double iouMean, iouVariance;
    double trigramMean, trigramVariance;
    int numberOfConsideredGroundTruths;
    List<TextboxWithText> textboxesWithText;

    /**
     * A container that contains all relevant values produced by the recognition evaluation
     * @param iouMean The intersection over union of single letters
     * @param iouVariance The variance of the iou
     * @param trigramMean The average jaccard trigram distance
     * @param trigramVariance The variance of the trigram similarities
     * @param numberOfConsideredGroundTruths The number of textboxes who had text assigned in the GT
     * @param textboxesWithText A list of TextboxWithText objects containing textbox, and both text in ground truth and text found
     */
    public RecognitionEvaluationResult(double iouMean, double iouVariance, double trigramMean, double trigramVariance, int numberOfConsideredGroundTruths, List<TextboxWithText> textboxesWithText) {
        this.iouMean = iouMean;
        this.iouVariance = iouVariance;
        this.trigramMean = trigramMean;
        this.trigramVariance = trigramVariance;
        this.numberOfConsideredGroundTruths = numberOfConsideredGroundTruths;
        this.textboxesWithText = textboxesWithText;
    }
}

