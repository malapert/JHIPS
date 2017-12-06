/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.malapert.jhips.provider;

import cds.moc.HealpixMoc;
import cds.moc.MocCell;
import healpix.essentials.HealpixBase;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import io.github.malapert.jhips.exception.JHIPSException;
import io.github.malapert.jhips.metadata.MetadataFile;
import io.github.malapert.jwcs.AbstractJWcs;
import io.github.malapert.jwcs.WcsNumericalMap;
import io.github.malapert.jwcs.proj.exception.JWcsException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Jean-Christophe Malapert
 */
public abstract class JHipsMetadata implements JHipsMetadataProviderInterface {
    
    /**
     * Buffered image.
     */
    private BufferedImage image;

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

    public void init(io.github.malapert.jhips.algorithm.Projection.ProjectionType type) throws JHIPSException {
        try {
            this.type = type;
            this.image = ImageIO.read(this.getFile());
            if (this.getSubImageSize()[0] == 0 && this.getSubImageSize()[1] == 0) {
                this.setSubImageSize(new int[]{this.image.getWidth(), this.image.getHeight()});
            }
            this.scale = initPixelScale(this.getSubImageSize(), this.getFOV());
            this.index = createIndex(this.scale);
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
     * Valid solution in the pixel range in the graphic reference frame.
     * <p>
     * [xmin, xmax, ymin, ymax]
     */
    private final int[] validatedPixelRange = new int[4];
    
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
    private HealpixMoc createIndex(double[] pixelScale) {
        HealpixMoc moc;
        try {
            moc = new HealpixMoc();
            HealpixBase base = new HealpixBase(1024, Scheme.NESTED);
            double radius = Math.sqrt(0.5 * this.getFOV()[0] * 0.5 * this.getFOV()[0] + 0.5 * this.getFOV()[1] * 0.5 * this.getFOV()[1]);
            double xFov = 0.5 * this.getDetectorSize()[0] - (this.getFirstSample()[0] + this.getSubImageSize()[0] * 0.5);
            double yFov = 0.5 * this.getDetectorSize()[1] - (this.getFirstSample()[1] + this.getSubImageSize()[1] * 0.5);
            double azimuthCenter = this.getHorizontalCoordinates()[0] + xFov * pixelScale[0];
            double elevationCenter = this.getHorizontalCoordinates()[1] + yFov * pixelScale[1];

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
        return this.getSubImageSize();
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
     * Returns the elevation of the detector center in radians.
     *
     * @return the cameraLongitude in radians
     */
    public double getCameraLongitude() {
        return this.getHorizontalCoordinates()[0];
    }

    /**
     * Returns the azimuth of the detector center in radians.
     *
     * @return the cameraLatitude in radians
     */
    public double getCameraLatitude() {
        return this.getHorizontalCoordinates()[1];
    }

    /**
     * Returns the camera's FOV in radians.
     *
     * @return the cameraFov
     */
    public double[] getCameraFov() {
        return this.getFOV();
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
            y = (int) (image.getHeight() - 1 - (y + 0.5 * (image.getHeight() - getSubImageHeight())));

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
    public String getInstrumentID() {
        return null;
    }
    
    @Override
    public Map<String, double[]> getDistortionCoeff() {
        return null;
    }    

    @Override
    public String getObs_collection() {
        return null;
    }

    @Override
    public String getObs_description() {
        return null;
    }

    @Override
    public String getObs_ack() {
        return null;
    }

    @Override
    public String getObs_copyright() {
        return null;
    }

    @Override
    public String getObs_copyright_url() {
        return null;
    }

    @Override
    public String getHips_copyright() {
        return "CNES";
    }

    @Override
    public String getHips_creator() {
        return "CNES (Jean-Christophe Malapert)";
    }

    @Override
    public String getPublisher_id() {
        return null;
    }

    @Override
    public String getProv_progenitor() {
        return null;
    }

    @Override
    public String getBib_reference() {
        return null;
    }

    @Override
    public String getBib_reference_url() {
        return null;
    }

    @Override
    public String getObs_regime() {
        return null;
    }

    @Override
    public String getData_ucd() {
        return null;
    }

    @Override
    public String getHips_pixel_cut() {
        return "0 255";
    }  
    
    @Override
    public String getHips_progenitor_url() {
        return null;
    }        

    @Override
    public String getVersion() {
        return "1.4";
    }        

    @Override
    public String getHips_builder() {
        return "JHIPS with HipsGen";
    }

    @Override
    public String getHips_creation_date() {
        return Calendar.getInstance().getTime().toString();
    }            

}
