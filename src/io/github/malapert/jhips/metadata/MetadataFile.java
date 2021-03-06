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

import io.github.malapert.jhips.provider.JHipsMetadataProviderInterface;
import cds.moc.HealpixMoc;
import cds.moc.MocCell;
import healpix.essentials.HealpixBase;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import io.github.malapert.jhips.exception.JHIPSException;
import io.github.malapert.jwcs.AbstractJWcs;
import io.github.malapert.jwcs.WcsNumericalMap;
import io.github.malapert.jwcs.proj.exception.JWcsException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
     * Valid solution in the pixel range in the graphic reference frame.
     * <p>
     * [xmin, xmax, ymin, ymax]
     */
    private final int[] validatedPixelRange = new int[4];

    /**
     * Creates an instance to store the file's metadata.
     *
     * @param metadata File metadata
     * @param type projection's type of the image
     * @throws io.github.malapert.jhips.exception.JHIPSException
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
            computeValidatedRangePixel();
        } catch (IOException ex) {
            throw new JHIPSException(ex);
        }
    }

    /**
     * Computed validated pixel range.
     * <p>
     * Basically, this computation remove the image borders from the solution
     */
    private void computeValidatedRangePixel() {
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
        this.validatedPixelRange[0] = xmin;
        this.validatedPixelRange[1] = xmax;
        this.validatedPixelRange[2] = ymin;
        this.validatedPixelRange[3] = ymax;
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
    public File getFile() {
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
        map.put(AbstractJWcs.CRPIX1, 0.5d * getDetectorSize()[0]);
        map.put(AbstractJWcs.CRPIX2, 0.5d * getDetectorSize()[1]);
        map.put(AbstractJWcs.CRVAL1, Math.toDegrees(getCameraLongitude()));
        map.put(AbstractJWcs.CRVAL2, Math.toDegrees(getCameraLatitude()));
        map.put(AbstractJWcs.CDELT1, -Math.toDegrees(getScale()[0]));
        map.put(AbstractJWcs.CDELT2, Math.toDegrees(getScale()[1]));
        map.put(AbstractJWcs.NAXIS, 2);
        map.put(AbstractJWcs.NAXIS1, getDetectorSize()[0]);
        map.put(AbstractJWcs.NAXIS2, getDetectorSize()[1]);
        map.put(AbstractJWcs.CTYPE1, "RA---" + getType());
        map.put(AbstractJWcs.CTYPE1, "DEC--" + getType());
        map.put(AbstractJWcs.CROTA2, 0.0d); // No rotation for this instrument
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
            // computes position in the camera reference frame
            double[] xy = this.wcs.wcs2pix(Math.toDegrees(longitude), Math.toDegrees(latitude));
            
            // applies correction on lens distortion
            //xy = correctedLensDistortion(xy);
            
            // computes position in the sub-image reference frame
            int x = (int) xy[0] - getFirstSample()[0];
            int y = (int) xy[1] - getFirstSample()[1];
            
            // computes position in the PNG reference frame
            x = (int) (x + 0.5 * (image.getWidth() - getSubImageWidth()));
            y = (int) (image.getHeight() - (y + 0.5 * (image.getHeight() - getSubImageHeight())));

            // Extracts only the physical measurement - remove the image borders
            if (x >= this.validatedPixelRange[1] || y >= this.validatedPixelRange[3] || x < this.validatedPixelRange[0] || y < this.validatedPixelRange[2]) {
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

    @Override
    public String getCreator_did() {
        return this.metadata.getCreator_did();
    }

    @Override
    public String getObs_title() {
        return this.metadata.getObs_title();
    }

    @Override
    public String getDataproduct_type() {
        return this.metadata.getDataproduct_type();
    }

    @Override
    public String getHips_release_date() {
        return this.metadata.getHips_release_date();
    }

    @Override
    public String getHips_status() {
        return this.metadata.getHips_status();
    }

    @Override
    public String getHips_frame() {
        return this.metadata.getHips_frame();
    }

    @Override
    public String getObs_collection() {
        return this.metadata.getObs_collection();
    }

    @Override
    public String getObs_description() {
        return this.metadata.getObs_description();
    }

    @Override
    public String getObs_ack() {
        return this.metadata.getObs_ack();
    }

    @Override
    public String getObs_copyright() {
        return this.metadata.getObs_copyright();
    }

    @Override
    public String getObs_copyright_url() {
        return this.metadata.getObs_copyright_url();
    }

    @Override
    public String getHips_copyright() {
        return this.metadata.getHips_copyright();
    }

    @Override
    public String getHips_creator() {
        return this.metadata.getHips_creator();
    }

    @Override
    public String getPublisher_id() {
        return this.metadata.getPublisher_id();
    }

    @Override
    public String getProv_progenitor() {
        return this.metadata.getProv_progenitor();
    }

    @Override
    public String getBib_reference() {
        return this.metadata.getBib_reference();
    }

    @Override
    public String getBib_reference_url() {
        return this.metadata.getBib_reference_url();
    }

    @Override
    public String getObs_regime() {
        return this.metadata.getObs_regime();
    }

    @Override
    public String getData_ucd() {
        return this.metadata.getData_ucd();
    }

    @Override
    public String getHips_pixel_cut() {
        return this.metadata.getHips_pixel_cut();
    }

    @Override
    public String getDataproduct_subtype() {
        return this.metadata.getDataproduct_subtype();
    }

    @Override
    public String getHips_progenitor_url() {
        return this.metadata.getHips_progenitor_url();
    }

    @Override
    public String getVersion() {
        return this.metadata.getVersion();
    }

    @Override
    public String getHips_builder() {
        return this.metadata.getHips_builder();
    }

    @Override
    public String getHips_creation_date() {
        return this.metadata.getHips_creation_date();
    }
}
