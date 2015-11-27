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
 *****************************************************************************
 */
package io.github.malapert.jhips;

import io.github.malapert.jhips.metadata.MastPDSMetadata;
import io.github.malapert.jhips.metadata.MetadataFile;
import io.github.malapert.jhips.algorithm.Projection;
import io.github.malapert.jhips.exception.JHIPSException;
import io.github.malapert.jhips.metadata.JHipsMetadataProviderInterface;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Program to generate a panorama from MAST camera images on Curiosity.
 *
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class MastPanorama extends JHIPS {

    //private final static double[] DISTORTION_COEFF_MAST_LEFT
    /**
     * Creates a HIPS map with a specific resolution.
     *
     * @param order Healpix order
     */
    public MastPanorama(int order) {
        super(order);
    }

    /**
     * Creates a HIPS map.
     */
    public MastPanorama() {
        super();
    }

    /**
     * Adds automatically all PNG files to process from a specific directory in
     * the list of files to process.
     *
     * @param directory the directory in which the files must be added in the
     * processing
     * @param hips the JHIPS library
     * @throws io.github.malapert.jhips.exception.JHIPSException
     */
    public void processDirectory(File directory, MastPanorama hips) throws JHIPSException {
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
                JHipsMetadataProviderInterface pds = new MastPDSMetadata(new URL("file://" + file));
                Logger.getLogger(MastPanorama.class.getName()).log(Level.INFO, "Adding {0} as file to process", file);
                MetadataFile metadata = new MetadataFile(pds, Projection.ProjectionType.TAN);
                hips.addFile(metadata);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MastPanorama.class.getName()).log(Level.WARNING, file + " is rejected from the list of files to process", ex);
            } catch (IOException ex) {
                Logger.getLogger(MastPanorama.class.getName()).log(Level.WARNING, file + " is rejected from the list of files to process", ex);
            }
        }
    }

    /**
     * Main program.
     *
     * @param args
     * @throws JHIPSException
     */
    public static void main(String[] args) throws JHIPSException {
        MastPanorama hProj = new MastPanorama(10);
        hProj.setOutputDirectory(new File("/tmp/data"));
        //hProj.processDirectory(new File("/home/malapert/Documents/MARS/test"), hProj);
        //hProj.processDirectory(new File("/home/malapert/Documents/MARS/SOL739"), hProj);
        hProj.processDirectory(new File("/home/malapert/Documents/MARS/PanoData/jcm"), hProj);
        //hProj.processDirectory(new File("/home/malapert/Documents/MARS/PanoData"), hProj);
        //hProj.processDirectory(new File("/home/malapert/Documents/MARS/PanoData/jcm/Test"), hProj);
        hProj.process();
        hProj.createRGBTiles(true);
    }

}
//output(x,y) = (input(x,y) - RADIANCE_OFFSET) / RADIANCE_SCALING_FACTOR + 0.5

//Exposure time is then removed. Exposure time comes from EXPOSURE_DURATION, converted
//to seconds:
//output(x,y) = input(x,y) / exposure_time

//INSTRUMENT_ID
