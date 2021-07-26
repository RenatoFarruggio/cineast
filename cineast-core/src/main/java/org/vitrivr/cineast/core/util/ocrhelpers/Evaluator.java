package org.vitrivr.cineast.core.util.ocrhelpers;

// TODO: remove all camel cases
// TODO: add rotation model
// FIXME: The ground truth text boxes are sometimes not shown
// TODO: write gradle command for evaluating

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

public class Evaluator {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Creates a csv file containing:
     *
     *  image_name,     The name of the image
     *  x_res,          The width of the image
     *  y_res,          The height of the image
     *  ms_det,         The time it took to do text detection
     *  ms_rec,         The time it took to recognize the text in all textboxes
     *  ms_tot,         The time it took to do both text detection and text recognition
     *  tp,             True positives: The set of <code>DetectedObject</code>s that were detected correctly
     *  fn,             False negatives: The set of undetected ground truth <code>DetectedObject</code>s
     *  fp,             False positives: The set of detected <code>DetectedObject</code>s where there is no corresponding ground truth box
     *  gtDetected,     The set of detected ground truth <code>DetectedObject</code>s
     *  iou_avg,        Average iou of the detected <code>DetectedObject</code>s:
     *                      The sum of all iou's divided by number considered ground truths
     *  jaccard_distance_recognition_avg,
     *                  The average intersection over union of single letters in the recognition step
     *  jaccard_distance_recognition_variance,
     *                  The variance of the intersection over union of single letters in the recognition step
     *  jaccard_trigram_distance_recognition_avg,
     *                  The sum of jaccard trigram distances between words found and words in the ground truth
     *  jaccard_trigram_distance_recognition_variance,
     *                  The variance of the jaccard trigram distances between words found and words in the ground truth
     *  numberOfConsideredGroundTruths,
     *                  The number of ground truth <code>DetectedObjects</code> that have text assigned to
     */
    public void evaluateOnIncidentalSceneText(int runNumber) {
        // These are user inputs. TODO: Implement argument builder
        final String file_name_results = "resultsIncidentalSceneText_45_" + runNumber + ".csv";
        final String path_to_images = "C:\\Users\\Renato\\Downloads\\IncidentalSceneText\\test\\ch4_test_images";
        final String path_to_ground_truth = "C:\\Users\\Renato\\Downloads\\IncidentalSceneText\\test\\Challenge4_Test_Task1_GT";
        final String path_to_output = "output/";

        final boolean save_output_images = false;
        final double det_threshhold = 0.45;

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
            LOGGER.error("Please make sure that the ocr models are downloaded. Run './gradlew getExternalFiles' to download them.");
        }
        System.out.println("The following error messages aren't actually errors.");
        // I just don't know how to get rid of them.
        InferenceModel inferenceModel = new InferenceModel(detectionModel, recognitionModel);
        System.out.println("You can ignore the above error message.");

        System.out.println("Starting run " + runNumber + " ...");

