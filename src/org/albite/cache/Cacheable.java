/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.cache;

/**
 *
 * @author Albus Dumbledore
 */
public interface Cacheable {
    /**
     * Return the expected physical size of the data
     */
    public int getPhysicalSize();
}
