/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.cache;

/**
 *
 * @author Albus Dumbledore
 */
public class PLRUMemCache extends PLRUCache {
    public PLRUMemCache(
            final Cache subCache,
            final int   targetPhysicalSize,
            final int   physicalCapacity,
            final int   targetElementSize,
            final int   elementCapacity) {

        if (targetPhysicalSize <= 0
                || physicalCapacity <= 0
                || targetElementSize <= 0
                || elementCapacity <= 0) {

            throw new IllegalArgumentException("sizes sould be positive");
        }

        if (targetPhysicalSize > physicalCapacity
                || targetElementSize > elementCapacity) {

            throw new IllegalArgumentException(
                    "target cannot be > the capacity");
        }

        this.subCache           = subCache;
        this.targetPhysicalSize = targetPhysicalSize;
        this.physicalCapacity   = physicalCapacity;
        this.targetElementSize  = targetElementSize;
        this.elementCapacity    = elementCapacity;
    }

    protected CacheItem createCacheItem(Object key, Cacheable value) {
        return new MemCacheItem(key, value);
    }

    private class MemCacheItem extends CacheItem {
        Cacheable value;

        MemCacheItem(final Object key, final Cacheable value) {
            this.key = key;
            this.value = value;
        }

        Cacheable getValue() {
            return value;
        }
    }
}
