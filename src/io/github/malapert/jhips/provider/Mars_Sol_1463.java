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
package io.github.malapert.jhips.provider;

import io.github.malapert.jhips.algorithm.Projection;
import io.github.malapert.jhips.exception.JHIPSException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Metadata for a whole planisphere.
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class Mars_Sol_1463 extends JHipsMetadata {
    
    private File file;
    private BufferedImage image;
    
    /**
     * Creates an instance of metadata for the planisphere.
     * @param file the whole planet
     * @throws io.github.malapert.jhips.exception.JHIPSException
     */
    public Mars_Sol_1463(final File file) throws JHIPSException {        
        try {
            this.file = file;
            this.image = ImageIO.read(this.file);
            init(Projection.ProjectionType.CAR);
        } catch (IOException ex) {
            Logger.getLogger(Mars_Sol_1463.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public File getFile() {
        return this.file;
    }

    @Override
    public double[] getHorizontalCoordinates() {
        return new double[]{-22,0};
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
    public double[] getPixelSize() {
        return new double[]{0,0};
    }

    @Override
    public String getCreator_did() {
        return "urn:mars:sol_1463";
    }

    @Override
    public String getObs_title() {
        return "Curiosity at Quela drilling site - sol 1463";
    }

    @Override
    public String getDataproduct_type() {
        return "image";
    }

    @Override
    public String getHips_release_date() {
        return Calendar.getInstance().getTime().toString();
    }

    @Override
    public String getHips_status() {
        return "private master unclonable";
    }


    @Override
    public String getHips_frame() {
        return "horizontalLocal";
    }

    @Override
    public String getObs_collection() {
        return "Curiosity";
    }

    @Override
    public String getObs_description() {
        return "Curiosity self-portrait at Quela drilling site in the Murray "
                + "Buttes area taken on sol 1463 at 13h45 local time. The mesa "
                + "on the left of Curiosity rover is 7m high while Mount Sharp "
                + "on its right is 5500m high.\n"
                + "This panorama combines 89 pictures acquired on sol 1463 plus "
                + "2 pictures taken on sol 1466 showing the hole created by "
                + "drilling at Quela site. Pictures are taken by MAHLI camera "
                + "located at the end of Curiosity's arm. Wrist motions and "
                + "turret rotations on the arm allowed MAHLI to acquire the "
                + "mosaic's component images, with the arm positioned out of "
                + "the shot in the images. The MAHLI camera can be seen in the "
                + "miniature scene reflected upside down in the parabolic mirror"
                + " at the top of the mast.\n"
                + "Courtesy of Steve Albers for generating the sky.";
    }

    @Override
    public String getObs_ack() {
        return "Data: NASA/JPL-Caltech/MSSS - Processing: Thomas Appéré - Sky by Steve Albers";
    }

    @Override
    public String getObs_copyright() {
        return "NASA/JPL-Caltech/MSSS/Thomas Appéré";
    }

    @Override
    public String getObs_copyright_url() {
        return "https://www.flickr.com/photos/thomasappere/31026023625/";
    }

    @Override
    public String getHips_copyright() {
        return "CNES";
    }

    @Override
    public String getHips_creator() {
        return "Jean-Christophe Malapert";
    }

    @Override
    public String getObs_regime() {
        return "Optical";
    }

    @Override
    public String getDataproduct_subtype() {
        return "color";
    }
    
}
