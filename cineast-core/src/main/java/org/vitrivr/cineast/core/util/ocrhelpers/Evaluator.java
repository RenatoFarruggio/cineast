package org.vitrivr.cineast.core.util.ocrhelpers;

// TODO: remove all camel cases
// TODO: add rotation model

import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;


// TODO: update CSV description
/* ***  CSV   ***

Information about picture
 - imageName
 - xResolution, yResolution

Information about time
 - msDet
 - msRec
 - msTot

Detections:
Information about how many shapes were detected
 - tp, fn, fp

Information about how good the detected shapes are overlapping with the ground truth
 - iouAverage

RECOGNITIONS:
jaccard trigram distance
jaccard distance (on characters), i.e. iou
 */


public class Evaluator {
    private static final Logger LOGGER = LogManager.getLogger();



    /**
     * Creates a csv file containing:
     *
     *  image_name,     name of the image
     *  x_res,          width of the image
     *  y_res,          height of the image
     *  ms_det,         time it took to do text detection
     *  ms_rec,         time it took to recognize the text in all textboxes
     *  ms_tot,         time it took to do text detection and text recognition
     *  tp,             true positives: textboxes that were detected correctly
     *  fn,             false negatives: textboxes in ground truth whose text was not detected
     *  fp,             false positives: textboxes detected where there is no text
     *  iou_avg,        average iou of detected textboxes: sum of all tp iou's divided by number of tp
     *  jaccard_distance_recognition,
     *                  intersection over union
     *  jaccard_trigram_distance_recognition,
     *                  sum of jaccard trigram distances between words found and words in ground truth
     */
    public void evaluateOnIncidentalSceneText() {
        // These are user inputs. TODO: Implement argument builder
        final String file_name_results = "resultsIncidentalSceneText.csv";
        final String path_to_images = "C:\\Users\\Renato\\Downloads\\IncidentalSceneText\\test\\ch4_test_images";
        final String path_to_ground_truth = "C:\\Users\\Renato\\Downloads\\IncidentalSceneText\\test\\Challenge4_Test_Task1_GT";
        final String path_to_output = "output/";
        final boolean save_output_images = true;

        File img_folder = new File(path_to_images);
        String[] imageNames = img_folder.list((dir, name) -> name.endsWith(".jpg"));

        assert imageNames != null;
        // TODO: find a solution for img sorting
        // Problem: this sorts images lexicographically, i.e.
        // img_1, img_10, img_100, img_101, img_102, ...
        // I want: img_1, img_2, img_3, ...
        Arrays.sort(imageNames);

        File detectionModel = new File(Paths.get("resources", "OCR-Models", "det_db.zip").toString());
        File recognitionModel = new File(Paths.get("resources", "OCR-Models", "rec_crnn.zip").toString());
        if (!(detectionModel.exists() && recognitionModel.exists())) {
            LOGGER.error("Please make sure that the ocr models are downloaded. Run './gradlew getExternalResources' to download them.");
        }
        System.out.println("The following error messages aren't actually errors.");
        // I just don't know how to get rid of them.
        InferenceModel inferenceModel = new InferenceModel(detectionModel, recognitionModel);
        System.out.println("You can ignore the above error message.");

        try (Writer w = new FileWriter(file_name_results)) {
            //try (Reader r = new FileReader(ground_truth))
            BufferedWriter csvWriter = new BufferedWriter(w);
            csvWriter.append("image_name,x_res,y_res,ms_det,ms_rec,ms_tot,tp,fn,fp,iou_avg,jaccard_distance_recognition,jaccard_trigram_distance_recognition");
            csvWriter.append(System.lineSeparator());

            // Define variables
            long start_det, end_det;  // detection
            long start_rec, end_rec;  // recognition
            long ms_det, ms_rec, ms_tot;  // milliseconds

            DetectionEvaluationResult detectionEvaluationResult;
            RecognitionEvaluationResult recognitionEvaluationResult;

            Image img;

            HashMap<String, List<Textbox>> groundTruthTextboxes = getGroundTruthTextboxes(path_to_ground_truth);
            DetectedObjects detectedBoxes;

            // PIPELINE //
            for (String imageName : imageNames) {
                Path imagePath = Paths.get(path_to_images,imageName);
                Path groundTruthFilePath = Paths.get(path_to_ground_truth, "gt_" + imageName.split("\\.")[0] + ".txt");

                // 1. Load image
                img = inferenceModel.loadImage(imagePath);

                // 2. Text detection
                start_det = System.currentTimeMillis();
                detectedBoxes = inferenceModel.detection(img);
                end_det = System.currentTimeMillis();

                /*
                System.out.println("// Detections \\\\");
                for (Classifications.Classification classification : detectedBoxes.items()) {
                    DetectedObjects.DetectedObject det = (DetectedObjects.DetectedObject) classification;
                    System.out.println(det);
                }
                System.out.println("\\\\ Detections //");*/

                // 3. Load ground truth boxes
                DetectedObjects groundTruthDetectedObjects =
                        IncidentalSceneTextLoader.GroundTruthDetectedObjectsFromTxtFile(
                                groundTruthFilePath,
                                img);
                DetectedObjects groundTruthWithTextDetectedObjects = Converters.getGroundTruthWithTextFromGroundTruth(groundTruthDetectedObjects);

                /*
                System.out.println("// Ground Truth \\\\");
                for (Classifications.Classification classification : groundTruthDetectedObjects.items()) {
                    DetectedObjects.DetectedObject GTdetctedObject = (DetectedObjects.DetectedObject) classification;
                    System.out.println(GTdetctedObject);
                }
                System.out.println("\\\\ Ground Truth //");

                System.out.println("// Ground Truth with text \\\\");
                for (Classifications.Classification classification : groundTruthWithTextDetectedObjects.items()) {
                    DetectedObjects.DetectedObject GTdetctedObject = (DetectedObjects.DetectedObject) classification;
                    System.out.println(GTdetctedObject);
                }
                System.out.println("\\\\ Ground Truth with text //");*/

                // 4. Text recognition on ground truth boxes
                start_rec = System.currentTimeMillis();
                List<String> recognizedText = inferenceModel.recognition(img, groundTruthWithTextDetectedObjects);  // On Ground Truth
                end_rec = System.currentTimeMillis();

                // 5. Evaluate detections
                System.out.println("Evaluating detections of img: " + imageName + " ...");
                String key = imageName.split("\\.", 2)[0];
                detectionEvaluationResult = evaluateDetections(img, detectedBoxes, groundTruthDetectedObjects);

                // 6. Evaluate recognitions
                recognitionEvaluationResult = evaluateRecognitions(recognizedText, groundTruthWithTextDetectedObjects); // TODO: add ground truth

                // 7. Calculate runtimes
                ms_tot = end_rec - start_det;
                ms_det = end_det - start_det;
                ms_rec = end_rec - start_rec;

                // 8. Write data to file
                String line = format_csv_line(imageName, img.getWidth(), img.getHeight(),
                        ms_det, ms_rec, ms_tot,
                        detectionEvaluationResult.getTp().size(),
                        detectionEvaluationResult.getFn().size(),
                        detectionEvaluationResult.getFp().size(),
                        detectionEvaluationResult.avgIOU,
                        recognitionEvaluationResult.iou,
                        recognitionEvaluationResult.jaccardTrigramDistance);
                csvWriter.write(line);
                System.out.println(line);

                // 9. Draw textboxes on the picture
                BufferedImage bufferedImage = ImageHandler.loadImage(Paths.get(path_to_images,imageName).toString());
                if (save_output_images) {

                    // Draw all boxes
                    drawDetectedObjects(bufferedImage,
                                        detectionEvaluationResult.getFn(),
                                        DRAWMODE.fn);

                    drawDetectedObjects(bufferedImage,
                                        detectionEvaluationResult.getFp(),
                                        DRAWMODE.fp);

                    drawDetectedObjects(bufferedImage,
                                        detectionEvaluationResult.getTp(),
                                        DRAWMODE.tp);

                    drawDetectedObjects(bufferedImage,
                                        detectionEvaluationResult.getGtDetected(),
                                        DRAWMODE.gtDetected);


                    // Draw a color legend
                    drawColorLegend(bufferedImage);

                    // Save image
                    String name = imageName.split("\\.")[0];  // e.g.: img_1
                    String type = imageName.split("\\.")[1];  // e.g.: jpg

                    ImageHandler.saveImage(bufferedImage, path_to_output, name + "_detection_result", type);
                }

            }
            csvWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void drawBoxes(BufferedImage img, List<Textbox> textboxes, DRAWMODE mode) {

        Color color = DRAWMODE.getColor(mode);

        for (Textbox textbox : textboxes) {
            ImageHandler.drawRectangle(img,
                    textbox.xMin,
                    textbox.xMax,
                    textbox.yMin,
                    textbox.yMax,
                    color);
        }
    }

    private void drawDetectedObjects(BufferedImage img, Set<DetectedObjects.DetectedObject> detectedObjects, DRAWMODE mode) {
        Color color = DRAWMODE.getColor(mode);

        for (DetectedObjects.DetectedObject detectedObject : detectedObjects) {
            Textbox textbox = Textbox.fromDetectedObject_extended(detectedObject, img.getWidth(), img.getHeight());
            ImageHandler.drawRectangle(img,
                    textbox.xMin,
                    textbox.xMax,
                    textbox.yMin,
                    textbox.yMax,
                    color);
        }
    }

    private void drawColorLegend(BufferedImage img) {
        int linespace = 2;
        int textheight = 10; // THIS DOES NOT CHANGE THE TEXT SIZE


        List<DRAWMODE> order = new ArrayList<DRAWMODE>(
                Arrays.asList(
                        DRAWMODE.tp,
                        DRAWMODE.fp,
                        DRAWMODE.gtDetected,
                        DRAWMODE.fn));
        Collections.reverse(order);

        int y = img.getHeight() - 10;
        for (DRAWMODE mode : order) {
            ImageHandler.drawText(img, DRAWMODE.getDescription(mode), 10, y, DRAWMODE.getColor(mode));
            y -= (textheight + linespace);
        }
    }

    private RecognitionEvaluationResult evaluateRecognitions(List<String> recognizedText, DetectedObjects groundTruthDetectedObjects) {
        if (recognizedText.size() != groundTruthDetectedObjects.getNumberOfObjects()) {
            LOGGER.error("Recognized Text and ground truth with text should have equal length. " +
                    "RecognizedText has size " + recognizedText.size() + " and groundTruth has size " +
                    groundTruthDetectedObjects.getNumberOfObjects());
        }
        int size = recognizedText.size();

        if (size == 0) {
            return new RecognitionEvaluationResult(1.0, 1.0);
        }

        double avgIOU = 0;
        double avgJaccardTrigram = 0;

        for (int i = 0; i < size; i++) {
            String recognized = recognizedText.get(i);
            String groundTruth = groundTruthDetectedObjects.item(i).getClassName();

            avgIOU += Distances.iou(recognized, groundTruth);
            avgJaccardTrigram += Distances.jaccardTrigramDistance(recognized, groundTruth);
        }

        avgIOU /= size;
        avgJaccardTrigram /= size;

        return new RecognitionEvaluationResult(avgIOU, avgJaccardTrigram);
    }

    /*
    private HashMap<String, DetectedObjects> getGroundTruthDetectedObjects(String path_to_ground_truth) {
        // TODO: implement me
        HashMap<String, DetectedObjects> hashMap = new HashMap<>();
        File gt_folder = new File(path_to_ground_truth);
        String[] files = gt_folder.list((dir, name) -> name.endsWith(".txt"));
        List<String> textList = new ArrayList<>();
        List<Double> probabilityList = new ArrayList<>();
        List<BoundingBox> boundingBoxList = new ArrayList<>();
        for (String fileName : files) {
            List<Textbox> list = new ArrayList<>();
            File file = new File(Paths.get(path_to_ground_truth,fileName).toString());
            String imgName = (fileName.split("_", 2)[1]).split("\\.", 2)[0];

            final Scanner s;

            textList.clear();
            probabilityList.clear();
            boundingBoxList.clear();

            try {
                s = new Scanner(file);
                while(s.hasNextLine()) {
                    final String line = s.nextLine();
                    rwedhjbkjrkdgrfd
                    list.add(Textbox.fromIncidentalSceneTextTXT(line));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            hashMap.put(imgName, new DetectedObjects(textList, probabilityList, boundingBoxList));
        }
        return hashMap;
    }*/

    private HashMap<String, List<Textbox>> getGroundTruthTextboxes(String path_to_ground_truth) {
        // pic:     img_1.jpg
        // gt :     gt_img_1.txt
        // key:     img_1
        // imgName: img_1
        HashMap<String, List<Textbox>> hashmap = new HashMap<>();
        File gt_folder = new File(path_to_ground_truth);
        String[] files = gt_folder.list((dir, name) -> name.endsWith(".txt"));
        for (String fileName : files) {
            List<Textbox> list = new ArrayList<>();
            File file = new File(Paths.get(path_to_ground_truth,fileName).toString());
            String imgName = (fileName.split("_", 2)[1]).split("\\.", 2)[0];

            final Scanner s;

            try {
                s = new Scanner(file);
                while(s.hasNextLine()) {
                    final String line = s.nextLine();

                    list.add(Textbox.fromIncidentalSceneTextTXT(line));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            hashmap.put(imgName, list);
        }
        return hashmap;
    }

    private DetectionEvaluationResult evaluateDetections(Image img, DetectedObjects detectedObjects, DetectedObjects groundTruthDetectedObjects) {
        List<DetectedObjects.DetectedObject> detectedObjectsList = Converters.detectedObjects2listOfDetectedObject(detectedObjects);
        List<DetectedObjects.DetectedObject> groundTruthDetectedObjectsList = Converters.detectedObjects2listOfDetectedObject(groundTruthDetectedObjects);

        Set<DetectedObjects.DetectedObject> fp = new HashSet<>(detectedObjectsList);
        Set<DetectedObjects.DetectedObject> fn = new HashSet<>(groundTruthDetectedObjectsList);

        double avgIOU = 0;

        HashMap<DetectedObjects.DetectedObject, DetectedObjects.DetectedObject> mostFittingGroundTruths = new HashMap<>();
        for (DetectedObjects.DetectedObject groundTruthDetectedObject : groundTruthDetectedObjectsList) {
            Textbox gtBox = Textbox.fromDetectedObject_extended(groundTruthDetectedObject, img.getWidth(), img.getHeight());

            double maxIoU = 0;
            DetectedObjects.DetectedObject fittingObject = null;

            for (DetectedObjects.DetectedObject detectedObject : detectedObjectsList) {
                Textbox detectedTextbox = Textbox.fromDetectedObject_extended(detectedObject, img.getWidth(), img.getHeight());

                double iou = gtBox.getIoU(detectedTextbox);
                if (iou > maxIoU) {
                    maxIoU = iou;
                    fittingObject = detectedObject;
                }
            }

            if (fittingObject != null) {
                mostFittingGroundTruths.put(fittingObject, groundTruthDetectedObject);
                fn.remove(groundTruthDetectedObject);
                fp.remove(fittingObject);
            }


            avgIOU += maxIoU;

        }

        if (mostFittingGroundTruths.values().isEmpty())
            avgIOU = 0;
        else
            avgIOU /= mostFittingGroundTruths.values().size();

        Set<DetectedObjects.DetectedObject> tp = new HashSet<>(mostFittingGroundTruths.keySet());


        return new DetectionEvaluationResult(avgIOU, tp, fn, fp, mostFittingGroundTruths);
    }

    private String format_csv_line(String image_name,
                                   int x_res,
                                   int y_res,
                                   long ms_det,
                                   long ms_rec,
                                   long ms_tot,
                                   int tp,
                                   int fn,
                                   int fp,
                                   double iou_avg,
                                   double jaccard_distance_recognition,
                                   double jaccard_trigram_distance_recognition)
            throws IOException {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(image_name);
        stringBuilder.append(",");

        stringBuilder.append(Integer.toString(x_res));
        stringBuilder.append(",");

        stringBuilder.append(Integer.toString(y_res));
        stringBuilder.append(",");

        stringBuilder.append(Long.toString(ms_det));
        stringBuilder.append(",");

        stringBuilder.append(Long.toString(ms_rec));
        stringBuilder.append(",");

        stringBuilder.append(Long.toString(ms_tot));
        stringBuilder.append(",");

        stringBuilder.append(Integer.toString(tp));
        stringBuilder.append(",");

        stringBuilder.append(Integer.toString(fn));
        stringBuilder.append(",");

        stringBuilder.append(Integer.toString(fp));
        stringBuilder.append(",");

        stringBuilder.append(Double.toString(iou_avg));
        stringBuilder.append(",");

        stringBuilder.append(Double.toString(jaccard_distance_recognition));
        stringBuilder.append(",");

        stringBuilder.append(Double.toString(jaccard_trigram_distance_recognition));


        stringBuilder.append(System.lineSeparator());

        return stringBuilder.toString();
    }

    public static void main(String[] args) {
        Evaluator evaluator = new Evaluator();
        evaluator.evaluateOnIncidentalSceneText();


    }

}


