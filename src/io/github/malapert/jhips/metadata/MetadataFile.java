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
package io.github.malapert.jhips.metadata;

import cds.moc.HealpixMoc;
import cds.moc.MocCell;
import com.sun.javafx.scene.CameraHelper;
import healpix.essentials.HealpixBase;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import io.github.malapert.jwcs.JWcs;
import io.github.malapert.jwcs.WcsNumericalMap;
import io.github.malapert.jwcs.proj.exception.JWcsException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Stores the image metadata and provides operation on it.
 *
 * <p>
 * The stored image metadata are the following
 * <ul>
 * <li>the image location
 * <li>the buffered image
 * <li>the image coordinates expressed in horizontal frame in radians
 * <li>the camera's FOV according to longitude x latitude in radians
 * <li>the pixel scale in radians/pixel
 * <li>the spatial index of the image
 * <li>the projection type that is associated to the image
 * </ul>
 *
 * <p>
 * The main operations are the following:
 * <ul>
 * <li>gets the color of a pixel at a azimuth/elevation
 * <li>computes automatically the scale in radians/pixel
 * <li>computes if a Healpix pixel intersects with the image spatial index.
 * </ul>
 *
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class MetadataFile {

    /**
     * Acquisition file from the camera.
     */
    private final URL file;

    /**
     * Buffered image.
     */
    private final BufferedImage image;
    /**
     * Longitude of the camera's center in radians.
     */
    private double cameraLongitude;
    /**
     * Latitude of the camera's center in radians.
     */
    private double cameraLatitude;
    /**
     * Camera's FOV [along longitude, along latitude] in radians.
     */
    private final double[] cameraFov;

    /**
     * Pixel's scale in rad/pixel.
     */
    private double[] scale;

    /**
     * The real size of the image in pixels.
     * <p>
     * The requested image can be smaller or higher then the detector. This
     * depends on the acquisition mode.
     */
    private int[] imageRequest;

    /**
     * The spatial index of the file.
     */
    private HealpixMoc index;

    /**
     * The projection code associated to the acquisition.
     */
    private io.github.malapert.jhips.algorithm.Projection.ProjectionType type;

    private WcsNumericalMap wcs;

    /**
     * creates an instance to store the file's metadata. By default, the
     * projection for an image is TAN.
     *
     * @param file file to store
     * @param longitude camera's center along longitude in radians
     * @param latitude camera's center along latitude in radians
     * @param cameraFov camera's fov along longitude and latitude in radians
     * @throws IOException When an error happens by reading the file
     */
    public MetadataFile(final URL file, double longitude, double latitude, double[] cameraFov) throws IOException {
        this(file, longitude, latitude, cameraFov, io.github.malapert.jhips.algorithm.Projection.ProjectionType.TAN);
    }

    /**
     * Creates an instance to store the file's metadata. By default, the
     * projection for an image is TAN and the light reaching the camera takes
     * all the camera's detector.
     *
     * @param file file to store
     * @param longitude camera's center along longitude in radian
     * @param latitude camera's center along latitude in radian
     * @param cameraFov camera's fov along longitude and latitude in radian
     * @param type projection's type of the image
     * @throws IOException When an error happens by reading the file
     */
    public MetadataFile(final URL file, double longitude, double latitude, double[] cameraFov, io.github.malapert.jhips.algorithm.Projection.ProjectionType type) throws IOException {
        this.file = file;
        this.image = ImageIO.read(file);
        this.imageRequest = new int[]{this.image.getWidth(), this.image.getHeight()};
        this.cameraLongitude = longitude;
        this.cameraLatitude = latitude;
        this.cameraFov = cameraFov;
        this.type = type;
        this.scale = initPixelScale(this.imageRequest, cameraFov);
        this.index = createIndex(longitude, latitude, cameraFov);
        this.wcs = createWcs();
        try {
            this.wcs.doInit();
        } catch (JWcsException ex) {
            Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Computes a spatial index of the image in order to boost the processing.
     *
     * @param longitude the azimuth in radians of the center of the image
     * @param latitude the elevation in radians of the center of the image.
     * @param cameraFOV the FOV in radians of the camera along azimuth and
     * elevation
     * @return the spatial index
     */
    private HealpixMoc createIndex(double longitude, double latitude, double[] cameraFOV) {
        HealpixMoc moc;
        try {
            moc = new HealpixMoc();
            HealpixBase base = new HealpixBase(1024, Scheme.NESTED);
            double radius = Math.sqrt(0.5 * cameraFOV[0] * 0.5 * cameraFOV[0] + 0.5 * cameraFOV[1] * 0.5 * cameraFOV[1]);
            RangeSet range = base.queryDiscInclusive(new Pointing(0.5 * Math.PI - latitude, longitude), radius, 128);
            RangeSet.ValueIterator iter = range.valueIterator();
            while (iter.hasNext()) {
                final long pixNest = iter.next();
                moc.add(new MocCell(base.getOrder(), pixNest));
            }
        } catch (Exception ex) {
            Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, null, ex);
            moc = null;
        }
        return moc;
    }

    /**
     * Initializes the pixel scale in rad/pixel.
     *
     * @param imageRequest the real image size
     * @param cameraFov the camera's field of view
     * @return the pixel scale
     */
    private double[] initPixelScale(int[] imageRequest, double[] cameraFov) {
        return computeScale(imageRequest, cameraFov);
    }

    /**
     * Computes the scales in rad/pixels.
     *
     * @param imageRequest the real image size
     * @param cameraFov the camera's field of view in radians
     * @return the scale in radians along X and Y axis
     */
    private double[] computeScale(int[] imageRequest, double[] cameraFov) {
        double scaleX = cameraFov[0] / (imageRequest[0]);
        double scaleY = cameraFov[1] / (imageRequest[1]);
        return new double[]{scaleX, scaleY};
    }

    /**
     * Sets the real image request in pixels.
     *
     * @param imageRequest size of image where light is on
     */
    public void setImageRequest(int[] imageRequest) {
        this.imageRequest = imageRequest;
        this.scale = computeScale(imageRequest, cameraFov);
        this.wcs = this.createWcs();
        try {
            this.wcs.doInit();
        } catch (JWcsException ex) {
            Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns the real image request.
     *
     * @return the real image request
     */
    public int[] getImageRequest() {
        return this.imageRequest;
    }

    /**
     * Returns the scale in radians / pixel.
     *
     * @return the scale in radians / pixel
     */
    public double[] getScale() {
        return this.scale;
    }

    /**
     * Returns the file's location.
     *
     * @return the file
     */
    public URL getFile() {
        return file;
    }

    /**
     * Returns the camera's center longitude in radians.
     *
     * @return the cameraLongitude in radians
     */
    public double getCameraLongitude() {
        return cameraLongitude;
    }

    /**
     * Returns the camera's center latitude in radians.
     *
     * @return the cameraLatitude in radians
     */
    public double getCameraLatitude() {
        return cameraLatitude;
    }

    /**
     * Returns the camera's FOV in radians.
     *
     * @return the cameraFov
     */
    public double[] getCameraFov() {
        return cameraFov;
    }

    /**
     * Returns the image's width in pixels.
     *
     * @return the image's width
     */
    public int getWidth() {
        return this.imageRequest[0];
    }

    /**
     * Returns the image's height in pixels.
     *
     * @return the image's height
     */
    public int getHeight() {
        return this.imageRequest[1];
    }

    /**
     * Returns the projection's type of the image.
     *
     * @return the projection's type
     */
    public io.github.malapert.jhips.algorithm.Projection.ProjectionType getType() {
        return this.type;
    }

    /**
     * Creates Map for World Coordinate System (WCS) processing.
     * <p>
     * The WCS is used to define a projection system.
     *
     * @return a map that contains WCS keywords
     */
    private WcsNumericalMap createWcs() {
        Map map = new HashMap();
        map.put(JWcs.CRPIX1, 0.5d * getWidth());
        map.put(JWcs.CRPIX2, 0.5d * getHeight());
//        if(this.file.getFile().contains("0739ML0031641260205464E01_DRCL")) {
//            this.cameraLongitude = this.cameraLongitude - Math.toRadians(1);
//            this.cameraLatitude = this.cameraLatitude + Math.toRadians(0.15);
//        } 
        map.put(JWcs.CRVAL1, Math.toDegrees(getCameraLongitude()));
        map.put(JWcs.CRVAL2, Math.toDegrees(getCameraLatitude()));
        map.put(JWcs.CDELT1, Math.toDegrees(getScale()[0]));
        map.put(JWcs.CDELT2, Math.toDegrees(getScale()[1]));
        map.put(JWcs.NAXIS, 2);
        map.put(JWcs.NAXIS1, getWidth());
        map.put(JWcs.NAXIS2, getHeight());
        map.put(JWcs.CTYPE1, "RA---" + getType());
        map.put(JWcs.CTYPE1, "DEC--" + getType());
        map.put(JWcs.CROTA2, 0.0d); // No rotation for this instrument
        return new WcsNumericalMap(map);
    }

//    private double[] correctedLensDistortion(double[] xy) {
//        double a11 = 135.154157;
//        double a12  =-0.038589;
//        double a21 = 0;
//        double a22 = 135.135135;
//        double i0 = 588.405;
//        double j0 = 834.620;        
//        
//        //double resultX = i0 + a11*xy[0] + a12*xy[1];
//        //double resultY = j0 + a21*xy[0] + a22*xy[1];
//        double resultX = xy[0];
//        double resultY = xy[1];
//        double xCenterLens = getWidth() * 0.5;
//        double yCenterLens = getWidth() * 0.5;
//        double x0 = -0.113876;
//        double y0 = 0.152029;
//        double dx = xy[0]* 0.0074 - (xCenterLens * 0.0074 + x0) ; //https://www.google.fr/url?sa=t&rct=j&q=&esrc=s&source=web&cd=3&cad=rja&uact=8&ved=0ahUKEwj79qDMoKvJAhWC1hoKHUwQA28QFggvMAI&url=http%3A%2F%2Fwww.lpi.usra.edu%2Fmeetings%2Flpsc2011%2Fpdf%2F2738.pdf&usg=AFQjCNEUtyTBIlDLuKCrcIvATjI6uqY3wQ&sig2=8pFTQTqOUhOqdnfDYgRJBA&bvm=bv.108194040,d.d2s
//        double dy = xy[1]* 0.0074 - (yCenterLens * 0.0074 + y0) ;
//        double r = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
//        if (r != 0) {
//            double k1 = -1.118977e-04;
//            double k2 = -1.023513e-06;
//            double k3 = 0;
//            double dr = k1 * Math.pow(r, 3) + k2 * Math.pow(r, 5) + k3 * Math.pow(r, 7);
//            double deltaX = xy[0] * dr / r;
//            double deltaY = xy[1] * dr / r;
//            System.out.println(resultX+" , "+resultY+" - (dx,dy)=("+deltaX+" , "+deltaY);
//            resultX += deltaX;
//            resultY += deltaY;
//            
//        }
//        return new double[]{resultX, resultY};
//    }

    /**
     * Returns the RGB color from a pixel based on a longitude and latitude.
     *
     * @param longitude longitude in radians
     * @param latitude latitude in radians
     * @return the RGB color
     */
    public Color getRGB(double longitude, double latitude) {
        Color result = null;
        try {
            double[] xy = this.wcs.wcs2pix(Math.toDegrees(longitude), Math.toDegrees(latitude));
            //xy = correctedLensDistortion(xy);

            int x = (int) (xy[0] + 0.5 * (image.getWidth() - getWidth()));
            int y = (int) (image.getHeight() - 1 - (xy[1] + 0.5 * (image.getWidth() - getWidth())));

            int xmin = (int) ((this.image.getWidth() > getWidth())
                    ? Math.ceil((this.image.getWidth() - getWidth()) * 0.5)
                    : 0);
            int xmax = (int) ((this.image.getWidth() > getWidth())
                    ? Math.floor(getWidth() - (this.image.getWidth() - getWidth()) * 0.5)
                    : this.image.getWidth());
            int ymin = (int) ((this.image.getHeight() > getHeight())
                    ? Math.ceil((this.image.getHeight() - getHeight())) * 0.5
                    : 0);
            int ymax = (int) ((this.image.getHeight() > getHeight())
                    ? Math.floor(getWidth() - (this.image.getHeight() - getHeight()) * 0.5)
                    : this.image.getHeight());

            // The pixel to extract is outside the camera
            if (x >= xmax || y >= ymax || x < xmin || y < ymin) {
                result = null;
            } else {
                try {
                    result = new Color(image.getRGB(x, y));
                } catch (ArrayIndexOutOfBoundsException ex) {
                    Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, "Error when extracting values (x,y) = ({0},{1}) from file {2}", new Object[]{x, y, getFile().toString()});
                    Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, "(width, height) = ({0},{1}) , imgRequest=({2},{3})", new Object[]{getWidth(), getHeight(), getImageRequest()[0], getImageRequest()[1]});
                    result = null;
                }
            }
        } catch (io.github.malapert.jwcs.proj.exception.ProjectionException ex) {
            Logger.getLogger(MetadataFile.class.getName()).log(Level.FINEST, null, ex);
        }
        return result;
    }

    /**
     * Returns the RGB color from a pixel based on a Healpix pixel.
     *
     * @param hpx index
     * @param pixel pixel to get
     * @return the RGB color
     */
    public Color getRGB(final HealpixBase hpx, long pixel) {
        Color result;
        try {
            Pointing pt = hpx.pix2ang(pixel);
            result = getRGB(pt.phi, 0.5 * Math.PI - pt.theta);
        } catch (Exception ex) {
            Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, null, ex);
            result = null;
        }
        return result;
    }

    /**
     * Checks whether a pixel is inside the spatial index.
     *
     * @param order mesh resolution
     * @param pixel pixel to test
     * @return True when the pixel in inside the index otherwise False
     */
    public boolean isInside(int order, long pixel) {
        return this.index.isIntersecting(order, pixel);
    }
}
