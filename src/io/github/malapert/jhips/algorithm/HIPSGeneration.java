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
package io.github.malapert.jhips.algorithm;

import cds.allsky.Context;
import cds.allsky.HipsGen;
import io.github.malapert.jhips.exception.TilesGenerationException;
import io.github.malapert.jhips.provider.JHipsMetadataProviderInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates HIPS tiles from a Healpix map file.
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class HIPSGeneration extends HipsGen {   

    /**
     * Creates an instance.
     */
    public HIPSGeneration() {
        super();
    }

    /**
     * Generates the files in tiles.
     * <p>
     * The processing is launched with the following parameter:
     * <ul>
     * <li>Label
     * <li>Publisher
     * <li>pixelCut=0 255
     * </ul>
     * @param in 
     * @param metadata 
     */
    public void process(String in, JHipsMetadataProviderInterface metadata) {
        this.context = new Context();
        try {
            List<String> args = new ArrayList();
            args.add("in="+in);
            args.add("-f");                      
            args.add("pixelCut="+metadata.getHips_pixel_cut());
            this.execute(args.toArray(new String[]{}));
        } catch (Exception ex) {
            Logger.getLogger(HIPSGeneration.class.getName()).log(Level.SEVERE, null, ex);
            throw new TilesGenerationException(ex);
        }
    }

}
