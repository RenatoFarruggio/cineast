package org.vitrivr.cineast.core.util.ocrhelpers;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TextboxWithText extends Textbox {

    private Color textboxColor = Color.BLACK;
    private Color textColorGT = Color.GREEN;
    private Color textColorFound = Color.RED;

    private Textbox textbox;
    private String textActual;
    private String textFound;

    public TextboxWithText(Textbox textbox, String text) {
        new TextboxWithText(textbox, "", text);
    }

    public TextboxWithText(Textbox textbox, String textActual, String textFound) {
        this.textbox = textbox;
        this.textActual = textActual;
        this.textFound = textFound;
    }

    @Override
    public String toString() {
        return "Actual: " + textActual + ", Found: " + textFound + " {" +
                "xMin=" + xMin +
                ", xMax=" + xMax +
                ", yMin=" + yMin +
                ", yMax=" + yMax +
                '}';
    }

    public BufferedImage drawOnImage(BufferedImage img) {
        ImageHandler.drawRectangle(
                img,
                this.textbox.xMin,
                this.textbox.xMax,
                this.textbox.yMin,
                this.textbox.yMax,
                textboxColor);

        ImageHandler.drawText(
                img,
                this.textActual,
                this.textbox.xMin,
                this.textbox.yMin,
                textColorGT);

        ImageHandler.drawText(
                img,
                this.textFound,
                this.textbox.xMin,
                this.textbox.yMin+15,
                textColorFound);

        return img;
    }

}
