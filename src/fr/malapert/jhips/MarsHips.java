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

import fr.malapert.jhips.exception.JHIPSException;
import fr.malapert.jhips.exception.JHIPSOutputImageException;
import healpix.essentials.Pointing;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Example with the following assumptions
 * - The camera'center has the position (0,0) in the azimuthal referenceFrame
 * - the camera'fov is 15x15 degrees
 * @author Jean-Christophe Malapert
 */
public class MarsHips extends JHIPS {
    
    // Default camera FOV along X and Y camera's axis
    private static final double FOV_CAMERA_ALONG_AZIMUTH = 15 * Math.PI / 180; 
    private static final double FOV_CAMERA_ALONG_ELEVATION = 15 * Math.PI / 180; 
    
    // Default camera position in the azimuthal frame in radians
    private static final double[] DEFAULT_CAMERA_AZ = new double[] {0,0};        
    
    // The camera's center in the azimuthal frame in radians
    private double[] cameraCoordInAz = DEFAULT_CAMERA_AZ;
     
    public MarsHips() {
        super(12, FOV_CAMERA_ALONG_AZIMUTH, FOV_CAMERA_ALONG_ELEVATION);
        // remove 12 to get the full resolution but need space on disk and memory
    }
        
    /**
     * Returns the center of the camera in azimuthal frame.
     * @return the cameraCoordInAz (azimuth, elevation) in radians
     */
    public double[] getCameraCoordInAz() {
        return cameraCoordInAz;
    }

    /**
     * Sets the center of the camera in azimuthal framr
     * @param cameraCoordInAz the cameraCoordInAz to set (azimuth, elevation) in radians
     */
    public void setCameraCoordInAz(double[] cameraCoordInAz) {
        this.cameraCoordInAz = cameraCoordInAz;
    }    
    
    @Override
    protected int[] getPixelValueFromSphericalCoordinates(Pointing pt, BufferedImage img) throws JHIPSOutputImageException {
        // converts spherical coordinate to azimutal coordinates
        double azimuth = pt.phi;
        double elevation = Math.PI * 0.5 - pt.theta;
        
        // Center of the camera in the reference pixel frame
        double[] centerCameraPixels = new double[]{0.5 * img.getWidth()+ 0.5, 0.5 * img.getHeight()+ 0.5};

        // Find the angular distance between the pt and camera's center projected on each axis (azimuth, elevation)
        double variationAzimuth = azimuth - getCameraCoordInAz()[0];
        if (variationAzimuth > Math.PI) {
            variationAzimuth = -2*Math.PI + variationAzimuth;
        }
        double variationElev = elevation - getCameraCoordInAz()[1];        
        double[] variationAzCoord = new double[] {variationAzimuth, variationElev}; 
        
        // Find the scale per pixel 
        double[] scale = new double[] {FOV_CAMERA_ALONG_AZIMUTH/img.getWidth(), FOV_CAMERA_ALONG_ELEVATION/img.getHeight()};
        
        // Find the number of pixel to move from the camera's center
        double[] pixelToMoveAlongXY = new double[]{variationAzCoord[0] / scale[0], variationAzCoord[1] / scale[1]};
                
        
        int x = (int) (-pixelToMoveAlongXY[0] + centerCameraPixels[0]);
        int y = (int) (-pixelToMoveAlongXY[1] + centerCameraPixels[1]);
        
        // The pixel to extract is outside the camera
        if(x>=img.getWidth() || y>= img.getHeight() || x<0 || y<0) {
           throw new JHIPSOutputImageException();
        }
        return new int[]{x, y};
    }    
    
    public static void main(String[] args) throws MalformedURLException, JHIPSException {
        MarsHips hProj = new MarsHips();
        hProj.setOutputDirectory(new File("/tmp/hpx"));
        hProj.addFile(new URL("file:///home/malapert/Documents/MARS/JHIPS/jhips_in/0899MR0039410010501279C00_DRCL.png"));
        hProj.process();       
    }        
    
}
