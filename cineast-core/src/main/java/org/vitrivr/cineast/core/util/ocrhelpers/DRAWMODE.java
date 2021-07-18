package org.vitrivr.cineast.core.util.ocrhelpers;

import java.awt.Color;

public enum DRAWMODE {
    gt,
    tp,
    fn,
    fp,
    gtDetected;

    public static Color getColor(DRAWMODE mode) {
        Color color = Color.WHITE; // default, indicates that this mode has no color assigned
        switch (mode) {
            case gt:
                color = Color.MAGENTA;  break;

            case tp:
                color = Color.GREEN;   break;

            case fn:
                color = Color.darkGray;   break;

            case fp:
                color = Color.RED;   break;

            case gtDetected:
                color = Color.lightGray;   break;

        }
        return color;
    }

    public static String getDescription(DRAWMODE mode) {
        String descr = "undefined";
        switch (mode) {
            case gt:
                descr = "[gt] Detected ground truth";
                break;
            case tp:
                descr = "[tp] Correctly detected";
                break;
            case fn:
                descr = "[fn] Undetected ground truth";
                break;
            case fp:
                descr = "[fp] Incorrectly detected";
                break;
            case gtDetected:
                descr = "[gtDetected] Ground Truth Found";
                break;
        }
        return descr;
    }
}