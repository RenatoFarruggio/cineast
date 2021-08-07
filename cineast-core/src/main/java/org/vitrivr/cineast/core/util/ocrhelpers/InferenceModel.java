package org.vitrivr.cineast.core.util.ocrhelpers;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.paddlepaddle.zoo.cv.imageclassification.PpWordRotateTranslator;
import ai.djl.paddlepaddle.zoo.cv.objectdetection.PpWordDetectionTranslator;
import ai.djl.paddlepaddle.zoo.cv.wordrecognition.PpWordRecognitionTranslator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InferenceModel implements AutoCloseable {

    //  This class is used for the demo, but has no use in Cineast.
    public class InferenceResult {
        BufferedImage img;
        String imgName;
        int detectionTime;
        int recognitionTime;
        int totalTime;
        List<String> recognizedTextList;

        InferenceResult(BufferedImage img, String imgName, int detectionTime, int recognitionTime, int totalTime, List<String> recognizedTextList) {
            this.img = img;
            this.imgName = imgName;
            this.detectionTime = detectionTime;
            this.recognitionTime = recognitionTime;
            this.totalTime = totalTime;
            this.recognizedTextList = recognizedTextList;
        }
    }

    private final Predictor<Image, DetectedObjects> detector;
    private final Predictor<Image, String> recognizer;

    /**
     * Constructor to create a model based on locally available files. Possible file types according to:
     * https://djl.ai/docs/load_model.html#current-supported-archive-formats
     *
     * @param detectionModelZip The trained model for text detection, e.g. zip file
     * @param recognitionModelZip The trained model for text recognition, e.g. zip file
     */
    public InferenceModel(File detectionModelZip, File recognitionModelZip) {
        // Load text detection model
        Criteria<Image, DetectedObjects> criteriaDetection = Criteria.builder()
                .optEngine("PaddlePaddle")
                .setTypes(Image.class, DetectedObjects.class)
                .optModelUrls(String.valueOf(detectionModelZip.toURI()))
                .optTranslator(new PpWordDetectionTranslator(new ConcurrentHashMap<String, String>()))
                .build();
        ZooModel<Image, DetectedObjects> detectionModel = null;
        try {
            detectionModel = ModelZoo.loadModel(criteriaDetection);
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            e.printStackTrace();
        }
        assert detectionModel != null;
        this.detector = detectionModel.newPredictor();

        // Load model for text recognition
        Criteria<Image, String> criteriaRecognition = Criteria.builder()
                .optEngine("PaddlePaddle")
                .setTypes(Image.class, String.class)
                .optModelUrls(String.valueOf(recognitionModelZip.toURI()))
                .optTranslator(new PpWordRecognitionTranslator())
                .build();
        ZooModel<Image, String> recognitionModel = null;
        try {
            recognitionModel = ModelZoo.loadModel(criteriaRecognition);
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            e.printStackTrace();
        }
        assert recognitionModel != null;
        this.recognizer = recognitionModel.newPredictor();
    }

    /**
     * Default constructor downloads a model for detection and a model for recognition automatically
     */
    public InferenceModel() {

        // Load text detection model
        Criteria<Image, DetectedObjects> criteria1 = Criteria.builder()
                .optEngine("PaddlePaddle")
                .setTypes(Image.class, DetectedObjects.class)
                .optModelUrls("https://resources.djl.ai/test-models/paddleOCR/mobile/det_db.zip")
                .optTranslator(new PpWordDetectionTranslator(new ConcurrentHashMap<String, String>()))
                .build();
        ZooModel<Image, DetectedObjects> detectionModel = null;
        try {
            detectionModel = ModelZoo.loadModel(criteria1);
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            e.printStackTrace();
        }
        assert detectionModel != null;
        this.detector = detectionModel.newPredictor();

        // Load model for text recognition
        Criteria<Image, String> criteria3 = Criteria.builder()
                .optEngine("PaddlePaddle")
                .setTypes(Image.class, String.class)
                .optModelUrls("https://resources.djl.ai/test-models/paddleOCR/mobile/rec_crnn.zip")
                .optTranslator(new PpWordRecognitionTranslator())
                .build();
        ZooModel<Image, String> recognitionModel = null;
        try {
            recognitionModel = ModelZoo.loadModel(criteria3);
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            e.printStackTrace();
        }
        assert recognitionModel != null;
        this.recognizer = recognitionModel.newPredictor();
    }

    public InferenceResult inference_on_example_image(Path imagePath) throws IOException, TranslateException {
        // Load image
        //String imagesFolder = "example_images/";
        //String imageName = "img_11.jpg";
        //String imageName = "img_5.png";
        Image img = ImageFactory.getInstance().fromFile(imagePath);
        img.getWrappedImage();

        img.save(new FileOutputStream("output_0_original.png"), "png");

        // Text detection
        long startDetection = System.currentTimeMillis();
        DetectedObjects detectedObj = detector.predict(img);
        long endDetection = System.currentTimeMillis();
        Image newImage = img.duplicate(Image.Type.TYPE_INT_ARGB);
        newImage.drawBoundingBoxes(detectedObj);
        newImage.getWrappedImage();

        newImage.save(new FileOutputStream("output_1_textboxes.png"), "png");

        // Cut out single textboxes
        List<DetectedObjects.DetectedObject> boxes = detectedObj.items();

        // Evaluate the whole picture
        List<String> names = new ArrayList<>();
        List<Double> prob = new ArrayList<>();
        List<BoundingBox> rect = new ArrayList<>();

        long startRecognition = System.currentTimeMillis();
        List<String> recognizedTextList = new ArrayList<>();
        for (DetectedObjects.DetectedObject box : boxes) {
            Image subImg = getSubImage(img, box.getBoundingBox());
            String name = recognizer.predict(subImg);
            names.add(name);
            prob.add(-1.0);
            rect.add(box.getBoundingBox());

            recognizedTextList.add(name);
            printWordAndBoundingBox(name, box.getBoundingBox());
        }
        long endRecognition = System.currentTimeMillis();
        newImage.drawBoundingBoxes(new DetectedObjects(names, prob, rect));
        newImage.getWrappedImage();

        newImage.save(new FileOutputStream("output_2_evaluated.png"), "png");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        newImage.save(out, "png");

        // Output
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
        String imgName = imagePath.getFileName().toString();
        int detectionTime = (int)(endDetection - startDetection);
        int recognitionTime = (int)(endRecognition - startRecognition);
        int totalTime = (int)(endRecognition - startDetection);
        return new InferenceResult(bufferedImage, imgName, detectionTime, recognitionTime, totalTime, recognizedTextList);
    }

    public Image loadImage(Path imagePath) {
        try {
            return ImageFactory.getInstance().fromFile(imagePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DetectedObjects detection(Image img) {
        try {
            return this.detector.predict(img);
        } catch (TranslateException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Do recognition on an already cropped image
     *
     * @param img The already cropped image to do recognition on
     * @return The text that has been recognized
     */
    public String recognition(Image img) {
        String name = null;
        try {
            name = recognizer.predict(img);
        } catch (TranslateException e) {
            e.printStackTrace();
        }
        return name;
    }

    /**
     * Do recognition on an image based on bounding boxes
     *
     * @param img The image to do recognition on
     * @param detectedObjects The bounding boxes that the image will be cropped to before recognition
     * @return The list of texts that have been recognized
     */
    public List<String> recognition(Image img, DetectedObjects detectedObjects) {

        List<DetectedObjects.DetectedObject> boxes = detectedObjects.items();

        List<String> names = new ArrayList<>();

        for (DetectedObjects.DetectedObject box : boxes) {
            Image subImg = getSubImage(img, box.getBoundingBox());


            String name = null;
            try {
                name = recognizer.predict(subImg);
            } catch (TranslateException e) {
                e.printStackTrace();
            }
            names.add(name);
        }

        return names;
    }

    Image getSubImage(Image img, BoundingBox box) {
        Rectangle rect = box.getBounds();
        Rectangle extendedRect = Converters.extendRect(rect);

        int width = img.getWidth();
        int height = img.getHeight();

        Rectangle extendedRectangleAbsolute = Converters.rectRelative2Absolute(extendedRect, width, height);

        return img.getSubimage(
                (int) extendedRectangleAbsolute.getX(),
                (int) extendedRectangleAbsolute.getY(),
                (int) extendedRectangleAbsolute.getWidth(),
                (int) extendedRectangleAbsolute.getHeight());
    }

    void printWordAndBoundingBox(String word, BoundingBox box) {
        System.out.println("{word:" + word + ",\tbox:" + box + "}");
    }

    @Override
    public void close() {
        this.detector.close();
        this.recognizer.close();
    }
}

