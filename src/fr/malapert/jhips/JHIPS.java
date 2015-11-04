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
package fr.malapert.jhips;

import healpix.essentials.HealpixBase;
import healpix.essentials.HealpixUtils;
import healpix.essentials.Pointing;
import healpix.essentials.Scheme;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Creates a HIPS for each chanel from CAR projection
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class JHIPS {

    private static final int DEFAULT_ORDER = 10;
    public static final int order_max = 29;    
    
    private File outputDirectory = new File("/tmp/data");    
    private long nside;
    private static final HIPSGeneration hips = new HIPSGeneration();
    
    /**
     * Main program. 
     * Takes the JPEG inputFile in CAR projection as first argument
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        System.out.println("processing http://www.fvalk.com/images/MaptoGeo/world-view-total.jpg");
        JHIPS hProj = new JHIPS();
        hProj.process(new URL("http://www.fvalk.com/images/MaptoGeo/world-view-total.jpg"));
    }
    
    /**
     * Create the instance
     * @param file 
     */
    private JHIPS() {       
        this.nside = (long) Math.pow(2, DEFAULT_ORDER);
    }    

    /**
     * Set the order.
     * @param order Healpix order
     */
    public void setOrder(int order) {
        this.nside = (long) Math.pow(2, order);
    }

    /**
     * Gets the order
     * @return the Healpix order
     * @throws Exception 
     */
    public int getOrder() throws Exception {
        return HealpixBase.nside2order(this.nside);
    }

    /**
     * Returns the output directory
     * @return the outputDirectory
     */
    private File getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Sets the output directory
     * @param outputDirectory the outputDirectory to set
     */
    private void setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    /**
     * Process the Healpix projection.
     * 
     * @param inputFile The file to ocess
     * @throws Exception 
     */
    public void process(final URL inputFile) throws Exception {
        BufferedImage img = readJpgImage(inputFile);
        int computedNside = computeNside(img.getHeight());
        HealpixBase hpx = initHealpixMap(computedNside);
        List<String> filesHMapToProcess = createHealpixVector(hpx, img);        
        generateHips(filesHMapToProcess);
    }

    /**
     * Read the JPEG inputFile
     * @param file
     * @return a buffered image
     */
    protected BufferedImage readJpgImage(final URL file) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(file);
        } catch (IOException e) {
        }
        return img;
    }

    /**
     * Computes nside
     * @param height height of the JPEG inputFile
     * @return nside
     */
    protected int computeNside(int height) {
        double pixSize = 180 * 60 * 60 / height;
        return calculateNSide(pixSize);
    }

    /**
     * Init the Healpix vector
     * @param computedNside
     * @return Healpix vector in NESTED mode
     * @throws Exception 
     */
    protected HealpixBase initHealpixMap(long computedNside) throws Exception {
        return new HealpixBase(computedNside, Scheme.NESTED);
    }

    /**
     * Fill the Healpix vector for each RGB of the buffered image
     * @param hpx Healpix index
     * @param img JPEG buffer
     * @return The RGB HIPS of the buffered image
     * @throws Exception 
     */
    protected List<String> createHealpixVector(HealpixBase hpx, BufferedImage img) throws Exception {
        List<String> filesHMapToProcess = new ArrayList();
        HealpixMapByte hpxByteR = new HealpixMapByte(hpx.getNside(), Scheme.NESTED);
        HealpixMapByte hpxByteG = new HealpixMapByte(hpx.getNside(), Scheme.NESTED);
        HealpixMapByte hpxByteB = new HealpixMapByte(hpx.getNside(), Scheme.NESTED);        
        long nPix = hpx.getNpix();
        for (long pixel = 0; pixel < nPix; pixel++) {
            Pointing pt = hpx.pix2ang(pixel); 
            int[] coords = getPixelValueFromSphericalCoordinates(pt, img);
            int rgb = img.getRGB(coords[0], coords[1]);
            Color c = new Color(rgb);
            hpxByteR.setPixel(pixel, (byte) c.getRed());
            hpxByteG.setPixel(pixel, (byte) c.getGreen());
            hpxByteB.setPixel(pixel, (byte) c.getBlue());
        }
        filesHMapToProcess.add(getOutputDirectory().getAbsolutePath()+"/r.fits");
        filesHMapToProcess.add(getOutputDirectory().getAbsolutePath()+"/g.fits");
        filesHMapToProcess.add(getOutputDirectory().getAbsolutePath()+"/b.fits");
        FITSUtil.writeByteMap(hpxByteR, filesHMapToProcess.get(0));
        FITSUtil.writeByteMap(hpxByteG, filesHMapToProcess.get(1));
        FITSUtil.writeByteMap(hpxByteB, filesHMapToProcess.get(2));
        return filesHMapToProcess;
    }
    
    /**
     * Generates HIPS for each Healpix map file
     * @param filesHMapToProcess Healpix Map files
     */
    protected void generateHips(final List<String> filesHMapToProcess) {        
        for (String iterFile:filesHMapToProcess)
            hips.process(iterFile);
    }    
    
    /**
     * Computes required nside given pixel size in arcsec
     *
     * @param pixsize in arcsec
     * @return long nside parameter
     */
    private int calculateNSide(double pixsize) {
        double arcsec2rad = Math.PI / (180. * 60. * 60.);
        double nsd = Math.sqrt(4 * Math.PI / 12.) / (arcsec2rad * pixsize);
        int order_req = Math.max(0, Math.min(order_max, 1 + HealpixUtils.ilog2((long) (nsd))));
        return 1 << order_req;
    }


    /**
     * Gets the pixel coordinate of the JPEG from spherical coordinate of the Healpix index
     * @param pt Healpix location on the sphere
     * @param img buffer
     * @return the pixel coordinate according to the pointing
     */
    private int[] getPixelValueFromSphericalCoordinates(Pointing pt, BufferedImage img) {
        int x = (int) ((pt.phi + Math.PI) * img.getWidth() / (2*Math.PI));
        if(x>=img.getWidth()) {
            x = x - img.getWidth();
        }
        int y = (int) (pt.theta * img.getHeight() / Math.PI);
        return new int[]{x, y};
    }

}
