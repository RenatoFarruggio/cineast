package org.vitrivr.cineast.core.util.ocrhelpers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageHandler {
    public static BufferedImage loadImage(String filename) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(filename));
        } catch (IOException e) {
            System.out.println("Image " + filename + " not found!");
        }
        return img;
    }

    public static void saveImage(BufferedImage img, String pathname, String name, String filetype) {
        if (!pathname.endsWith("/") & !pathname.equals(""))
            pathname += "/";

        new File(pathname).mkdir();  // create output directory if it does not exist

        try {
            // retrieve image
            File outputfile = new File(pathname + name + "." + filetype);
            ImageIO.write(img, filetype, outputfile);
        } catch (IOException e) {
            System.out.println("Error while saving image: " + name + "\\." + filetype);
        }
    }

    public static void drawRectangle(BufferedImage img, int xmin, int xmax, int ymin, int ymax, Color color) {
        Graphics2D graph = img.createGraphics();
        graph.setColor(color);
        graph.setStroke(new BasicStroke(2));
        graph.drawRect(xmin, ymin, xmax-xmin, ymax-ymin);
        graph.dispose();
    }

    public static void drawText(BufferedImage img, String str, int startposX, int startposY, Color color) {
        Graphics2D graph = img.createGraphics();
        graph.setColor(color);
        graph.setFont(new Font("Courier New",Font.BOLD,20));
        graph.drawString(str, startposX, startposY);
        graph.dispose();
    }
}