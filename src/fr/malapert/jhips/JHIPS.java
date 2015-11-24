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

import fr.malapert.jhips.metadata.MetadataFileCollection;
import fr.malapert.jhips.metadata.MetadataFile;
import fr.malapert.jhips.algorithm.HIPSGeneration;
import fr.malapert.jhips.algorithm.HealpixMapByte;
import fr.malapert.jhips.algorithm.RGBGeneration;
import fr.malapert.jhips.util.FITSUtil;
import fr.malapert.jhips.exception.JHIPSException;
import fr.malapert.jhips.util.Utils;
import healpix.essentials.HealpixBase;
import healpix.essentials.HealpixUtils;
import healpix.essentials.Scheme;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JHIPS is the library base class for creating HIPS (Hierarchical Progressive
 * Surveys) from a set of graphic images. JHIPS uses Healpix as sphere
 * partitioning and the CDS library for tiles generation. JHIPS object
 * encapsulates the state information needed for HIPS computing operations. This
 * state information includes:
 * <ul>
 * <li>The output directory to write the result
 * <li>The operations to add files to process
 * <li>The processing operations
 * </ul>
 * <p>
 * The processing operations include the following steps :
 * <ul>
 * <li>The scale computing based on all images to process
 * <li>The Nside parameter computing for the generation of the index deep
 * <li>The initialization of Healpix index based on Nside parameter
 * <li>The creation of the Healpix vector for each channel RGB
 * <li>The operations to fill the Healpix Vector.
 * <li>The generation of tiles based on the Healpix vector
 * </ul>
 *
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class JHIPS {

    /**
     * Transforms one degree in arc seconds.
     */
    public static final double DEG2ARCSEC = 3600;

    /**
     * Max index order = 29.
     */
    public static final int ORDER_MAX = 29;

    /**
     * Selecting index order and initialized to ORDER_MAX.
     */
    public static int ORDER = ORDER_MAX;

    /**
     * Output directory to store the result.
     */
    private File outputDirectory = new File("/tmp/data");

    /**
     * List of files to process.
     */
    public final MetadataFileCollection files = new MetadataFileCollection();

    /**
     * Initialize HIPS generation.
     */
    private static final HIPSGeneration hips = new HIPSGeneration();

    /**
     * Main program.
     *
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        System.out.println("processing http://www.fvalk.com/images/MaptoGeo/world-view-total.jpg");
        JHIPS hProj = new JHIPS();
        hProj.setOutputDirectory(new File("/tmp/data"));
        hProj.process(new URL("http://www.fvalk.com/images/MaptoGeo/world-view-total.jpg"), 0, 0, new double[]{Math.PI * 2, Math.PI}, fr.malapert.jhips.algorithm.Projection.ProjectionType.CAR);
    }

    /**
     * Creates a JHIPS instance with the default order. Order gives the mesh
     * resolution of the sphere. Order goes from 0 to ORDER_MAX according to the
     * resolution accuracy.
     *
     * @see JHIPS.ORDER
     * @see JHIPS.ORDER_MAX
     */
    public JHIPS() {
        this(JHIPS.ORDER);
    }

    /**
     * Creates a JHIPS instance at a specific order
     *
     * @param order Mesh resolution of the Healpix index
     */
    public JHIPS(int order) {
        JHIPS.ORDER = order;
    }

    /**
     * Returns the output directory in which the tiles are generated.
     *
     * @return the outputDirectory
     */
    public File getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Sets the output directory in which the tiles are generated.
     *
     * @param outputDirectory the outputDirectory to set
     */
    public void setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Returns the list of files to process in order to merge them in the same
     * sphere
     *
     * @return the files
     */
    public MetadataFileCollection getFiles() {
        return files;
    }

    /**
     * Add files to process in order to merge them in the same sphere.
     *
     * @param files the files to set
     */
    public void addFiles(final List<MetadataFile> files) {
        for (MetadataFile file : files) {
            this.files.addMetadataFile(file);
        }
    }

    /**
     * Add a file to process in order to merge it with other files in the same
     * sphere. Each file is associated with its own camera's center, its FOV and
     * its projection.
     *
     * @param file the file to add
     * @param azimuthCenter azimuth of the image center expressed in radians
     * @param elevationCenter elevation of the image center expressed in radians
     * @param fov the field of view in radians along azimuth and elevation
     * @param type the projection of the file to add
     * @throws java.io.IOException error when loading the file
     */
    public void addFile(final URL file, double azimuthCenter, double elevationCenter, double[] fov, fr.malapert.jhips.algorithm.Projection.ProjectionType type) throws IOException {
        this.addFile(new MetadataFile(file, azimuthCenter, elevationCenter, fov, type));
    }

    /**
     * Add a file to process in order to merge it with other files in the same
     * sphere. Each file is associated with its own camera's center, its FOV and
     * its projection.
     *
     * @param file the file to add
     * @throws IOException error when loading a file
     */
    public void addFile(MetadataFile file) throws IOException {
        this.files.addMetadataFile(file);
    }

    /**
     * Process the files in tiles.
     * <p>
     * The processing operations include the following steps :
     * <ul>
     * <li>The scale computing based on all images to process
     * <li>The Nside parameter computing for the generation of the index deep
     * <li>The initialization of Healpix index based on Nside parameter
     * <li>The creation of the Healpix vector for each channel RGB
     * <li>The operations to fill the Healpix Vector.
     * <li>The generation of tiles based on the Healpix vector
     * </ul>
     *
     * @throws JHIPSException error while processing
     */
    public void process() throws JHIPSException {
        try {
            // create directory
            getOutputDirectory().mkdirs();

            Logger.getLogger(JHIPS.class.getName()).log(Level.INFO, "{0} files are being processed ... ", getFiles().size());

            double scale = Math.toDegrees(getFiles().getScale()) * DEG2ARCSEC;
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
     * Process a file in tiles.
     *
     * @param inputFile The file to process
     * @throws JHIPSException error while processing
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
     * Initializes the Healpix index.
     *
     * @param computedNside the nside parameter for defining the mesh resolution
     * @return Healpix the index in NESTED mode
     * @throws Exception Healpix error
     */
    protected HealpixBase initHealpixMap(long computedNside) throws Exception {
        return new HealpixBase(computedNside, Scheme.NESTED);
    }

    /**
     * Creates and fills the three Healpix vectors (RGB) for all files.
     *
     * @param files List of files to project on the sphere
     * @param hpx Healpix index
     * @return The path of the Healpix vectors (RGB)
     * @throws Exception Healpix error
     */
    protected List<String> createHealpixVector(final MetadataFileCollection files, final HealpixBase hpx) throws Exception {
        List<String> filesHMapToProcess = new ArrayList();
        HealpixMapByte hpxByteR = new HealpixMapByte(hpx.getNside(), Scheme.NESTED);
        HealpixMapByte hpxByteG = new HealpixMapByte(hpx.getNside(), Scheme.NESTED);
        HealpixMapByte hpxByteB = new HealpixMapByte(hpx.getNside(), Scheme.NESTED);
        fillHealpixVector(hpx, files, hpxByteR, hpxByteG, hpxByteB);
        filesHMapToProcess.add(getOutputDirectory().getAbsolutePath() + "/r.fits");
        filesHMapToProcess.add(getOutputDirectory().getAbsolutePath() + "/g.fits");
        filesHMapToProcess.add(getOutputDirectory().getAbsolutePath() + "/b.fits");
        FITSUtil.writeByteMap(hpxByteR, filesHMapToProcess.get(0));
        FITSUtil.writeByteMap(hpxByteG, filesHMapToProcess.get(1));
        FITSUtil.writeByteMap(hpxByteB, filesHMapToProcess.get(2));
        return filesHMapToProcess;
    }

    /**
     * Fills the Healpix vectors for earch RGB channel. The Healpix vectors are
     * filled by iterating on each pixel on the sphere.
     * <p>
     * For each pixel, we get the mean RGB pixel of all files that contains this
     * pixel.
     *
     * @param hpx Healpix index
     * @param collection List of files to process
     * @param hpxByteR Healpix vector in R color
     * @param hpxByteG Healpix vector in G color
     * @param hpxByteB Healpix vector in B color
     * @throws Exception Healpix error
     */
    private void fillHealpixVector(final HealpixBase hpx, final MetadataFileCollection collection, final HealpixMapByte hpxByteR, final HealpixMapByte hpxByteG, final HealpixMapByte hpxByteB) throws Exception {
        long nPix = hpx.getNpix();
        for (long pixel = 0; pixel < nPix; pixel++) {
            Utils.monitoring(pixel, nPix);
            Color c = collection.getRGB(hpx, pixel);
            if (c != null) {
                hpxByteR.setPixel(pixel, (byte) c.getRed());
                hpxByteG.setPixel(pixel, (byte) c.getGreen());
                hpxByteB.setPixel(pixel, (byte) c.getBlue());
            }
        }
        Utils.monitoring(nPix, nPix);
    }

    /**
     * Generates tiles for each Healpix vector.
     *
     * @param filesHMapToProcess path to the Healpix vectors
     */
    protected void generateHips(final List<String> filesHMapToProcess) {
        for (String iterFile : filesHMapToProcess) {
            hips.process(iterFile);
        }
    }

    /**
     * Computes the required nside given the pixel size in arcsec.
     *
     * @param pixsize the pixel size in arcsec
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
     * Creates RGB tiles and removes intermediate tiles.
     * @param removeIntermediateFiles intermediate files
     */
    public void createRGBTiles(boolean removeIntermediateFiles) {
        try {
            RGBGeneration.create(getOutputDirectory());
            if (removeIntermediateFiles) {
                removeDirectory(getOutputDirectory().toString() + File.separator + RGBGeneration.R_DIRECTORY);
                removeDirectory(getOutputDirectory().toString() + File.separator + RGBGeneration.G_DIRECTORY);
                removeDirectory(getOutputDirectory().toString() + File.separator + RGBGeneration.B_DIRECTORY);
            }
        } catch (IOException ex) {
            Logger.getLogger(JHIPS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Deletes recursively a directory.
     * @param directoryToDelete directory to delete
     * @throws IOException 
     */
    private void removeDirectory(String directoryToDelete) throws IOException {
        Path directory = Paths.get(directoryToDelete);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

        });
    }
}
