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
package io.github.malapert.jhips.exception;

/**
 * Tiles generation exception.
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class TilesGenerationException extends Error {
    private static final long serialVersionUID = -150379700198989907L;
    
    /**
     *
     */
    public TilesGenerationException() {
        super();
    }
    
    /**
     *
     * @param message
     */
    public TilesGenerationException(String message) {
        super(message);
    }
    
    /**
     *
     * @param cause
     */
    public TilesGenerationException(Throwable cause) {
        super(cause);
    }
    
    /**
     *
     * @param message
     * @param cause
     */
    public TilesGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
