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
package io.github.malapert.jhips.metadata;

import cds.moc.HealpixMoc;
import cds.moc.MocCell;
import healpix.essentials.HealpixBase;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import io.github.malapert.jhips.exception.JHIPSException;
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
public class MetadataFile implements JHipsMetadataProviderInterface {

    /**
     * Buffered image.
     */
    private final BufferedImage image;

    /**
     * Pixel's scale in rad/pixel.
     */
    private double[] scale;

    /**
     * The spatial index of the file.
     */
    private HealpixMoc index;

    /**
     * The projection code associated to the acquisition.
     */
    private io.github.malapert.jhips.algorithm.Projection.ProjectionType type;

    /**
     * Wcs map that contains elements for computing (azimuth, elevation) <-->
     * (x,y).
     */
    private WcsNumericalMap wcs;

    /**
     * JHips metadata.
     */
    private final JHipsMetadataProviderInterface metadata;

    /**
     * Creates an instance to store the file's metadata.
     *
     * @param metadata File metadata
     * @param type projection's type of the image
     * @throws java.io.IOException
     */
    public MetadataFile(JHipsMetadataProviderInterface metadata, io.github.malapert.jhips.algorithm.Projection.ProjectionType type) throws JHIPSException {
        try {
            this.metadata = metadata;
            this.type = type;
            this.image = ImageIO.read(metadata.getFile());
            if (metadata.getSubImageSize()[0] == 0 && metadata.getSubImageSize()[1] == 0) {
                metadata.setSubImageSize(new int[]{this.image.getWidth(), this.image.getHeight()});
            }
            this.scale = initPixelScale(metadata.getSubImageSize(), metadata.getFOV());
            this.index = createIndex(metadata, this.scale);
            this.wcs = createWcs();
            try {
                this.wcs.doInit();
            } catch (JWcsException ex) {
                Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (IOException ex) {
            throw new JHIPSException(ex);
        }
    }

    /**
     * Computes a spatial index of the image in order to boost the processing.
     *
     * @param metadata JHips metadata
     * @param pixelScale pixel scale in rad/pixel along X and Y axis
     * @return the spatial index
     */
    private HealpixMoc createIndex(final JHipsMetadataProviderInterface metadata, double[] pixelScale) {
        HealpixMoc moc;
        try {
            moc = new HealpixMoc();
            HealpixBase base = new HealpixBase(1024, Scheme.NESTED);
            double radius = Math.sqrt(0.5 * metadata.getFOV()[0] * 0.5 * metadata.getFOV()[0] + 0.5 * metadata.getFOV()[1] * 0.5 * metadata.getFOV()[1]);
            double xFov = 0.5 * metadata.getDetectorSize()[0] - (metadata.getFirstSample()[0] + metadata.getSubImageSize()[0] * 0.5);
            double yFov = 0.5 * metadata.getDetectorSize()[1] - (metadata.getFirstSample()[1] + metadata.getSubImageSize()[1] * 0.5);
            double azimuthCenter = metadata.getHorizontalCoordinates()[0] + xFov * pixelScale[0];
            double elevationCenter = metadata.getHorizontalCoordinates()[1] + yFov * pixelScale[1];

            RangeSet range = base.queryDiscInclusive(new Pointing(0.5 * Math.PI - elevationCenter, azimuthCenter), radius, 128);
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
     * Returns the real image request.
     *
     * @return the real image request
     */
    public int[] getImageRequest() {
        return this.metadata.getSubImageSize();
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
     * @return the file location
     */
    @Override
    public URL getFile() {
        return this.metadata.getFile();
    }

    /**
     * Returns the elevation of the detector center in radians.
     *
     * @return the cameraLongitude in radians
     */
    public double getCameraLongitude() {
        return this.metadata.getHorizontalCoordinates()[0];
    }

    /**
     * Returns the azimuth of the detector center in radians.
     *
     * @return the cameraLatitude in radians
     */
    public double getCameraLatitude() {
        return this.metadata.getHorizontalCoordinates()[1];
    }

    /**
     * Returns the camera's FOV in radians.
     *
     * @return the cameraFov
     */
    public double[] getCameraFov() {
        return this.metadata.getFOV();
    }

    /**
     * Returns the part width of the detector that is illuminated.
     *
     * @return the number of pixels that are illuminated along X axis
     */
    public int getSubImageWidth() {
        return getSubImageSize()[0];
    }

    /**
     * Returns the part height of the detector that is illuminated.
     *
     * @return the number of pixels that are illuminated along Y axis
     */
    public int getSubImageHeight() {
        return getSubImageSize()[1];
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
        map.put(JWcs.CRPIX1, 0.5d * getDetectorSize()[0]);
        map.put(JWcs.CRPIX2, 0.5d * getDetectorSize()[1]);
        map.put(JWcs.CRVAL1, Math.toDegrees(getCameraLongitude()));
        map.put(JWcs.CRVAL2, Math.toDegrees(getCameraLatitude()));
        map.put(JWcs.CDELT1, Math.toDegrees(getScale()[0]));
        map.put(JWcs.CDELT2, Math.toDegrees(getScale()[1]));
        map.put(JWcs.NAXIS, 2);
        map.put(JWcs.NAXIS1, getDetectorSize()[0]);
        map.put(JWcs.NAXIS2, getDetectorSize()[1]);
        map.put(JWcs.CTYPE1, "RA---" + getType());
        map.put(JWcs.CTYPE1, "DEC--" + getType());
        map.put(JWcs.CROTA2, 0.0d); // No rotation for this instrument
        return new WcsNumericalMap(map);
    }

    /**
     * Applies the lens distortion on the whole detector.
     *
     * @param xy the pixel coordinates
     * @return the corrected pixel coordinates
     */
    private double[] correctedLensDistortion(double[] xy) {
        double resultX = xy[0];
        double resultY = xy[1];
        if (getDistortionCoeff() != null) {
            double xCenterLens = getSubImageWidth() * 0.5 + getFirstSample()[0];
            double yCenterLens = getSubImageHeight() * 0.5 + getFirstSample()[1];
            double x0 = getDistortionCoeff().get(getInstrumentID())[0];
            double y0 = getDistortionCoeff().get(getInstrumentID())[1];
            double dx = xy[0] * getPixelSize()[0] - (xCenterLens * getPixelSize()[0] + x0); //https://www.google.fr/url?sa=t&rct=j&q=&esrc=s&source=web&cd=3&cad=rja&uact=8&ved=0ahUKEwj79qDMoKvJAhWC1hoKHUwQA28QFggvMAI&url=http%3A%2F%2Fwww.lpi.usra.edu%2Fmeetings%2Flpsc2011%2Fpdf%2F2738.pdf&usg=AFQjCNEUtyTBIlDLuKCrcIvATjI6uqY3wQ&sig2=8pFTQTqOUhOqdnfDYgRJBA&bvm=bv.108194040,d.d2s
            double dy = xy[1] * getPixelSize()[1] - (yCenterLens * getPixelSize()[1] + y0);
            double r = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
            if (r != 0) {
                double k1 = getDistortionCoeff().get(getInstrumentID())[2];
                double k2 = getDistortionCoeff().get(getInstrumentID())[3];
                double k3 = getDistortionCoeff().get(getInstrumentID())[4];
                double dr = k1 * Math.pow(r, 3) + k2 * Math.pow(r, 5) + k3 * Math.pow(r, 7);
                double deltaX = xy[0] * dr / r;
                double deltaY = xy[1] * dr / r;
                resultX += deltaX;
                resultY += deltaY;

            }
        }
        return new double[]{resultX, resultY};
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
            double[] xy = this.wcs.wcs2pix(Math.toDegrees(longitude), Math.toDegrees(latitude));
            xy = correctedLensDistortion(xy);
            int x = (int) xy[0] - getFirstSample()[0];
            int y = (int) xy[1] - getFirstSample()[1];
            x = (int) (x + 0.5 * (image.getWidth() - getSubImageWidth()));
            y = (int) (image.getHeight() - (y + 0.5 * (image.getWidth() - getSubImageWidth())));

            int xmin = (int) ((this.image.getWidth() > getSubImageWidth())
                    ? Math.ceil((this.image.getWidth() - getSubImageWidth()) * 0.5)
                    : 0);
            int xmax = (int) ((this.image.getWidth() > getSubImageWidth())
                    ? Math.floor(getSubImageWidth() - (this.image.getWidth() - getSubImageWidth()) * 0.5)
                    : this.image.getWidth());
            int ymin = (int) ((this.image.getHeight() > getSubImageHeight())
                    ? Math.ceil((this.image.getHeight() - getSubImageHeight())) * 0.5
                    : 0);
            int ymax = (int) ((this.image.getHeight() > getSubImageHeight())
                    ? Math.floor(getSubImageWidth() - (this.image.getHeight() - getSubImageHeight()) * 0.5)
                    : this.image.getHeight());

            // The pixel to extract is outside the camera
            if (x >= xmax || y >= ymax || x < xmin || y < ymin) {
                result = null;
            } else {
                try {
                    result = new Color(image.getRGB(x, y));
                } catch (ArrayIndexOutOfBoundsException ex) {
                    Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, "Error when extracting values (x,y) = ({0},{1}) from file {2}", new Object[]{x, y, getFile().toString()});
                    Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, "(width, height) = ({0},{1}) , imgRequest=({2},{3})", new Object[]{getSubImageWidth(), getSubImageHeight(), getImageRequest()[0], getImageRequest()[1]});
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

    @Override
    public double[] getHorizontalCoordinates() {
        return this.metadata.getHorizontalCoordinates();
    }

    @Override
    public double[] getFOV() {
        return this.metadata.getFOV();
    }

    @Override
    public void setSubImageSize(int[] subImage) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int[] getSubImageSize() {
        return this.metadata.getSubImageSize();
    }

    @Override
    public int[] getFirstSample() {
        return this.metadata.getFirstSample();
    }

    @Override
    public String getInstrumentID() {
        return this.metadata.getInstrumentID();
    }

    @Override
    public Map<String, double[]> getDistortionCoeff() {
        return this.metadata.getDistortionCoeff();
    }

    @Override
    public double[] getPixelSize() {
        return this.metadata.getPixelSize();
    }

    @Override
    public int[] getDetectorSize() {
        return this.metadata.getDetectorSize();
    }
}
