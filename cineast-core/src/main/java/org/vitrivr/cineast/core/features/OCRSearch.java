package org.vitrivr.cineast.core.features;

import ai.djl.modality.cv.BufferedImageFactory;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.DetectedObjects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vitrivr.cineast.core.data.segments.SegmentContainer;
import org.vitrivr.cineast.core.features.abstracts.AbstractTextRetriever;
import org.vitrivr.cineast.core.util.ocrhelpers.InferenceModel;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 *  OCR is handled by adding fuzziness / levenshtein-distance support to the query if there are no quotes present (as quotes indicate precision)
 *  This makes sense here since we expect small errors from OCR sources
 */
public class OCRSearch extends AbstractTextRetriever {

  private static final Logger LOGGER = LogManager.getLogger();

  public static final String OCR_TABLE_NAME = "features_ocr";
  public static final InferenceModel inferenceModel = new InferenceModel();

  /**
   * Default constructor for {@link OCRSearch}.
   */
  public OCRSearch() {
    super(OCR_TABLE_NAME);
  }

  @Override
  protected String enrichQueryTerm(String queryTerm) {
    if (queryTerm.contains("\"")) {
      return queryTerm;
    }
    return queryTerm + "~1";
  }

  @Override
  public void processSegment(SegmentContainer shot) {
    // extract -e "C:\\Users\\Renato\\Desktop\\cineast\\extraction_config.json"

    LOGGER.debug("Processing segment: " + shot.getId());

    // 1. Load image
    BufferedImage bufferedImage = shot.getMostRepresentativeFrame().getImage().getBufferedImage();
    Image img = BufferedImageFactory.getInstance().fromImage(bufferedImage);

    // 2. Detection
    DetectedObjects detectedBoxes = inferenceModel.detection(img);

    // 3. Return if nothing is found
    if (detectedBoxes.getNumberOfObjects() == 0) {
      LOGGER.debug("No text found.");
      return;
    }

    // 4. Recognition
    List<String> recognizedText = inferenceModel.recognition(img, detectedBoxes);

    // 5. Persist
    for (String word : recognizedText) {
      if (word.equals("")) {
        return;
      }
      super.persist(shot.getId(), word);
      LOGGER.debug("Found and saved word: " + word);
    }

  }
}
