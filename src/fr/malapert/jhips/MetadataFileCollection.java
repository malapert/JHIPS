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

import healpix.essentials.HealpixBase;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores all metadata of each file to project.
 *
 * @author Jean-Christophe Malapert
 */
public class MetadataFileCollection {

    /**
     * List of files to project.
     */
    private List<MetadataFile> metadataFiles;
    /**
     * Pixel's scale in radian/pixel along width x height
     */
    private double[] scale;

    /**
     * Creates an instance of metadata collection.
     */
    public MetadataFileCollection() {
        this(new ArrayList<MetadataFile>());
    }

    /**
     * Creates an instance of metadata collection based on a list of files.
     *
     * @param files files to project
     */
    public MetadataFileCollection(final List<MetadataFile> files) {
        setMetadataFiles(files);
    }

    /**
     * Returns the files to project.
     *
     * @return the metadataFiles
     */
    public List<MetadataFile> getMetadataFiles() {
        return metadataFiles;
    }

    /**
     * Sets the files to project.
     *
     * @param files the metadataFiles to set
     */
    public final void setMetadataFiles(final List<MetadataFile> files) {
        this.metadataFiles = files;
        scale = computeHighestResolution(files);
    }

    /**
     * Adds a file to project.
     *
     * @param file
     */
    public void addMetadataFile(final MetadataFile file) {
        getMetadataFiles().add(file);
        scale = computeHighestResolution(scale, file);
    }

    /**
     * Returns the pixel's scale along width x height in radian/pixel
     *
     * @return the highestResolution
     */
    public double[] getPixelScale() {
        return scale;
    }

    /**
     * Returns the pixel's scale based on the computation of pixel's diagonal.
     *
     * @return the scale in radian/pixel
     */
    public double getScale() {
        return Math.sqrt((getPixelScale()[0] * getPixelScale()[0] + getPixelScale()[1] * getPixelScale()[1]));
    }

    /**
     * Returns the number of files to project.
     *
     * @return
     */
    public int size() {
        return metadataFiles.size();
    }

    /**
     * Computes the highest resolution along width and height from the list of
     * files
     *
     * @param files files in which the highest resolution is searched
     * @return The highest resolution along width and height in radian/pixel
     */
    private double[] computeHighestResolution(final List<MetadataFile> files) {
        double[] highestScale = new double[]{Double.MAX_VALUE, Double.MAX_VALUE};
        for (MetadataFile file : files) {
            highestScale = computeHighestResolution(highestScale, file);
        }
        return highestScale;
    }

    /**
     * Computes the highest resolution based on the computed scale and a new
     * scale.
     *
     * @param scale the computed scale
     * @param file new scale
     * @return the highest scale
     */
    private double[] computeHighestResolution(double[] scale, final MetadataFile file) {
        double scaleX = file.getScale()[0];
        double scaleY = file.getScale()[1];
        scale[0] = (scaleX < scale[0]) ? scaleX : scale[0];
        scale[1] = (scaleY < scale[1]) ? scaleY : scale[1];
        return scale;
    }

    /**
     * Returns the RGB color from a pixel based on a longitude and latitude in
     * radian.
     *
     * @param longitude longitude in radian
     * @param latitude latitude in radian
     * @return the RGB color
     */
    public Color getRGB(double longitude, double latitude) {
        int alpha = 0;
        int red = 0;
        int green = 0;
        int blue = 0;
        int match = 0;

        for (MetadataFile file : getMetadataFiles()) {
            Color c = file.getRGB(longitude, latitude);
            if (c != null) {
                alpha += c.getAlpha();
                red += c.getRed();
                green += c.getGreen();
                blue += c.getBlue();
                match++;
                //break;
            }
        }
        return (match == 0) ? null : new Color(red / match, green / match, blue / match, alpha / match);
    }

    public Color getRGB(final HealpixBase hpx, long pixel) {
        int alpha = 0;
        int red = 0;
        int green = 0;
        int blue = 0;
        int match = 0;

        for (MetadataFile file : getMetadataFiles()) {
            if (file.isInside(hpx.getOrder(), pixel)) {
                Color c = file.getRGB(hpx, pixel);
                if (c != null) {
                    alpha += c.getAlpha();
                    red += c.getRed();
                    green += c.getGreen();
                    blue += c.getBlue();
                    match++;
                    //break;
                }
            }
        }
        return (match == 0) ? null : new Color(red / match, green / match, blue / match, alpha / match);
    }
}
