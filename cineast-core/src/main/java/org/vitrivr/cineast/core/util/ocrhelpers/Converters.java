package org.vitrivr.cineast.core.util.ocrhelpers;

import ai.djl.modality.Classifications;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

public class Converters {

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

    public static Rectangle extendRect(Rectangle rect) {
        double xMin = rect.getX();
        double yMin = rect.getY();
        double width = rect.getWidth();
        double height = rect.getHeight();

        double centerx = xMin + width / 2;
        double centery = yMin + height / 2;

        if (width > height) {
            width += height * 2.0;
            height *= 3.0;
        } else {
            height += width * 2.0;
            width *= 3.0;
        }

        // clamping
        double newX = centerx - width / 2 < 0 ? 0 : centerx - width / 2;
        double newY = centery - height / 2 < 0 ? 0 : centery - height / 2;
        double newWidth = newX + width > 1 ? 1 - newX : width;
        double newHeight = newY + height > 1 ? 1 - newY : height;
        return new Rectangle(newX, newY, newWidth, newHeight);
    }

    public static Rectangle reduceRect(Rectangle extended) {

        double xmin = extended.getX();
        double ymin = extended.getY();
        double width = extended.getWidth();
        double height = extended.getHeight();

        double centerx = xmin + width / 2;
        double centery = ymin + height / 2;
        if (width > height) {
            height /= 3.0;
            width -= height * 2.0;
        } else {
            width /= 3.0;
            height -= width * 2.0;
        }
        double newX = centerx - width / 2;
        double newY = centery - height / 2;

        return new Rectangle(newX,newY,width,height);
    }

    public static Rectangle rectRelative2Absolute(Rectangle rect, int imgWidth, int imgHeight) {
        return new Rectangle(
                rect.getX() * imgWidth,
                rect.getY() * imgHeight,
                rect.getWidth() * imgWidth,
                rect.getHeight() * imgHeight);
    }

    public static Rectangle rectAbsolute2Relative(Rectangle rect, int imgWidth, int imgHeight) {
        throw new NotImplementedException("This method hasn't been used before.");
    }
}
