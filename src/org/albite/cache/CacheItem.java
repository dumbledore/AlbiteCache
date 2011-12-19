/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.cache;

/**
 *
 * @author Albus Dumbledore
 */
public abstract class CacheItem {

    CacheItem previous;
    CacheItem next;

    Object key;
    abstract Cacheable getValue();

    /**
     * This might need to be overriden if getting the physical size
     * might need the value to be fetched from a disk or something
     */
    int getValuePhysicalSize() {
        return getValue().getPhysicalSize();
    }
}
