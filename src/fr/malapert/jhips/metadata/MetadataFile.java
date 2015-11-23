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
package fr.malapert.jhips.metadata;

import cds.moc.HealpixMoc;
import cds.moc.MocCell;
import fr.malapert.jhips.exception.ProjectionException;
import healpix.essentials.HealpixBase;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Stores the file's metadata.
 *
 * The file's metadata are the following - file's location - BufferedImage of
 * the file - (longitude,latitude) of the camera's center in radian - the
 * camera's FOV as following longitude x latitude in radian
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
    private final double cameraLongitude;
    /**
     * Latitude of the camera's center in radians.
     */
    private final double cameraLatitude;
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
     */
    private int[] imageRequest;

    /**
     * The spatial index of the file.
     */
    private HealpixMoc index;

    /**
     * The projection code associated to the acquisition.
     */
    private fr.malapert.jhips.algorithm.Projection.ProjectionType type;

    /**
     * creates an instance to store the file's metadata. By default, 
     * the projection for an image is TAN.
     *
     * @param file file to store
     * @param longitude camera's center along longitude in radians
     * @param latitude camera's center along latitude in radians
     * @param cameraFov camera's fov along longitude and latitude in radians
     * @throws IOException When an error happens by reading the file
     */
    public MetadataFile(final URL file, double longitude, double latitude, double[] cameraFov) throws IOException {
        this(file, longitude, latitude, cameraFov, fr.malapert.jhips.algorithm.Projection.ProjectionType.TAN);
    }

    /**
     * Creates an instance to store the file's metadata. By default, the 
     * projection for an image is TAN and the light reaching the camera takes all
     * the camera's detector.
     *
     * @param file file to store
     * @param longitude camera's center along longitude in radian
     * @param latitude camera's center along latitude in radian
     * @param cameraFov camera's fov along longitude and latitude in radian
     * @param type projection's type of the image
     * @throws IOException When an error happens by reading the file
     */
    public MetadataFile(final URL file, double longitude, double latitude, double[] cameraFov, fr.malapert.jhips.algorithm.Projection.ProjectionType type) throws IOException {
        this.file = file;
        this.image = ImageIO.read(file);
        this.imageRequest = new int[]{this.image.getWidth(), this.image.getHeight()};
        this.cameraLongitude = longitude;
        this.cameraLatitude = latitude;
        this.cameraFov = cameraFov;
        this.type = type;
        this.scale = initPixelScale(this.imageRequest, cameraFov);
        this.index = createIndex(longitude, latitude, cameraFov);
    }

    /**
     * Computes a spatial index of the image in order to boost the processing.
     * @param longitude the azimuth in radians of the center of the image
     * @param latitude the elevation in radians of the center of the image.
     * @param cameraFOV the FOV in radians of the camera along azimuth and elevation
     * @return the spatial index
     */
    private HealpixMoc createIndex(double longitude, double latitude, double[] cameraFOV) {
        HealpixMoc moc;
        try {
            moc = new HealpixMoc();
            HealpixBase base = new HealpixBase(1024, Scheme.NESTED);
            double radius = Math.sqrt(0.5*cameraFOV[0]*0.5*cameraFOV[0] + 0.5*cameraFOV[1]*0.5*cameraFOV[1]);
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
     * @param imageRequest the real image size
     * @param cameraFov the camera's field of view
     * @return the pixel scale
     */
    private double[] initPixelScale(int[] imageRequest, double[] cameraFov) {
        return computeScale(imageRequest, cameraFov);
    }

    /**
     * Computes the scales in rad/pixels.
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
        scale = computeScale(imageRequest, cameraFov);
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
    public fr.malapert.jhips.algorithm.Projection.ProjectionType getType() {
        return this.type;
    }

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

            // Center of the camera in the reference pixel frame
            double[] centerCameraPixels = new double[]{0.5 * getWidth() + 0.5, 0.5 * getHeight() + 0.5};

            // Center of the camera in the reference horizontal frame
            double[] centerCameraHoriz = new double[]{getCameraLongitude(), getCameraLatitude()};

            double[] xy = fr.malapert.jhips.algorithm.Projection.unProject(centerCameraPixels, centerCameraHoriz, scale, new double[]{0, 0}, longitude, latitude, type);
            int x = (int) xy[0];
            //int y = (int) -(getHeight()- xy[1]);
            int y = (int) (getHeight() - xy[1]);
            
            int xmin = (int) ((this.image.getWidth() > getWidth()) 
                    ? (this.image.getWidth() - getWidth())*0.5:0);
            int xmax = (int) ((this.image.getWidth() > getWidth()) 
                    ? getWidth()- (this.image.getWidth() - getWidth())*0.5:getWidth());
            int ymin = (int) ((this.image.getHeight() > getHeight()) 
                    ? (this.image.getHeight() - getHeight())*0.5:0);
            int ymax = (int) ((this.image.getHeight() > getHeight()) 
                    ? getWidth()- (this.image.getHeight() - getHeight())*0.5:getHeight());            
            

            // The pixel to extract is outside the camera
            if (x >= xmax || y >= ymax || x < xmin || y < ymin) {
                result = null;
            } else {
                try {
                    result = new Color(image.getRGB(x, y));
                } catch (ArrayIndexOutOfBoundsException ex) {
                    Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, "Error when extracting values (x,y) = ({0},{1}) from file {2}", new Object[]{x, y, getFile().toString()});
                    Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, "(width, height) = ({0},{1}) , imgRequest=({2},{3})", new Object[]{getWidth(), getHeight(), getImageRequest()[0], getImageRequest()[1]});
                }
            }

        } catch (ProjectionException ex) {
            Logger.getLogger(MetadataFile.class.getName()).log(Level.FINEST, null, ex);
        }
        return result;
    }

    /**
     * Returns the RGB color from a pixel based on a Healpix pixel.
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
     * Checks whether a pixel is inside the index.
     * @param order mesh resolution
     * @param pixel pixel to test
     * @return True when the pixel in inside the index otherwise False
     */
    public boolean isInside(int order, long pixel) {
        return this.index.isIntersecting(order, pixel);
    }
}
