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

import io.github.malapert.jhips.JHIPS;
import healpix.essentials.*;
import java.util.Arrays;

/**
 * Class representing a full HEALPix map containing byte values.
 */
public class HealpixMapByte extends HealpixBase {

    private byte[] data;

    /**
     * Creates a Healpix map with nside=1 and a nested scheme.
     * @throws Exception
     */
    public HealpixMapByte() throws Exception {
        this(1, Scheme.NESTED);
    }

    /**
     * Creates a Healpix map to store the result.
     * @param nside_in Healpix nside
     * @param scheme_in Healpix scheme
     * @throws Exception
     */
    public HealpixMapByte(long nside_in, Scheme scheme_in) throws Exception {
        super(nside_in, scheme_in);
        HealpixUtils.check(nside <= (1 << JHIPS.ORDER), "resolution too high");
        data = new byte[(int) getNpix()];
    }

    /**
     * Creates a Healpix map to store the result.
     * <p>
     * The nside is comuted automatically from data.
     * @param data_in data to store
     * @param scheme_in Healpix scheme
     * @throws Exception
     */
    public HealpixMapByte(byte[] data_in, Scheme scheme_in) throws Exception {
        super(npix2Nside(data_in.length), scheme_in);
        HealpixUtils.check(nside <= (1 << JHIPS.ORDER), "resolution too high");
        data = data_in;
    }

    /**
     * Adjusts the object to nside_in.
     *
     * @param nside_in the new Nside parameter
     * @throws java.lang.Exception
     */
    @Override
    public void setNside(long nside_in) throws Exception {
        if (nside_in != nside) {
            super.setNside(nside_in);
            HealpixUtils.check(nside <= (1 << JHIPS.ORDER), "resolution too high");
            data = new byte[(int) getNpix()];
        }
    }

    /**
     * Adjusts the object to nside_in and scheme_in.
     *
     * @param nside_in the new Nside parameter
     * @param scheme_in the new ordering scheme
     * @throws java.lang.Exception
     */
    @Override
    public void setNsideAndScheme(long nside_in, Scheme scheme_in)
            throws Exception {
        super.setNsideAndScheme(nside_in, scheme_in);
        HealpixUtils.check(nside <= (1 << JHIPS.ORDER), "resolution too high");
        data = new byte[(int) getNpix()];
    }

    /**
     * Adjusts the object to scheme_in, and sets pixel data to data_in.
     *
     * @param data_in pixel data; must have a valid length (12*nside^2)
     * @param scheme_in the new ordering scheme
     * @throws java.lang.Exception
     */
    public void setDataAndScheme(byte[] data_in, Scheme scheme_in)
            throws Exception {
        super.setNsideAndScheme(npix2Nside(data_in.length), scheme_in);
        data = data_in;
    }

    /**
     * Sets all map pixel to a specific value.
     *
     * @param val pixel value to use
     */
    public void fill(byte val) {
        Arrays.fill(data, val);
    }

    /**
     * Converts the map from NESTED to RING scheme or vice versa. This operation
     * is done in-place, i.e. it does not require additional memory.
     * @throws java.lang.Exception
     */
    public void swapScheme() throws Exception {
        HealpixUtils.check((order >= 0) && (order <= JHIPS.ORDER),
                "swapping not supported for this Nside");
        for (int m = 0; m < swap_cycle[order].length; ++m) {
            int istart = swap_cycle[order][m];

            byte pixbuf = data[istart];
            long iold = istart,
                    inew = (scheme == Scheme.RING) ? nest2ring(istart) : ring2nest(istart);
            while (inew != istart) {
                data[(int) iold] = data[(int) inew];
                iold = inew;
                inew = (scheme == Scheme.RING) ? nest2ring(inew) : ring2nest(inew);
            }
            data[(int) iold] = pixbuf;
        }
        scheme = (scheme == Scheme.RING) ? Scheme.NESTED : Scheme.RING;
    }

    /**
     * Returns the value of the pixel with a given index.
     *
     * @param ipix index of the requested pixel
     * @return pixel value
     */
    public float getPixel(int ipix) {
        return data[ipix];
    }

    /**
     * Returns the value of the pixel with a given index.
     *
     * @param ipix index of the requested pixel
     * @return pixel value
     */
    public float getPixel(long ipix) {
        return data[(int) ipix];
    }

