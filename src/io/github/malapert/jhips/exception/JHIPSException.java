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

import io.github.malapert.jhips.JHIPS;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exception for JHIPS
 *
 * @author Jean-Christophe Malapert <jcmalapert@gmail.com>
 */
public class JHIPSException extends Exception {

    private static final long serialVersionUID = 2499532318568792301L;

    /**
     *
     */
    public JHIPSException() {
        super();
        Logger.getLogger(JHIPS.class.getName()).log(Level.SEVERE, "Error");

    }

    /**
     *
     * @param message
     */
    public JHIPSException(String message) {
        super(message);
        Logger.getLogger(JHIPS.class.getName()).log(Level.SEVERE, message);

    }

    /**
     *
     * @param cause
     */
    public JHIPSException(Throwable cause) {
        super(cause);
        Logger.getLogger(JHIPS.class.getName()).log(Level.SEVERE, "Error", cause);

    }

    /**
     *
     * @param message
     * @param cause
     */
    public JHIPSException(String message, Throwable cause) {
        super(message, cause);
        Logger.getLogger(JHIPS.class.getName()).log(Level.SEVERE, message, cause);

    }
}
