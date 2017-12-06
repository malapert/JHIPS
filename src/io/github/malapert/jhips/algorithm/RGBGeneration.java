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
package io.github.malapert.jhips.algorithm;

import io.github.malapert.jhips.provider.JHipsMetadataProviderInterface;
import io.github.malapert.jhips.util.Utils;
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
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class RGBGeneration {

    /**
     * Directory where the R tiles are stored. 
     */
    public static String R_DIRECTORY = "r.fitsHiPS";

    /**
     * Directory where the G tiles are stored.
     */
    public static String G_DIRECTORY = "g.fitsHiPS";

    /**
     * Directory where the B tiles are stored.
     */
    public static String B_DIRECTORY = "b.fitsHiPS";

    /**
     * Directory where the color tiles are stored.
     */
    public static String COLOR_DIRECTORY = "color";

    /**
     * Creates RGB tiles based on a set tiles on R, G, B color.
     * <p>
     * The RGB generation is created in outputDirectory/COLOR_DIRECTORY based on
     * <ul>
     * <li>R tiles in outputDirectory/R_DIRECTORY
     * <li>G tiles in outputDirectory/G_DIRECTORY
     * <li>B tiles in outputDirectory/B_DIRECTORY
     * </ul>
     * 
     * @param outputDirectory output directory where RGB tiles are created
     * @param metadata
     * @throws IOException 
     */
    public static void create(File outputDirectory, JHipsMetadataProviderInterface metadata) throws IOException {
        Path rDirectory = Paths.get(outputDirectory.getAbsolutePath() + File.separator + R_DIRECTORY);
        Path originPropetyFile = Paths.get(outputDirectory.getAbsolutePath() + File.separator + R_DIRECTORY + File.separator+ "properties");
        int order = getOrder(originPropetyFile.toFile());
        long nbPixels = (long) (Math.pow(2, order) * Math.pow(2, order) * 12);
        FileVisitor<Path> fileProcessor = new ProcessFile(nbPixels, outputDirectory);
        Files.walkFileTree(rDirectory, fileProcessor);
        Path destPropetyFile = Paths.get(outputDirectory.getAbsolutePath() + File.separator + COLOR_DIRECTORY + File.separator + "properties");
        createMetadata(metadata, originPropetyFile.toFile(), destPropetyFile.toFile());

    }
    
    /**
     * Returns order from a property file by reading hips_order keyword.
     * @param properties property file
     * @return the order
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private static int getOrder(File properties) throws FileNotFoundException, IOException {
        Properties propertyFile = new Properties();
        propertyFile.load(new FileInputStream(properties));
        String order = propertyFile.getProperty("hips_order");
        return Integer.parseInt(order);
    }
    
    /**
     * Creates metadata file for RGB tiles based on a property template from an
     * existing tile.
     * @param src source property
     * @param dest destination property
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private static void createMetadata(JHipsMetadataProviderInterface metadata, File src, File dest) throws FileNotFoundException, IOException {  
        Properties properties = new Properties();
        properties.load(new FileInputStream(src));
        properties.setProperty("hips_tile_format", "png");
        properties.setProperty("format", "png");
        if(metadata.getBib_reference() != null) {
            properties.setProperty("bib_reference", metadata.getBib_reference());            
        }
        if(metadata.getBib_reference_url() != null) {
            properties.setProperty("bib_reference_url", metadata.getBib_reference_url());                        
        }        
        if(metadata.getCreator_did() != null) {
            properties.setProperty("creator_did", metadata.getCreator_did());                        
        }        
        if(metadata.getData_ucd() != null) {
            properties.setProperty("data_ucd", metadata.getData_ucd());                                    
        }        
        if(metadata.getDataproduct_subtype() != null) {
            properties.setProperty("dataproduct_subtype", metadata.getDataproduct_subtype());                                                
        }        
        if(metadata.getDataproduct_type() != null) {
            properties.setProperty("dataproduct_type", metadata.getDataproduct_type());                                                
        }               
        if(metadata.getHips_copyright() != null) {
            properties.setProperty("hips_copyright", metadata.getHips_copyright());                                                
        }        
        if(metadata.getHips_creator() != null) {
            properties.setProperty("hips_creator", metadata.getHips_creator());                                                
        }        
        if(metadata.getHips_frame() != null) {
            properties.setProperty("hips_frame", metadata.getHips_frame());                                                
        }        
        if(metadata.getHips_pixel_cut() != null) {
            properties.setProperty("hips_pixel_cut", metadata.getHips_pixel_cut());                                                
        }        
        if(metadata.getHips_progenitor_url() != null) {
            properties.setProperty("hips_progenitor_url", metadata.getHips_progenitor_url());                                                
        } 
        if(metadata.getHips_release_date() != null) {
            properties.setProperty("hips_creation_date", metadata.getHips_creation_date());                                                
        }          
        if(metadata.getHips_release_date() != null) {
            properties.setProperty("hips_release_date", metadata.getHips_release_date());                                                
        }        
        if(metadata.getHips_status() != null) {
            properties.setProperty("hips_status", metadata.getHips_status());                                                
        }        
        if(metadata.getObs_ack() != null) {
            properties.setProperty("obs_ack", metadata.getObs_ack());                                                
        }        
        if(metadata.getObs_collection() != null) {
            properties.setProperty("obs_collection", metadata.getObs_collection());                                                
        }
        if(metadata.getObs_copyright() != null) {
            properties.setProperty("obs_copyright", metadata.getObs_copyright());                                                
        }        
        if(metadata.getObs_copyright_url() != null) {
            properties.setProperty("obs_copyright_url", metadata.getObs_copyright_url());                                                
        }        
        if(metadata.getObs_description() != null) {
            properties.setProperty("obs_description", metadata.getObs_description());                                                
        }        
        if(metadata.getObs_regime() != null) {
            properties.setProperty("obs_regime", metadata.getObs_regime());                                                
        }        
        if(metadata.getObs_title() != null) {
            properties.setProperty("obs_title", metadata.getObs_title());                                                
        }        
        if(metadata.getProv_progenitor() != null) {
            properties.setProperty("prov_progenitor", metadata.getProv_progenitor());                                                
        }        
        if(metadata.getPublisher_id() != null) {
            properties.setProperty("publisher_id", metadata.getPublisher_id());                                                
        }  
        if(metadata.getVersion() != null) {
            properties.setProperty("version", metadata.getVersion());                                                
        }  
        if(metadata.getHips_builder() != null) {
            properties.setProperty("hips_builder", metadata.getHips_builder());                                                
        }          
        properties.store(new FileOutputStream(dest), "");        
    }

    /**
     * Process the RGB tiles.
     */
    private static final class ProcessFile extends SimpleFileVisitor<Path> {

        private long counter = 0;
        private final long nbPixels;

        /**
         * Creates an instance of the process.
         * @param nbPixels number of pixels to create
         * @param outputDirectory output directory where the result is created
         */
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

        /**
         * Create RGB tile
         * @param aFile tile
         * @throws IOException 
         */
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

    /**
     * Main program
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        //RGBGeneration.create(new File("/tmp/data"));
    }

}
