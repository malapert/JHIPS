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
package io.github.malapert.jhips;

import io.github.malapert.jhips.metadata.MetadataFile;
import io.github.malapert.jhips.algorithm.Projection;
import io.github.malapert.jhips.exception.JHIPSException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Program to generate a panorama from Curiosity acquisitions.
 *
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class MarsHips extends JHIPS {

    /**
     * Horizontal coordinates [azimuth, elevation] expressed in radians.
     */
    private final double[] horizontalCoordinates = new double[]{Double.NaN, Double.NaN};

    /**
     * Camera's FOV [FOV along azimuth, FOV along elevation] in radians.
     */
    private final double[] fov = new double[]{Double.NaN, Double.NaN};

    /**
     * The real image size according to the FOV.
     * <p>
     * This size is different of the file size (width, height) of the graphic file
     * because the light can be impressed on only a part of the camera or more 
     * than the camera's detector.
     * <p>
     * When this parameter is not set in the JHIPS library, then the imageRequest
     * in the JHIPS camera is assumed to be the size of the graphic image (width, height)
     */
    private final int[] imageRequest = new int[]{0, 0};

    /**
     * Number of values to extract in each metadata file.
     */
    private final static int NUMBER_VALUE_TO_EXTRACT = 6;

    /**
     * Creates a HIPS map with a specific resolution.
     * @param order Healpix order
     */
    public MarsHips(int order) {
        super(order);
    }

    /**
     * Creates a HIPS map.
     */
    public MarsHips() {
        super();
    }    
    
    /**
     * Extracts values from a metadata file.
     * <p>
     * The parameter to extract are the following:
     * <ul>
     * <li>FIXED_INSTRUMENT_AZIMUTH : the azimuth value in degrees of the camera
     * <li>FIXED_INSTRUMENT_ELEVATION : the elevation value in degrees of the
     * camera
     * <li>LINES in IMAGE_REQUEST_PARMS group : height of the graphic image
     * <li>LINE_SAMPLES in IMAGE_REQUEST_PARMS group : width of the graphic
     * image
     * <li>HORIZONTAL_FOV : FOV in degrees along azimuth
     * <li>VERTICAL_FOV : FOV in degrees along elevation
     * </ul>
     *
     * @param filename the file where metadata must be extracted
     * @throws FileNotFoundException
     * @throws IOException
     * @throws io.github.malapert.jhips.exception.JHIPSException
     */
    public void readMetadata(String filename) throws FileNotFoundException, IOException, JHIPSException {
        String lblFile = filename.replace("png", "LBL");
        BufferedReader reader = new BufferedReader(new FileReader(lblFile));
        String currentLine;

        int found = 0;
        boolean isImageRequestGroup = false;

        Logger.getLogger(MarsHips.class.getName()).log(Level.INFO, "Parsing file {0} ", filename);
        while ((currentLine = reader.readLine()) != null || found != NUMBER_VALUE_TO_EXTRACT) {
            String trimmedLine = currentLine.trim();
            if (trimmedLine.contains("FIXED_INSTRUMENT_AZIMUTH")) {
                horizontalCoordinates[0] = Math.toRadians(Double.valueOf(trimmedLine.split("= ")[1]));
                found++;
            } else if (trimmedLine.contains("FIXED_INSTRUMENT_ELEVATION")) {
                horizontalCoordinates[1] = Math.toRadians(Double.valueOf(trimmedLine.split("= ")[1]));
                found++;
            } else if (trimmedLine.startsWith("GROUP")) {
                isImageRequestGroup = (trimmedLine.contains("IMAGE_REQUEST_PARMS"));
            } else if (isImageRequestGroup && trimmedLine.contains("LINES")) {
                imageRequest[1] = Integer.parseInt(trimmedLine.split("= ")[1]);
                found++;
            } else if (isImageRequestGroup && trimmedLine.contains("LINE_SAMPLES")) {
                imageRequest[0] = Integer.parseInt(trimmedLine.split("= ")[1]);
                found++;
            } else if (trimmedLine.startsWith("HORIZONTAL_FOV")) {
                fov[0] = Math.toRadians(Double.valueOf(trimmedLine.split("= ")[1]));
                found++;
            } else if (trimmedLine.startsWith("VERTICAL_FOV")) {
                fov[1] = Math.toRadians(Double.valueOf(trimmedLine.split("= ")[1]));
                found++;
            }
        }
        if (found != NUMBER_VALUE_TO_EXTRACT) {
            throw new JHIPSException("Cant extract all needed metadata for processing.");
        }
    }

    /**
     * Adds automatically all PNG files to process from a specific directory
     * in the list of files to process.
     *
     * @param directory the directory in which the files must be added in the
     * processing
     * @param hips the JHIPS library
     * @throws IOException
     */
    public void processDirectory(File directory, MarsHips hips) throws IOException {
        FilenameFilter extensionFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                return lowercaseName.endsWith(".png");
            }
        };
        File[] listOfFiles = directory.listFiles(extensionFilter);
        for (File listOfFile : listOfFiles) {
            String file = listOfFile.getAbsolutePath();
            try {
                readMetadata(file);
                Logger.getLogger(MarsHips.class.getName()).log(Level.INFO, "Adding {0} as file to process", file);
                MetadataFile metadata = new MetadataFile(new URL("file://" + file), horizontalCoordinates[0], horizontalCoordinates[1], fov, Projection.ProjectionType.TAN);
                metadata.setImageRequest(imageRequest);
                hips.addFile(metadata);
            } catch (FileNotFoundException | JHIPSException ex) {
                Logger.getLogger(MarsHips.class.getName()).log(Level.WARNING, file + " is rejected from the list of files to process", ex);
            }
        }
    }

    /**
     * Main program.
     * @param args
     * @throws MalformedURLException
     * @throws JHIPSException
     * @throws IOException 
     */
    public static void main(String[] args) throws MalformedURLException, JHIPSException, IOException {
        MarsHips hProj = new MarsHips(10);
        hProj.setOutputDirectory(new File("/tmp/data"));
        hProj.processDirectory(new File("/home/malapert/Documents/MARS/PanoData/jcm"), hProj);
        //hProj.processDirectory(new File("/home/malapert/Documents/MARS/PanoData"), hProj);
        //hProj.processDirectory(new File("/home/malapert/Documents/MARS/PanoData/jcm/Test"), hProj);
        hProj.process();
        hProj.createRGBTiles(true);
    }

}
