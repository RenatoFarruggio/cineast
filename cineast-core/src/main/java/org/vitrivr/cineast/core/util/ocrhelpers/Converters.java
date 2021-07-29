package org.vitrivr.cineast.core.util.ocrhelpers;

import ai.djl.modality.Classifications;
import ai.djl.modality.cv.output.BoundingBox;
import org.vitrivr.cineast.core.util.ocrhelpers.Textbox;
import ai.djl.modality.cv.output.DetectedObjects;

import java.util.ArrayList;
import java.util.List;

public class Converters {
    public static List<Textbox> DetectedObjects2ListOfTextboxes(DetectedObjects detectedObjects, int imageWidth, int imageHeight) {
        List<Textbox> textboxes = new ArrayList<>();
        for (int i = 0; i < detectedObjects.getNumberOfObjects(); i++) {
            DetectedObjects.DetectedObject det = detectedObjects.item(i);
            Textbox detectedTextbox = Textbox.fromDetectedObject_extended(det, imageWidth, imageHeight);
            textboxes.add(detectedTextbox);
        }
        return textboxes;
    }

    public static DetectedObjects listOfDetectedObject2detectedObjects(List<DetectedObjects.DetectedObject> detectedObjectList) {
        return null;
    }

    public static List<DetectedObjects.DetectedObject> detectedObjects2listOfDetectedObject(DetectedObjects detectedObjects) {
        List<DetectedObjects.DetectedObject> objectList = new ArrayList<>();
        for (int i = 0; i < detectedObjects.getNumberOfObjects(); i++) {
            DetectedObjects.DetectedObject det = detectedObjects.item(i);
            objectList.add(det);
        }
        return objectList;
    }

    public static DetectedObjects getGroundTruthWithTextFromGroundTruth(DetectedObjects input) {
        List<String> words = new ArrayList<>();
        List<Double> probabilities = new ArrayList<>();
        List<BoundingBox> boundingBoxes = new ArrayList<>();

        for (Classifications.Classification classification : input.items()) {
            DetectedObjects.DetectedObject detectedObject = (DetectedObjects.DetectedObject)classification;
            if (detectedObject.getClassName().equals("###")
                || detectedObject.getClassName().equals("")) {
                continue;
            }

            String word = detectedObject.getClassName();
            Double probability = detectedObject.getProbability();
            BoundingBox boundingBox = detectedObject.getBoundingBox();

            words.add(word);
            probabilities.add(probability);
            boundingBoxes.add(boundingBox);
        }

        return new DetectedObjects(words, probabilities, boundingBoxes);
    }
}
