package org.vitrivr.cineast.core.util.ocrhelpers;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.*;

public class IncidentalSceneTextLoader {
    public static DetectedObjects.DetectedObject GroundTruthDetectedObjectFromTxtLine(String lineOfTxtFile) {
        // TODO: implement me
        return null;
    }

    public static DetectedObjects GroundTruthDetectedObjectsFromTxtFile(Path pathToTxtFile, Image img) {
        // img is used only to detect its size
        int width = img.getWidth();
        int height = img.getHeight();
        File file = new File(pathToTxtFile.toString());
        String fileName = pathToTxtFile.getFileName().toString(); // gt_img_1.txt
        String imgName = (fileName.split("_", 2)[1]).split("\\.", 2)[0];

        final Scanner s;
        List<String> classNames = new ArrayList<>();
        List<Double> probabilities = new ArrayList<>();
        List<BoundingBox> boundingBoxes = new ArrayList<>();

        try {

            s = new Scanner(file);
            while(s.hasNextLine()) {
                final String line = s.nextLine();
                String[] stringValues = line.split(",");
                List<Integer> xValues = Arrays.asList(
                        Integer.parseInt(stringValues[0]),
                        Integer.parseInt(stringValues[2]),
                        Integer.parseInt(stringValues[4]),
                        Integer.parseInt(stringValues[6]));
                List<Integer> yValues = Arrays.asList(
                        Integer.parseInt(stringValues[1]),
                        Integer.parseInt(stringValues[3]),
                        Integer.parseInt(stringValues[5]),
                        Integer.parseInt(stringValues[7]));
                int xMin = Collections.min(xValues);
                int xMax = Collections.max(xValues);
                int yMin = Collections.min(yValues);
                int yMax = Collections.max(yValues);

                String word = stringValues[8];

                BoundingBox boundingBox = new Rectangle(
                        (double) (xMin) / width,
                        (double) (yMin) / height,
                        (double) (xMax - xMin) / width,
                        (double) (yMax - yMin) / height
                );

                classNames.add(word);
                probabilities.add(1.0d);
                boundingBoxes.add(reduce((Rectangle)boundingBox));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return new DetectedObjects(classNames, probabilities, boundingBoxes);
    }

    private static Rectangle reduce(Rectangle extended) {

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
        double newX = centerx - width / 2 < 0 ? 0 : centerx - width / 2;
        double newY = centery - height / 2 < 0 ? 0 : centery - height / 2;
        double newWidth = newX + width > 1 ? 1 - newX : width;
        double newHeight = newY + height > 1 ? 1 - newY : height;

        return new Rectangle(newX,newY,newWidth,newHeight);
    }

    public static Textbox GroundTruthTextboxFromTxtLine(String lineOfTxtFile) {
        // TODO: implement me
        return null;
    }

    public static List<Textbox> GroundTruthListOfTextboxesFromTxtFile(Path pathToTxtFile) {
        // TODO: implement me
        return null;
    }

    public static HashMap<String, String> loadRecognitionGroundTruthHashMap(Path pathToTxtFile) {
        HashMap<String, String> map = new HashMap<>();

        File file = new File(pathToTxtFile.toString());

        final Scanner s;

        try {

            s = new Scanner(file);
            while(s.hasNextLine()) {
                String line = s.nextLine();
                String[] values = line.split(",", 2);
                //System.out.println(values[0]);
                // Because the first word has weird chars in front of it
                String key = values[0].substring(values[0].indexOf("w"));
                String text = (values[1]).substring(2, values[1].length()-1);
                map.put(key, text);
                //System.out.println("---------");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return map;
    }
}
