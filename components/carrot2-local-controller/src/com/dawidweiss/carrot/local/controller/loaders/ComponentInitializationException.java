
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2006, Dawid Weiss, Stanisław Osiński.
 * Portions (C) Contributors listed in "carrot2.CONTRIBUTORS" file.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

package com.dawidweiss.carrot.local.controller.loaders;


/**
 * Thrown from a component could not be instantiated because
 * of some error. 
 *
 * @author Dawid Weiss
 * @version $Revision$
 */
public class ComponentInitializationException extends Exception {

    /**
     * Creates a new exception object. 
     * 
     * @param message The cause of the exception.
     */
    public ComponentInitializationException(String message) {
        super(message);
    }
    
    /**
     * Creates a new exception wrapper.
     * 
     * @param message The cause of the exception.
     * @param cause The wrapped exception.
     */
    public ComponentInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
