package org.vitrivr.cineast.core.util.ocrhelpers;

import java.util.List;

public class RecognitionEvaluationResult {
    double iouMean, iouVariance;
    double trigramMean, trigramVariance;
    int numberLevenshteinZero, numberLevenshteinOne, numberLevenshteinTwo, numberLevenshteinThreeOrMore;
    int numberOfConsideredGroundTruths;
    List<TextboxWithText> textboxesWithText;

    /**
     * A container that contains all relevant values produced by the recognition evaluation
     * @param iouMean The intersection over union of single letters
     * @param iouVariance The variance of the iou
     * @param trigramMean The average jaccard trigram distance
     * @param trigramVariance The variance of the trigram similarities
     * @param numberLevenshteinZero The number of words where the levenshtein distance was 0
     * @param numberLevenshteinOne The number of words where the levenshtein distance was 1
     * @param numberLevenshteinTwo The number of words where the levenshtein distance was 2
     * @param numberLevenshteinThreeOrMore The number of words where the levenshtein distance was 3 or more
     * @param numberOfConsideredGroundTruths The number of textboxes who had text assigned in the GT
     * @param textboxesWithText A list of TextboxWithText objects containing textbox, and both text in ground truth and text found
     */
    public RecognitionEvaluationResult(double iouMean, double iouVariance, double trigramMean, double trigramVariance, int numberLevenshteinZero, int numberLevenshteinOne, int numberLevenshteinTwo, int numberLevenshteinThreeOrMore, int numberOfConsideredGroundTruths, List<TextboxWithText> textboxesWithText) {
        this.iouMean = iouMean;
        this.iouVariance = iouVariance;
        this.trigramMean = trigramMean;
        this.trigramVariance = trigramVariance;
        this.numberLevenshteinZero = numberLevenshteinZero;
        this.numberLevenshteinOne = numberLevenshteinOne;
        this.numberLevenshteinTwo = numberLevenshteinTwo;
        this.numberLevenshteinThreeOrMore = numberLevenshteinThreeOrMore;
        this.numberOfConsideredGroundTruths = numberOfConsideredGroundTruths;
        this.textboxesWithText = textboxesWithText;
    }
}

