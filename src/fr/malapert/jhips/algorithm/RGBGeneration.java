 /*******************************************************************************
 * Copyright 2015 - Jean-Christophe Malapert (jcmalapert@gmail.com)
 *
 * This file is part of JHIPS.
 *
 * JHIPS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JHIPS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JHIPS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.malapert.jhips.algorithm;

import fr.malapert.jhips.util.Utils;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.Properties;
import javax.imageio.ImageIO;

/**
 * Creates RGB tiles from a R,G,B tiles.
 * 
 * @author Jean-Christophe Malapert
 */
public class RGBGeneration {

    public static String R_DIRECTORY = "r.fitsHiPS";
    public static String G_DIRECTORY = "g.fitsHiPS";
    public static String B_DIRECTORY = "b.fitsHiPS";
    public static String COLOR_DIRECTORY = "color";

    public static void create(File outputDirectory) throws IOException {
        Path rDirectory = Paths.get(outputDirectory.getAbsolutePath() + File.separator + R_DIRECTORY);
        Path originPropetyFile = Paths.get(outputDirectory.getAbsolutePath() + File.separator + R_DIRECTORY + File.separator+ "properties");
        int order = getOrder(originPropetyFile.toFile());
        long nbPixels = (long) (Math.pow(2, order) * Math.pow(2, order) * 12);
        FileVisitor<Path> fileProcessor = new ProcessFile(nbPixels, outputDirectory);
        Files.walkFileTree(rDirectory, fileProcessor);
        Path destPropetyFile = Paths.get(outputDirectory.getAbsolutePath() + File.separator + COLOR_DIRECTORY + File.separator + "properties");
        copyMetadata(originPropetyFile.toFile(), destPropetyFile.toFile());

    }
    
    private static int getOrder(File properties) throws FileNotFoundException, IOException {
        Properties propertyFile = new Properties();
        propertyFile.load(new FileInputStream(properties));
        String order = propertyFile.getProperty("hips_order");
        return Integer.parseInt(order);
    }
    
    private static void copyMetadata(File src, File dest) throws FileNotFoundException, IOException {  
        Properties properties = new Properties();
        properties.load(new FileInputStream(src));
        properties.setProperty("hips_tile_format", "png");
        properties.setProperty("format", "png");
        properties.setProperty("hips_creation_date", Calendar.getInstance().getTime().toString());
        properties.store(new FileOutputStream(dest), "");        
    }

    private static final class ProcessFile extends SimpleFileVisitor<Path> {

        private long counter = 0;
        private final long nbPixels;

        public ProcessFile(long nbPixels, File outputDirectory) {
            this.nbPixels = nbPixels;
        }

        @Override
        public FileVisitResult visitFile(Path aFile, BasicFileAttributes aAttrs) throws IOException {
            if (aFile.toString().endsWith("png")) {
                this.counter++;
                Utils.monitoringFile(aFile.getFileName().toString(), counter, nbPixels);
                createRGB(aFile);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path aDir, BasicFileAttributes aAttrs) throws IOException {
            if (aDir.toString().contains(R_DIRECTORY)) {
                String directory = aDir.toString().replace(R_DIRECTORY, COLOR_DIRECTORY);
                new File(directory).mkdir();
            }
            System.out.println("Processing directory:" + aDir);
            return FileVisitResult.CONTINUE;
        }

        private void createRGB(Path aFile) throws IOException {
            // loads R filter
            BufferedImage imageR = ImageIO.read(aFile.toFile());

            // loads G filter
            String gFileName = aFile.toString().replace(R_DIRECTORY, G_DIRECTORY);
            File gFile = new File(gFileName);
            BufferedImage imageG = ImageIO.read(gFile);

            //loads B filter
            String bFileName = aFile.toString().replace(R_DIRECTORY, B_DIRECTORY);
            File bFile = new File(bFileName);
            BufferedImage imageB = ImageIO.read(bFile);

            //Creates RGB file
            BufferedImage imageColor = new BufferedImage(imageR.getWidth(), imageR.getHeight(), BufferedImage.TYPE_INT_RGB);

            //Fills RGB color
            for (int i = 0; i < imageR.getHeight(); i++) {
                for (int j = 0; j < imageR.getWidth(); j++) {
                    Color rPixel = new Color(imageR.getRGB(j, i));
                    Color gPixel = new Color(imageG.getRGB(j, i));
                    Color bPixel = new Color(imageB.getRGB(j, i));
                    Color colorPixel = new Color(rPixel.getRed(), gPixel.getGreen(), bPixel.getBlue());
                    imageColor.setRGB(j, i, colorPixel.getRGB());
                }
            }

            //Writes the output            
            String colorFileName = aFile.toString().replace("r.fitsHiPS", "color");
            ImageIO.write(imageColor, "png", new File(colorFileName));
        }

    }

    public static void main(String[] args) throws IOException {
        RGBGeneration.create(new File("/tmp/data"));
    }

}
