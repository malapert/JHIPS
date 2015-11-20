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

import fr.malapert.jhips.algorithm.HIPSGeneration;
import fr.malapert.jhips.algorithm.HealpixMapByte;
import fr.malapert.jhips.util.FITSUtil;
import fr.malapert.jhips.exception.JHIPSException;
import fr.malapert.jhips.exception.JHIPSOutputImageException;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a HIPS for each chanel from CAR projection
 *
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class JHIPS {

    /**
     * Transforms one degree in arcseconds.
     */
    public static final double DEG2ARCSEC = 3600;
    /**
     * Max Healpix order.
     */
    public static final int ORDER_MAX = 29;
    /**
     * Selecting Healpix order and initialized to ORDER_MAX
     */
    public static int ORDER = ORDER_MAX;

    /**
     * Output directory to store the result
     */
    private File outputDirectory = new File("/tmp/data");

    // List of files to process
    public final MetadataFileCollection files = new MetadataFileCollection();

    private static final HIPSGeneration hips = new HIPSGeneration();

    /**
     * Main program. Takes the JPEG inputFile in CAR projection as first
     * argument
     *
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        System.out.println("processing http://www.fvalk.com/images/MaptoGeo/world-view-total.jpg");
        JHIPS hProj = new JHIPS();
        hProj.process(new URL("http://www.fvalk.com/images/MaptoGeo/world-view-total.jpg"),0,0,new double[]{Math.PI*2,Math.PI}, fr.malapert.jhips.algorithm.Projection.ProjectionType.CAR);
    }

    /**
     * Creates a JHIPS instance with the specific order JHIPS.ORDER
     */
    public JHIPS() {
        this(JHIPS.ORDER);
    }

    /**
     * Creates a JHIPS instance at a specific order
     *
     * @param order Mesh's resolution of the Healpix index
     */
    public JHIPS(int order) {
        JHIPS.ORDER = order;
    }

    /**
     * Returns the output directory
     *
     * @return the outputDirectory
     */
    public File getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Sets the output directory
     *
     * @param outputDirectory the outputDirectory to set
     */
    public void setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Returns the list of files to process in order to merge all files in the
     * same sky
     *
     * @return the files
     */
    public MetadataFileCollection getFiles() {
        return files;
    }

    /**
     * Add files to process in order to merge them in the same sky
     *
     * @param files the files to set
     */
    public void addFiles(final List<MetadataFile> files) {
        for (MetadataFile file : files) {
            this.files.addMetadataFile(file);
        }
    }

    /**
     * Add file to process in order to merge it with other files in the same
     * sky.
     *
     * <p>
     * Each file is associated with its own camera's center and its FOV
     * reference frame
     *
     * @param file the file to set
     * @param azimuthCenter
     * @param elevationCenter
     * @param fov
     * @throws java.io.IOException
     */
    public void addFile(final URL file, double azimuthCenter, double elevationCenter, double[] fov, fr.malapert.jhips.algorithm.Projection.ProjectionType type) throws IOException {
        this.addFile(new MetadataFile(file, azimuthCenter, elevationCenter, fov, type));
    }
    
    public void addFile(MetadataFile file) throws IOException {
        this.files.addMetadataFile(file);
    }    

    /**
     * Projects all files on the sphere.
     *
     * @throws JHIPSException
     */
    public void process() throws JHIPSException  {
        try {
            Logger.getLogger(JHIPS.class.getName()).log(Level.INFO, "{0} files are being processed ... ", getFiles().size());

            double scale = Math.toDegrees(getFiles().getScale())*DEG2ARCSEC;
            Logger.getLogger(JHIPS.class.getName()).log(Level.INFO, "Computing pixel size ...  {0} arsec ", scale);

            int nside = calculateNSide(scale);

            Logger.getLogger(JHIPS.class.getName()).log(Level.INFO, "The Healpix index is being processed ... ");
            HealpixBase hpx = initHealpixMap(nside);

            Logger.getLogger(JHIPS.class.getName()).log(Level.INFO, "Creating Healpix vector ... ");
            List<String> filesHMapToProcess = createHealpixVector(getFiles(), hpx);

            Logger.getLogger(JHIPS.class.getName()).log(Level.INFO, "Creating HIPS ... ");
            generateHips(filesHMapToProcess);
        } catch (Exception ex) {
            throw new JHIPSException(ex);
        }
    }

    /**
     * Projects a file on the sphere.
     *
     * @param inputFile The file to process
     * @throws JHIPSException
     */
    public void process(final MetadataFile inputFile) throws JHIPSException {
        process(inputFile.getFile(), inputFile.getCameraLongitude(), inputFile.getCameraLatitude(), inputFile.getCameraFov(), inputFile.getType());
    }
    
    public void process(final URL file, double azimuthCenter, double elevationCenter, double[] fov, fr.malapert.jhips.algorithm.Projection.ProjectionType type) throws JHIPSException {
        try {
            addFile(file, azimuthCenter, elevationCenter, fov, type);
            process();
        } catch (IOException ex) {
            throw new JHIPSException(ex);
        }
    }    

    /**
     * Init the Healpix vector
     *
     * @param computedNside
     * @return Healpix vector in NESTED mode
     * @throws Exception
     */
    protected HealpixBase initHealpixMap(long computedNside) throws Exception {
        return new HealpixBase(computedNside, Scheme.NESTED);
    }

    /**
     * Fill the Healpix vector for each RGB of all files.
     *
     * @param files List of files to project on the sphere
     * @param hpx Healpix index
     * @return The RGB HIPS of the buffered image
     * @throws Exception
     */
    protected List<String> createHealpixVector(final MetadataFileCollection files, final HealpixBase hpx) throws Exception {
        List<String> filesHMapToProcess = new ArrayList();
        HealpixMapByte hpxByteR = new HealpixMapByte(hpx.getNside(), Scheme.NESTED);
        HealpixMapByte hpxByteG = new HealpixMapByte(hpx.getNside(), Scheme.NESTED);
        HealpixMapByte hpxByteB = new HealpixMapByte(hpx.getNside(), Scheme.NESTED);   
        fillHealpixVectorForOneFile(hpx, files, hpxByteR, hpxByteG, hpxByteB);
        filesHMapToProcess.add(getOutputDirectory().getAbsolutePath() + "/r.fits");
        filesHMapToProcess.add(getOutputDirectory().getAbsolutePath() + "/g.fits");
        filesHMapToProcess.add(getOutputDirectory().getAbsolutePath() + "/b.fits");
        FITSUtil.writeByteMap(hpxByteR, filesHMapToProcess.get(0));
        FITSUtil.writeByteMap(hpxByteG, filesHMapToProcess.get(1));
        FITSUtil.writeByteMap(hpxByteB, filesHMapToProcess.get(2));
        return filesHMapToProcess;
    }

    /**
     * Fills the Healpix vector for one file.
     *
     * @param hpx Healpix index
     * @param img Image to project on the sphere
     * @param hpxByteR Healpix vector in R color
     * @param hpxByteG Healpix vector in G color
     * @param hpxByteB Healpix vector in B color
     * @throws Exception Healpix exception
     */
    private void fillHealpixVectorForOneFile(final HealpixBase hpx, final MetadataFileCollection collection, final HealpixMapByte hpxByteR, final HealpixMapByte hpxByteG, final HealpixMapByte hpxByteB) throws Exception {
        final String anim= "|/-\\";
        long nPix = hpx.getNpix();
        for (long pixel = 0; pixel < nPix; pixel++) {
            int percent = (int) (pixel * 100 / nPix);
            if (percent%10 == 0) {
                System.out.print("\r"+ anim.charAt((int)pixel%anim.length())+" "+percent+"% done");
                System.out.flush();
            }
           
            Pointing pt = hpx.pix2ang(pixel);
            Color c = collection.getRGB(pt.phi, 0.5 * Math.PI - pt.theta);
            if (c != null) {
                hpxByteR.setPixel(pixel, (byte) c.getRed());
                hpxByteG.setPixel(pixel, (byte) c.getGreen());
                hpxByteB.setPixel(pixel, (byte) c.getBlue());
            }
        }
        System.out.println("\r"+ anim.charAt((int)nPix%anim.length())+" 100% done");
    }
                   

    /**
     * Generates HIPS for each Healpix map file
     *
     * @param filesHMapToProcess Healpix Map files
     */
    protected void generateHips(final List<String> filesHMapToProcess) {
        for (String iterFile : filesHMapToProcess) {
            hips.process(iterFile);
        }
    }

    /**
     * Computes required nside given pixel size in arcsec
     *
     * @param pixsize in arcsec
     * @return long nside parameter
     */
    protected int calculateNSide(double pixsize) {
        double arcsec2rad = Math.PI / (180. * 60. * 60.);
        double nsd = Math.sqrt(4 * Math.PI / 12.) / (arcsec2rad * pixsize);
        int order_req = Math.max(0, Math.min(ORDER, 1 + HealpixUtils.ilog2((long) (nsd))));
        Logger.getLogger(JHIPS.class.getName()).log(Level.INFO, "Selecting order={0} / instead of {1}", new Object[]{order_req, 1 + HealpixUtils.ilog2((long) (nsd))});
        //Logger.getLogger(JHIPS.class.getName()).log(Level.INFO, "Space required ... {0} Mb", 512*Math.pow(2, order_req) / 1024 / 1024 * 3);
        return 1 << order_req;
    }

    /**
     * Gets the pixel coordinate of the JPEG from spherical coordinate of the
     * Healpix index
     *
     * @param pt Healpix location on the sphere
     * @param img buffer
     * @return the pixel coordinate according to the pointing
     * @throws fr.malapert.jhips.exception.JHIPSOutputImageException No pixel to
     * extract
     */
    protected int[] getPixelValueFromSphericalCoordinates(Pointing pt, BufferedImage img) throws JHIPSOutputImageException {
        int x = (int) ((pt.phi + Math.PI) * img.getWidth() / (2 * Math.PI));
        if (x >= img.getWidth()) {
            x = x - img.getWidth();
        }
        //Flip X coordinate
        x = (int) Math.abs(x - (img.getWidth() - 1));

        int y = (int) (pt.theta * img.getHeight() / Math.PI);
        return new int[]{x, y};
    }

}
