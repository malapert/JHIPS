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
package io.github.malapert.jhips.provider;

import java.io.File;
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
    File getFile();
    
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
     * Instrument ID for which we have optical deformation.
     * @return instrument ID.
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
    
    /**
     * Unique ID of the HiPS - Format: IVOID.
     * This parameter is required.
     * @return Unique ID of the HiPS
     */
    String getCreator_did();
    
    /**
     * Unique ID of the HiPS publisher – Format: IVOID 
     * @return Unique ID of the HiPS publisher
     */
    String getPublisher_id();
    
    /**
     * Short name of original data set – Format: one word 
     * @return Data set title
     */
    String getObs_collection();
    
    /**
     * Data set title – Format: free text, one line.
     * This parameter is required.
     * @return Data set title
     */
    String getObs_title();
    
    /**
     * Data set description – Format: free text, longer free text description of
     * the dataset.
     * This parameter is recommended.
     * @return Data set description
     */
    String getObs_description();
    
    /**
     * Acknowledgment mention.
     * @return 
     */
    String getObs_ack();
    
    /**
     * Provenance of the original data – Format: free text.
     * This parameter is recommended.
     * @return Provenance of the original data
     */
    String getProv_progenitor();
    
    /**
     * Bibliographic reference
     * @return Bibliographic reference
     */
    String getBib_reference();
    
    /**
     * URL to bibliographic reference
     * @return URL to bibliographic reference
     */
    String getBib_reference_url();
    
    /**
     * Copyright mention associated to the original data – Format: free text
     * @return Copyright mention associated to the original data
     */
    String getObs_copyright();
    
    /**
     * Copyright mention associated to the original data.
     * @return Copyright mention associated to the original data
     */
    String getObs_copyright_url();
    
    /**
     * Copyright mention associated to the HiPS – Format: free text
     * @return Copyright mention associated to the HiPS
     */
    String getHips_copyright();
    
    /**
     * General wavelength – Format: word: "Radio" | "Millimeter" | "Infrared" | 
     * "Optical" | "UV" | "EUV" | "X-ray" | "Gamma-ray".
     * This parameter is recommended.
     * @return General wavelength
     */
    String getObs_regime();
    
    /**
     * UCD describing data contents.
     * @return 
     */
    String getData_ucd();
    
    /**
     * Number of HiPS version – Format: 1.4
     * @return Number of HiPS version
     */
    String getVersion();

    /**
     * Name and version of the tool used for building the HiPS – Format: free text
     * @return Name and version of the tool used for building the HiPS
     */
    String getHips_builder();

    /**
     * HiPS first creation date - Format: ISO 8601 => YYYY-mm-ddTHH:MMZ
     * @return HiPS first creation date
     */
    String getHips_creation_date();
    
    /**
     * Institute or person who built the HiPS – Format: free text 
     * @return Institute or person who built the HiPS
     */
    String getHips_creator();
    
    /**
     * Last HiPS update date - Format: ISO 8601 => YYYY-mm-ddTHH:MMZ
     * @return  Last HiPS update date
     */
    String getHips_release_date();
    
    /**
     * HiPS status – Format: list of blank separated words (private” or “public”),
     * (“master”, “mirror”, or “partial”), (“clonable”, “unclonable” or 
     * “clonableOnce”) – Default : public master clonableOnce.
     * This parameter is required.
     * @return HiPS status
     */
    String getHips_status();
    
    //hips_estsize
    
    /**
     * Coordinate frame reference.
     * This parameter is required.
     * @return Coordinate frame reference
     */
    String getHips_frame();
    
    // hips_order
    // hips_tile_width
    // hips_tile_format
    
    /**
     * Suggested pixel display cut range (physical values) – 
     * Format: min max – Ex :10 300
     * @return Suggested pixel display cut range (physical values)
     */
    String getHips_pixel_cut();
    
    // hips_data_range
    // hips_sampling
    // hips_overlay
    // hips_skyval
    // hips_pixel_bitpix
    // data_pixel_bitpix
    
    /**
     * Type of data – Format: word “image”, “cube”, “catalog”.
     * This parameter is required.
     * @return Type of data
     */
    String getDataproduct_type();
    
    /**
     * Subtype of data – Format: word “color”, “live”.
     * This parameter is required.
     * @return Subtype of data
     */
    String getDataproduct_subtype();
    
    /**
     * URL to an associated progenitor HiPS.
     * @return URL to an associated progenitor HiPS
     */
    String getHips_progenitor_url();
    
    //mock_sky_fraction                                   
}
