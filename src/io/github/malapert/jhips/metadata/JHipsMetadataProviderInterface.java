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
package io.github.malapert.jhips.metadata;

import java.net.URL;
import java.util.Map;

/**
 * Specifies the metadata interface for JHIPS
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public interface JHipsMetadataProviderInterface {
        
    /**
     * Specifies the file location for which the following metadata are described.
     * @return the file location
     */
    URL getFile();
    
    /**
     * Specifies the horizontal coordinates of the detector's center
     * <p>
     * The horizontal coordinates are expressed in radians along X and Y axis
     * @return the horizontal coordinates of the detector's center
     */
    double[] getHorizontalCoordinates();
    
    /**
     * Specifies the field of view seen by the illuminated part of the detector.
     * <p>
     * The field of view is expressed in radians along X and Y axis
     * @return the field of view seen by the illuminated part of the detector
     */
    double[] getFOV();    
    
    /**
     * Sets the sub-image in pixels along X and Y axis
     * <p>
     * In some configurations, only a part of the detector is illuminated. 
     * This part of the detector is called sub-image.
     * @param subImage the illuminated part of the detector in pixels
     */
    void setSubImageSize(int[] subImage);
    
    /**
     * Specifies the size of the sub-image in pixels along X and Y axis.
     * <p>
     * In some configurations, only a part of the detector is illuminated. 
     * This part of the detector is called sub-image.     
     * @return the size of the sub-image in pixels along X and Y axis
     */
    int[] getSubImageSize();
    
    /**
     * Specifies the size of the detector in pixels along X and Y axis.
     * @return the size of the detector in pixels along X and Y axis
     */
    int[] getDetectorSize();
    
    /**
     * Specifies the sample within a source image that corresponds
     * to the first sample in pixel coordinates in a sub-image along X and Y axis.
     * <p>
     * In some configurations, only a part of the detector is illuminated. 
     * This part of the detector is called sub-image.   
     * <p>
     * The first Sample is expressed in the pixel frame (starts to 0)
     * @return the first sample in pixel coordinates in a sub-image
     */
    int[] getFirstSample();
    
    /**
     * Specifies the instrument ID.
     * @return the instrument ID
     */
    String getInstrumentID();
    
    /**
     * Specifies the distortion coefficient for each camera.
     * <p>
     * The structure of the map is the following :<br/> 
     * INSTRUMENT_ID : x0(mm), y0(mm), k1, k2, k3
     * @return the distortion coefficient for each camera
     */
    Map<String, double[]> getDistortionCoeff();
    
    /**
     * Specifies the pixel size in mm along X and Y axis.
     * @return the pixel size in mm along X and Y axis
     */
    double[] getPixelSize();
}
