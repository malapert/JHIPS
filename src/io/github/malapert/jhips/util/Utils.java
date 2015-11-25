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
package io.github.malapert.jhips.util;

/**
 * Class Utility.
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class Utils {

    public static final String anim = "|/-\\";

    /**
     * Monitors process.
     * @param current current number of the record
     * @param total total number of records
     */
    public static void monitoring(long current, long total) {
        int percent = (int) (current * 100 / total);
        if (percent % 5 == 0) {
            System.out.print("\r" + anim.charAt((int) current % anim.length()) + " " + percent + "% done");
            System.out.flush();
        }
    }
    
    /**
     * Monitors process.
     * @param filename current filename
     * @param current current number of the record
     * @param total total number of records
     */
    public static void monitoringFile(String filename, long current, long total) {
        int percent = (int) (current * 100 / total);
        if (percent % 5 == 0) {
            System.out.print("\r Processing " + filename + " "+anim.charAt((int) current % anim.length()) + " " + percent + "% done");
            System.out.flush();
        }
    }    

}
