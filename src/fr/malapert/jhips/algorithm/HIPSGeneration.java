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
package fr.malapert.jhips.algorithm;

import cds.allsky.Context;
import cds.allsky.HipsGen;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Geneates HIPS from a Healpix map file.
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class HIPSGeneration extends HipsGen {
    
    private String label = "";
    private String publisher = "JC Malapert (jcmalapert@gmail.com)";    

    public HIPSGeneration() {
        super();
    }

    public void process(String in) {
        this.context = new Context();
        try {
            List<String> args = new ArrayList();
            args.add("in="+in);
            args.add("-f");
            if(!getLabel().isEmpty()) {
                args.add("label="+getLabel());
            }
            if(!getPublisher().isEmpty()) {
                args.add("publisher="+getPublisher());
            }
            args.add("pixelCut=0 255");
            this.execute(args.toArray(new String[]{}));
        } catch (Exception ex) {
            Logger.getLogger(HIPSGeneration.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @return the label
     */
    private String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    private void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the publisher
     */
    private String getPublisher() {
        return publisher;
    }

    /**
     * @param publisher the publisher to set
     */
    private void setPublisher(String publisher) {
        this.publisher = publisher;
    }

}