        try (Writer w = new FileWriter(file_name_results)) {
            //try (Reader r = new FileReader(ground_truth))
            BufferedWriter csvWriter = new BufferedWriter(w);
            csvWriter.append("image_name,x_res,y_res,ms_det,ms_rec,ms_tot,tp,fn,fp,gtDetected,iou_detection_avg,iou_recognition_avg,iou_recognition_variance,jaccard_trigram_distance_recognition_avg,jaccard_trigram_distance_recognition_variance,number_of_considered_ground_truths");
            csvWriter.append(System.lineSeparator());

            // Define variables
            long start_det, end_det;  // detection
            long start_rec, end_rec;  // recognition
            long ms_det, ms_rec, ms_tot;  // milliseconds

            DetectionEvaluationResult detectionEvaluationResult;
            RecognitionEvaluationResult recognitionEvaluationResult;

            Image img;

            int counter = 1;
            int numberOfImages = imageNames.length;

            //HashMap<String, List<Textbox>> groundTruthTextboxes = getGroundTruthTextboxes(path_to_ground_truth);
            DetectedObjects detectedBoxes;

            // PIPELINE //
            for (String imageName : imageNames) {
                System.out.println("Evaluating model on image " + counter++ + "/" + numberOfImages + ": " + imageName + " ...");

                Path imagePath = Paths.get(path_to_images,imageName);
                Path groundTruthFilePath = Paths.get(path_to_ground_truth, "gt_" + imageName.split("\\.")[0] + ".txt");

                // 1. Load image
                img = inferenceModel.loadImage(imagePath);

                // 2. Load ground truth boxes
                DetectedObjects groundTruthDetectedObjects =
                        IncidentalSceneTextLoader.GroundTruthDetectedObjectsFromTxtFile(
                                groundTruthFilePath,
                                img);
                DetectedObjects groundTruthWithTextDetectedObjects = Converters.getGroundTruthWithTextFromGroundTruth(groundTruthDetectedObjects);

                // 3. Text detection
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
                //System.out.println("Evaluating detections of img: " + imageName + " ...");
                String key = imageName.split("\\.", 2)[0];
                detectionEvaluationResult = evaluateDetections(img, detectedBoxes, groundTruthDetectedObjects, det_threshhold);

                // 6. Evaluate recognitions
                recognitionEvaluationResult = evaluateRecognitions(img, recognizedText, groundTruthWithTextDetectedObjects);

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
                        detectionEvaluationResult.getGtDetected().size(),
                        detectionEvaluationResult.avgIOU,
                        recognitionEvaluationResult.iouMean,
                        recognitionEvaluationResult.iouVariance,
                        recognitionEvaluationResult.trigramMean,
                        recognitionEvaluationResult.trigramVariance,
                        recognitionEvaluationResult.numberOfConsideredGroundTruths);
                csvWriter.write(line);
                //System.out.println(line);

                // 9. Draw textboxes of detections on the picture
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

                // 10. Draw textboxes of recognitions on the picture
                bufferedImage = ImageHandler.loadImage(Paths.get(path_to_images,imageName).toString());
                if (save_output_images) {

                    // Draw the boxes
                    for (TextboxWithText textboxWithText : recognitionEvaluationResult.textboxesWithText) {
                        textboxWithText.drawOnImage(bufferedImage);
                    }

                    // Save image
                    String name = imageName.split("\\.")[0];  // e.g.: img_1
                    String type = imageName.split("\\.")[1];  // e.g.: jpg

                    ImageHandler.saveImage(bufferedImage, path_to_output, name + "_recognition_result", type);
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
        int textheight = 20; // THIS DOES NOT CHANGE THE TEXT SIZE


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

    private RecognitionEvaluationResult evaluateRecognitions(Image img, List<String> recognizedText, DetectedObjects groundTruthDetectedObjects) {
        if (recognizedText.size() != groundTruthDetectedObjects.getNumberOfObjects()) {
            LOGGER.error("Recognized Text and ground truth with text should have equal length. " +
                    "RecognizedText has size " + recognizedText.size() + " and groundTruth has size " +
                    groundTruthDetectedObjects.getNumberOfObjects());
        }
        int size = recognizedText.size();
        List<TextboxWithText> textboxesWithText = new ArrayList<>();

        if (size == 0) {
            return new RecognitionEvaluationResult(1.0, 1.0,1.0, 1.0, 0, textboxesWithText);
        }

        List<Double> ious = new ArrayList<>();
        List<Double> jaccardTrigramSimilarities = new ArrayList<>();

        //double avgIOU = 0;
        //double avgJaccardTrigram = 0;

        for (int i = 0; i < size; i++) {
            String recognized = recognizedText.get(i);
            DetectedObjects.DetectedObject detectedObjectGroundTruth = groundTruthDetectedObjects.item(i);
            String groundTruth = detectedObjectGroundTruth.getClassName();

            ious.add(Distances.iou(recognized, groundTruth));
            jaccardTrigramSimilarities.add(Distances.jaccardTrigramDistance(recognized, groundTruth));

            Textbox textbox = Textbox.fromDetectedObject_extended(detectedObjectGroundTruth, img.getWidth(), img.getHeight());
            TextboxWithText textboxWithText = new TextboxWithText(textbox,groundTruth,recognized);
            textboxesWithText.add(textboxWithText);
        }

        double iouMean = getMean(ious);
        double iouVariance = getVariance(ious, iouMean);

        double trigramMean = getMean(jaccardTrigramSimilarities);
        double trigramVariance = getVariance(jaccardTrigramSimilarities, trigramMean);


        return new RecognitionEvaluationResult(iouMean, iouVariance, trigramMean, trigramVariance, size, textboxesWithText);
    }

    private double getMean(List<Double> data) {
        double mean = 0.0;
        for (double d : data) {
            mean += d;
        }
        mean /= data.size();
        return mean;
    }

    private double getVariance(List<Double> data, double mean) {
        double variance = 0;
        for (double d : data) {
            variance += Math.pow(d-mean, 2);
        }
        variance /= data.size();
        return variance;
    }

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

    private DetectionEvaluationResult evaluateDetections(Image img, DetectedObjects detectedObjects, DetectedObjects groundTruthDetectedObjects, double det_threshhold) {
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

            if (maxIoU < det_threshhold) {
                fittingObject = null;
                maxIoU = 0;
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
                                   int gtDetected,
                                   double iou_detection_avg,
                                   double iou_recognition_avg,
                                   double iou_recognition_variance,
                                   double jaccard_trigram_distance_recognition_avg,
                                   double jaccard_trigram_distance_recognition_variance,
                                   int number_of_considered_ground_truths)
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

        stringBuilder.append(Integer.toString(gtDetected));
        stringBuilder.append(",");

        stringBuilder.append(String.format("%.2f", iou_detection_avg));
        stringBuilder.append(",");

        stringBuilder.append(String.format("%.2f", iou_recognition_avg));
        stringBuilder.append(",");

        stringBuilder.append(String.format("%.5f", iou_recognition_variance));
        stringBuilder.append(",");

        stringBuilder.append(String.format("%.2f", jaccard_trigram_distance_recognition_avg));
        stringBuilder.append(",");

        stringBuilder.append(String.format("%.5f", jaccard_trigram_distance_recognition_variance));
        stringBuilder.append(",");

        stringBuilder.append(Integer.toString(number_of_considered_ground_truths));

        stringBuilder.append(System.lineSeparator());

        return stringBuilder.toString();
    }

    public static void main(String[] args) {
        int runs = 2;
        for (int i = 1; i <= runs; i++) {
            Evaluator evaluator = new Evaluator();
            evaluator.evaluateOnIncidentalSceneText(i);
        }

    }

}


