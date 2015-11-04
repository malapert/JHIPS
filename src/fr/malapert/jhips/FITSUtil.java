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

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsFactory;
import nom.tam.util.BufferedDataOutputStream;

/**
 * Class Utility for FITS file
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class FITSUtil {

    public static void writeByteMap(HealpixMapByte map, String filename)
            throws Exception {
        FitsFactory.setUseHierarch(true);
        FitsFactory.setUseAsciiTables(false);
        Fits f = new Fits();
        Object[] table = new Object[1];
        table[0] = map.getData();

        f.addHDU(Fits.makeHDU(table));
        BinaryTableHDU bhdu = (BinaryTableHDU) f.getHDU(1);
        bhdu.addValue("COORDSYS", "E", "Coordinate system");
        bhdu.addValue("AUTHOR", "JHIPS", "HIPS map generated by JHIPS");
        bhdu.addValue("ORIGIN", "JHIPS", "Responsible for creating the FITS file");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        GregorianCalendar gc = new GregorianCalendar();
        String dateString = sdf.format(gc.getTime());       
        gc.setTime(sdf.parse(dateString));      
        XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
        bhdu.addValue("DATE",date.toString(),"Creation date");
        bhdu.setColumnName(0, "data", "values");
        bhdu.addValue("PIXTYPE", "HEALPIX", "This is a HEALPix map");
        bhdu.addValue("NSIDE", map.getNside(), "HEALPix NSIDE parameter");
        bhdu.addValue("ORDERING", map.getScheme().toString().toUpperCase(),
                "HEALPix ordering scheme");

        FileOutputStream fos = new FileOutputStream(filename);
        BufferedDataOutputStream s = new BufferedDataOutputStream(fos);

        f.write(s);
        s.flush();
        s.close();
    }
}