/**
 * *****************************************************************************
 * Copyright 2015 - Jean-Christophe Malapert (jcmalapert@gmail.com)
 *
 * This file is part of JHIPS.
 *
 * JHIPS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * JHIPS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * JHIPS. If not, see <http://www.gnu.org/licenses/>.
 * ****************************************************************************
 */
package fr.malapert.jhips;

import fr.malapert.jhips.algorithm.Projection;
import fr.malapert.jhips.exception.JHIPSException;
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
 *
 * @author Jean-Christophe Malapert
 */
public class MarsHips extends JHIPS {
    
    //[azimuth, elevation] in radians
    private final double[] horizontalCoordinates = new double[] {Double.NaN, Double.NaN};
    
    // Camera's FOV in radians
    private final double[] fov  = new double[] {Double.NaN, Double.NaN};
    
    private final int[] imageRequest = new int[]{0,0};
    
    private final static int NUMBER_VALUE_TO_EXTRACT = 6;

    public MarsHips() {
        super(12);
        // remove 12 to get the full resolution but need space on disk and memory
    }

    public void readMetadata(String filename) throws FileNotFoundException, IOException {
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
            } else if(trimmedLine.startsWith("HORIZONTAL_FOV")) {
                fov[0] = Math.toRadians(Double.valueOf(trimmedLine.split("= ")[1]));
                found++;
            } else if(trimmedLine.startsWith("VERTICAL_FOV")) {
                fov[1] = Math.toRadians(Double.valueOf(trimmedLine.split("= ")[1]));
                found++;
            }           
        }
        
        //TODO Vérifier que fov et horizontalRequest soient remplis
    }

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
            readMetadata(file);
            Logger.getLogger(MarsHips.class.getName()).log(Level.INFO, "Adding {0} as file to process", file);
            MetadataFile metadata = new MetadataFile(new URL("file://"+file), horizontalCoordinates[0], horizontalCoordinates[1], fov, Projection.ProjectionType.TAN);
            metadata.setImageRequest(imageRequest);
            hips.addFile(metadata);
        }
    }

    public static void main(String[] args) throws MalformedURLException, JHIPSException, IOException {
        MarsHips hProj = new MarsHips();
        hProj.setOutputDirectory(new File("/tmp/data"));
//        hProj.processDirectory(new File("/home/malapert/Documents/MARS/PanoData/jcm"), hProj);
//        hProj.processDirectory(new File("/home/malapert/Documents/MARS/PanoData"), hProj);
        hProj.processDirectory(new File("/home/malapert/Documents/MARS/PanoData"), hProj);
        hProj.process();
    }

}
