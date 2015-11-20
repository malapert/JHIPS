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
 * @author Jean-Christophe Malapert
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
     * Longitude of the camera's center in radian.
     */
    private final double cameraLongitude;
    /**
     * Latitude of the camera's center in radian.
     */
    private final double cameraLatitude;
    /**
     * Camera's FOV [along longitude, along latitude] in radian
     */
    private final double[] cameraFov;

    /**
     * Pixel's scale in rad/pixel
     */
    private double[] scale;

    /**
     * The real size of the image in pixels (without white borders)
     */
    private int[] imageRequest = new int[]{0, 0};

    private fr.malapert.jhips.algorithm.Projection.ProjectionType type;

    /**
     * creates an instance to store the file's metadata
     *
     * By default, the projection for an image is TAN.
     *
     * @param file file to store
     * @param longitude camera's center along longitude in radian
     * @param latitude camera's center along latitude in radian
     * @param cameraFov camera's fov along longitude and latitude in radian
     * @throws IOException When an error happens by reading the file
     */
    public MetadataFile(final URL file, double longitude, double latitude, double[] cameraFov) throws IOException {
        this(file, longitude, latitude, cameraFov, fr.malapert.jhips.algorithm.Projection.ProjectionType.TAN);
    }

    /**
     * creates an instance to store the file's metadata
     *
     * By default, the projection for an image is TAN.
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
        this.cameraLongitude = longitude;
        this.cameraLatitude = latitude;
        this.cameraFov = cameraFov;
        this.type = type;
        this.scale = initPixelScale(image, cameraFov);
    }

    /**
     * Pixel to remove on X and Y
     *
     * @return the number of pixels to remove in X and Y axis
     */
    private int[] computePixelsToRemove() {
        return new int[]{Math.abs(image.getWidth() - getImageRequest()[0]), Math.abs(image.getHeight()) - getImageRequest()[1]};
    }

    private double[] initPixelScale(BufferedImage image, double[] cameraFov) {
        return computeScale(image, cameraFov, new int[]{0, 0});
    }

    private double[] computeScale(BufferedImage image, double[] cameraFov, int[] pixelsToRemove) {
        double scaleX = cameraFov[0] / (image.getWidth() - pixelsToRemove[0]);
        double scaleY = cameraFov[1] / (image.getHeight() - pixelsToRemove[1]);
        return new double[]{scaleX, scaleY};
    }

    /**
     * Sets the real image request in pixels.
     *
     * @param imageRequest size of image where light is on
     */
    public void setImageRequest(int[] imageRequest) {
        this.imageRequest = imageRequest;
        int[] pixelToRemove = computePixelsToRemove();
        scale = computeScale(image, cameraFov, pixelToRemove);
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
     * Returns the scale in radian / pixel.
     *
     * @return the scale in radian / pixel
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
     * Returns the camera's center longitude in radian.
     *
     * @return the cameraLongitude in radian
     */
    public double getCameraLongitude() {
        return cameraLongitude;
    }

    /**
     * Returns the camera's center latitude in radian.
     *
     * @return the cameraLatitude
     */
    public double getCameraLatitude() {
        return cameraLatitude;
    }

    /**
     * Returns the camera's FOV in radian.
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
        return image.getWidth();
    }

    /**
     * Returns the image's height in pixels.
     *
     * @return the image's height
     */
    public int getHeight() {
        return image.getHeight();
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
        // Center of the camera in the reference pixel frame
        double[] centerCameraPixels = new double[]{0.5 * getWidth() + 0.5, 0.5 * getHeight() + 0.5};

        // Center of the camera in the reference horizontal frame
        double[] centerCameraHoriz = new double[]{getCameraLongitude(), getCameraLatitude()};

        double[] xy = fr.malapert.jhips.algorithm.Projection.unProject(centerCameraPixels, centerCameraHoriz, scale, new double[]{0, 0}, longitude, latitude, type);
        int x = (int) xy[0];
        int y = (int) (getHeight() - xy[1]);

        // Pixel to remove on X and Y axis
        int[] pixelsToRemove = computePixelsToRemove();
        pixelsToRemove[0] = (pixelsToRemove[0]<0 || pixelsToRemove[0]>getWidth()) ? 0 : pixelsToRemove[0];
        pixelsToRemove[1] = (pixelsToRemove[1]<0 || pixelsToRemove[1]>getHeight()) ? 0 : pixelsToRemove[1];

        // The pixel to extract is outside the camera       
        if (x >= (int) (getWidth() - 0.5 * pixelsToRemove[0]) || y >= (int) (getHeight() - 0.5 * pixelsToRemove[1]) || x < 0.5 * pixelsToRemove[0] || y < 0.5 * pixelsToRemove[1]) {
            result = null;
        } else {
            try {
                result = new Color(image.getRGB(x, y));
                // TODO : A supprimer - les conditions dans le if précédent doivent résoudre ce problème
                //if (result.getRed() == 255 && result.getBlue() == 255 && result.getGreen() == 255) {
                //     Logger.getLogger(MetadataFile.class.getName()).log(Level.WARNING, "I am inside");
                //    result = null;
                //}
            } catch (ArrayIndexOutOfBoundsException ex) {
                Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, "Error when extracting values (x,y) = ({0},{1}) from file {2}", new Object[]{x,y,getFile().toString()});
                Logger.getLogger(MetadataFile.class.getName()).log(Level.SEVERE, "(width, height) = ({0},{1}) , imgRequest=({2},{3})", new Object[]{getWidth(), getHeight(), getImageRequest()[0], getImageRequest()[1]});                
            }
        }
        return result;
    }

}
