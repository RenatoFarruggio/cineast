package org.vitrivr.cineast.core.util.ocrhelpers;

import ai.djl.modality.cv.output.DetectedObjects;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.HashSet;
import java.util.Set;

public class Distances {

    public static double iou(String word1, String word2) {
        if (word1.equals("") && word2.equals(""))
            return 1.0d;

        if (word1.equals("") || word2.equals(""))
            return 0.0d;

        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();

        char[] charArray1 = word1.toCharArray();
        char[] charArray2 = word2.toCharArray();

        HashSet<Character> set1 = new HashSet<>();
        HashSet<Character> set2 = new HashSet<>();

        for (char c : charArray1) {
            set1.add(c);
        }
        for (char c : charArray2) {
            set2.add(c);
        }

        HashSet<Character> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        HashSet<Character> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double)intersection.size() / (double)union.size();
    }

    public static double jaccardTrigramDistance(String word1, String word2) {
        if (word1.equals("") && word2.equals(""))
            return 1.0d;

        Set<String> set1 = trigrams(word1);
        Set<String> set2 = trigrams(word2);

        HashSet<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        HashSet<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double)intersection.size() / (double)union.size();
    }

    private static Set<String> trigrams(String word) {
        HashSet<String> trigrams = new HashSet<>();

        word = word.toLowerCase();

        if (word.length() == 0) {
            return trigrams;
        }

        if (word.length() == 1) {
            trigrams.add("##" + word);
            trigrams.add("#" + word + "#");
            trigrams.add(word + "##");
            return trigrams;
        }

        if (word.length() == 2) {
            trigrams.add("##" + word.charAt(0));
            trigrams.add("#" + word.substring(0,1) + word.substring(1,2));
            trigrams.add(word.substring(0,1) + word.substring(1,2) + "#");
            trigrams.add(word.substring(1,2) + "##");
            return trigrams;
        }

        word = "##" + word + "##";
        for (int i = 0; i < word.length()-2; i++) {
            trigrams.add(word.substring(i,i+3));
        }

        return trigrams;
    }

    public static int levenshteinDistance(String word1, String word2) {
        return LevenshteinDistance.getDefaultInstance().apply(word1, word2);
    }
}
