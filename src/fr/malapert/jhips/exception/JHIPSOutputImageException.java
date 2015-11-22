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
package fr.malapert.jhips.exception;

/**
 * Exception when the pixel that has been queried is outside the image
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class JHIPSOutputImageException extends Exception {
    private static final long serialVersionUID = 2499532318568792301L;
    
    public JHIPSOutputImageException() {
        super();
    }
    
    public JHIPSOutputImageException(String message) {
        super(message);
    }
    
    public JHIPSOutputImageException(Throwable cause) {
        super(cause);
    }  
    
    public JHIPSOutputImageException(String message, Throwable cause) {
        super(message, cause);
    }    
}
