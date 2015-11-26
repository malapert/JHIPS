 /******************************************************************************
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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Metadata for a whole planisphere.
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class PlanisphereMetadataProvider implements JHipsMetadataProviderInterface {
    
    private URL file;
    private BufferedImage image;
    
    /**
     * Creates an instance of metadata for the planisphere.
     * @param file the whole planet
     */
    public PlanisphereMetadataProvider(final URL file) {
        this.file = file;
        try {
            this.image = ImageIO.read(file);
        } catch (IOException ex) {
            Logger.getLogger(PlanisphereMetadataProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public URL getFile() {
        return this.file;
    }

    @Override
    public double[] getHorizontalCoordinates() {
        return new double[]{0,0};
    }

    @Override
    public double[] getFOV() {
        return new double[]{Math.PI * 2 , Math.PI};
    }

    @Override
    public void setSubImageSize(int[] subImage) {
    }

    @Override
    public int[] getSubImageSize() {
        return new int[]{this.image.getWidth(), this.image.getHeight()};
    }

    @Override
    public int[] getDetectorSize() {
        return new int[]{this.image.getWidth(), this.image.getHeight()};
    }

    @Override
    public int[] getFirstSample() {
        return new int[]{0,0};
    }

    @Override
    public String getInstrumentID() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, double[]> getDistortionCoeff() {
        return null;
    }

    @Override
    public double[] getPixelSize() {
        return new double[]{0,0};
    }
    
}
