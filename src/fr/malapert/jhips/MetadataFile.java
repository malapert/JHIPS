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
import javax.imageio.ImageIO;

/**
 * Stores the file's metadata.
 * 
 * The file's metadata are the following
 * - file's location
 * - BufferedImage of the file
 * - (longitude,latitude) of the camera's center in radian
 * - the camera's FOV as following longitude x latitude in radian
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
     * creates an instance to store the file's metadata
     * @param file file to store
     * @param longitude camera's center along longitude in radian
     * @param latitude camera's center along latitude in radian
     * @param cameraFov camera's fov along longitude and latitude in radian
     * @throws IOException When an error happens by reading the file
     */
    public MetadataFile(final URL file, double longitude, double latitude, double[] cameraFov) throws IOException {
        this.file = file;
        this.image = ImageIO.read(file);
        this.cameraLongitude = longitude;
        this.cameraLatitude = latitude;
        this.cameraFov = cameraFov;
    }

    /**
     * Returns the file's location.
     * @return the file
     */
    public URL getFile() {
        return file;
    }


    /**
     * Returns the camera's center longitude in radian.
     * @return the cameraLongitude in radian
     */
    public double getCameraLongitude() {
        return cameraLongitude;
    }

    /**
     * Returns the camera's center latitude in radian.
     * @return the cameraLatitude
     */
    public double getCameraLatitude() {
        return cameraLatitude;
    }

    /**
     * Returns the camera's FOV in radian.
     * @return the cameraFov
     */
    public double[] getCameraFov() {
        return cameraFov;
    }
    
    /**
     * Returns the image's width in pixels.
     * @return the image's width
     */
    public int getWidth() {
        return image.getWidth();
    }
    
    /**
     * Returns the image's height in pixels.
     * @return the image's height
     */
    public int getHeight() {
        return image.getHeight();
    }
    
    /**
     * Returns the RGB color from a pixel based on a longitude and latitude.
     * @param longitude longitude in radians
     * @param latitude latitude in radians
     * @return the RGB color
     */
    public Color getRGB(double longitude, double latitude) {        
        Color result;      
        // Center of the camera in the reference pixel frame
        double[] centerCameraPixels = new double[]{0.5 * getWidth()+ 0.5, 0.5 * getHeight()+ 0.5};

        // Find the angular distance between the pt and camera's center projected on each axis (azimuth, elevation)
        double variationLongitude = longitude - getCameraLongitude();
        if (variationLongitude > Math.PI) {
            variationLongitude = -2*Math.PI + variationLongitude;
        }
        
        double variationLatitude = latitude - getCameraLatitude();        
        double[] variationCoord = new double[] {variationLongitude, variationLatitude}; 
        
        // Find the scale per pixel 
        double[] scale = new double[] {getCameraFov()[0]/getWidth(), getCameraFov()[1]/getHeight()};
        
        // Find the number of pixel to move from the camera's center
        double[] pixelToMoveAlongXY = new double[]{variationCoord[0] / scale[0], variationCoord[1] / scale[1]};
                
        
        int x = (int) (pixelToMoveAlongXY[0] + centerCameraPixels[0]);
        int y = (int) (-pixelToMoveAlongXY[1] + centerCameraPixels[1]);
        
        // The pixel to extract is outside the camera
        if(x>=getWidth() || y>= getHeight() || x<0 || y<0) {
           result = null;
        } else {
           result = new Color(image.getRGB(x, y));
        }
        return result;
    }
        
}
