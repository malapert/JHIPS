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
 *****************************************************************************
 */
package fr.malapert.jhips;

import fr.malapert.jhips.exception.JHIPSException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Jean-Christophe Malapert
 */
public class MarsHips extends JHIPS {

    public MarsHips() {
        super(12);
        // remove 12 to get the full resolution but need space on disk and memory
    }
   
    public static void main(String[] args) throws MalformedURLException, JHIPSException, IOException {
        MarsHips hProj = new MarsHips();
        hProj.setOutputDirectory(new File("/tmp/hpx"));
        hProj.addFile(new URL("file:///home/malapert/Documents/MARS/PanoData/0900MR0039440010501309C00_DRCL.png"), Math.toRadians(179.85), Math.toRadians(-9.25), new double[]{Math.toRadians(7.04), Math.toRadians(7.04)});
        hProj.addFile(new URL("file:///home/malapert/Documents/MARS/PanoData/0900MR0039440020501310C00_DRCL.png"), Math.toRadians(184.386), Math.toRadians(-9.3011), new double[]{Math.toRadians(7.04), Math.toRadians(7.04)});
        hProj.addFile(new URL("file:///home/malapert/Documents/MARS/PanoData/0900MR0039440030501311C00_DRCL.png"), Math.toRadians(184.4857), Math.toRadians(-13.2926), new double[]{Math.toRadians(7.04), Math.toRadians(7.04)});
        hProj.addFile(new URL("file:///home/malapert/Documents/MARS/PanoData/0900MR0039440040501312C00_DRCL.png"), Math.toRadians(179.9457), Math.toRadians(-13.2962), new double[]{Math.toRadians(7.04), Math.toRadians(7.04)});

        hProj.process();
    }

}