    /**
     * Sets the value of a specific pixel.
     *
     * @param ipix index of the pixel
     * @param val new value for the pixel
     */
    public void setPixel(int ipix, byte val) {
        data[ipix] = val;
    }

    /**
     * Sets the value of a specific pixel.
     *
     * @param ipix index of the pixel
     * @param val new value for the pixel
     */
    public void setPixel(long ipix, byte val) {
        data[(int)ipix] = val;
    }

    /**
     * Returns the array containing all map pixels.
     *
     * @return the map array
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Imports the map "orig" to this object, adjusting pixel ordering.
     *
     * @param orig map to import
     * @throws java.lang.Exception
     */
    public void importNograde(HealpixMapByte orig) throws Exception {
        HealpixUtils.check(nside == orig.nside,
                "importNograde: maps have different nside");
        if (orig.scheme == scheme) {
            System.arraycopy(orig.data, 0, data, 0, (int) npix);
        } else {
            for (int m = 0; m < npix; ++m) {
                data[scheme == Scheme.NESTED ? (int) ring2nest(m) : (int) nest2ring(m)]
                        = orig.data[m];
            }
        }
    }

    /**
     * Imports the map "orig" to this object, adjusting pixel ordering and
     * increasing resolution.
     *
     * @param orig map to import
     * @throws java.lang.Exception
     */
    public void importUpgrade(HealpixMapByte orig) throws Exception {
        HealpixUtils.check(nside > orig.nside, "importUpgrade: this is no upgrade");
        int fact = (int) (nside / orig.nside);
        HealpixUtils.check(nside == orig.nside * fact,
                "the larger Nside must be a multiple of the smaller one");

        for (int m = 0; m < orig.npix; ++m) {
            Xyf xyf = orig.pix2xyf(m);
            int x = xyf.ix, y = xyf.iy, f = xyf.face;
            for (int j = fact * y; j < fact * (y + 1); ++j) {
                for (int i = fact * x; i < fact * (x + 1); ++i) {
                    long mypix = xyf2pix(i, j, f);
                    data[(int) mypix] = orig.data[m];
                }
            }
        }
    }

    /**
     * Imports the map "orig" to this object, adjusting pixel ordering and
     * reducing resolution.
     *
     * @param orig map to import
     * @param pessimistic if true, set a pixel to undefined if at least one the
     * original subpixels was undefined; otherwise only set it to undefined if
     * all original subpixels were undefined.
     * @throws java.lang.Exception
     */
    public void importDegrade(HealpixMapByte orig, boolean pessimistic)
            throws Exception {
        HealpixUtils.check(nside < orig.nside, "importDegrade: this is no degrade");
        int fact = (int) (orig.nside / nside);
        HealpixUtils.check(orig.nside == nside * fact,
                "the larger Nside must be a multiple of the smaller one");

        int minhits = pessimistic ? fact * fact : 1;
        for (int m = 0; m < npix; ++m) {
            Xyf xyf = pix2xyf(m);
            int x = xyf.ix, y = xyf.iy, f = xyf.face;
            int hits = 0;
            double sum = 0;
            for (int j = fact * y; j < fact * (y + 1); ++j) {
                for (int i = fact * x; i < fact * (x + 1); ++i) {
                    int opix = (int) orig.xyf2pix(i, j, f);
                    if (!HealpixUtils.approx(orig.data[opix], 0, 1e-5)) {
                        ++hits;
                        sum += orig.data[opix];
                    }
                }
            }
            data[m] = (hits < minhits) ? 0 : (byte) (sum / hits);
        }
    }

    /**
     * Imports the map "orig" to this object, adjusting pixel ordering and
     * resolution if necessary.
     *
     * @param orig map to import
     * @param pessimistic only used when resolution must be reduced: if true,
     * set a pixel to undefined if at least one the original subpixels was
     * undefined; otherwise only set it to undefined if all original subpixels
     * were undefined.
     * @throws java.lang.Exception
     */
    public void importGeneral(HealpixMapByte orig, boolean pessimistic)
            throws Exception {
        if (orig.nside == nside) {
            importNograde(orig);
        } else if (orig.nside < nside) // upgrading
        {
            importUpgrade(orig);
        } else {
            importDegrade(orig, pessimistic);
        }
    }
}
