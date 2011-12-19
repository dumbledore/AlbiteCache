/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.cache;

import java.util.Hashtable;

/**
 *
 * @author Albus Dumbledore
 */
public abstract class Cache {

    /*
     * Internal fields
     */

    /**
     * Next level cache
     */
    Cache subCache;

    /**
     * The index used as a LUT
     */
    Hashtable index = new Hashtable();

    /**
     * head is the most recently used item
     */
    CacheItem head;
    
    /**
     * tail is from where one starts deleting items
     */
    CacheItem tail;

    /*
     * That's the amount of physical data the cacheable values
     * already take
     */
    int physicalSize;

    /*
     * This is what the maximum amount of allocated physical size should be
     * after a clean up has been done
     */
    int targetPhysicalSize;

    /*
     * That's the maximum amount of physical data the cacheable values
     * are allowed to take.
     */
    int physicalCapacity;

    /*
     * That's the amount of currently used cache elements
     */
    int elementSize;

    /*
     * This is what the maximum amount of elements should be
     * after a clean up has been done
     */
    int targetElementSize;

    /*
     * That's the maximum amount of cache elements that are allowed to be
     */
    int elementCapacity;


    /*
     * Public methods
     */
    public void put(Object key, Cacheable value)
            throws CacheException {

        if (value == null) {
            throw new IllegalArgumentException("can't add null values!");
        }

        /*
         * Invalidate it first
         */
        invalidate(key);

        /*
         * Now we are ready to add it to the top of the cache
         */
        add(key, value);
    }

    public Cacheable get(Object key) {
        CacheItem item = (CacheItem) index.get(key);
        
        if (item != null) {
            /*
             * The getValue() method might do some additional processing if
             * such is needed, e.g. reading the value data from a disk
             */
            return item.getValue();
        } else {
            /*
             * try the internal cache
             */
            return searchInternalCache(key);
        }
    }

    protected Cacheable searchInternalCache(Object key) {
        if (subCache != null) {
            Cacheable value = subCache.get(key);

            if (value != null) {
                try {
                    /*
                     * Cache it here, too.
                     */
                    add(key, value);

                    /*
                     * Remove it from the subcache as it's not needed
                     * anymore
                     */
                    subCache.invalidate(key);
                } catch (CacheException e) {
                    /*
                     * Couldn't bring it from the back cache. It still works
                     * but this is a very nasty situation
                     */
                }
            }

            return value;
        }

        return null;
    }

    /**
     * This removes the element of the cache and of any subcaches
     * there might be
     */
    public void invalidate(Object key) {
        /*
         * Invlidate the internal caches
         */
        if (subCache != null) {
            subCache.invalidate(key);
        }
        
        /*
         * Remove from index
         */
        CacheItem item = (CacheItem) index.remove(key);

        if (item == null) {
            /*
             * Nothing to invalidate
             */
            return;
        }

        /*
         * Remove from list
         */
        if (item.previous != null) {
            item.previous.next = item.next;
        }

        if (item.next != null) {
            item.next.previous = item.previous;
        }

        if (head == item) {
            head = item.next;
        }

        if (tail == item) {
            tail = item.previous;
        }

        /*
         * Update the counters
         */
        physicalSize    -= item.getValuePhysicalSize();
        elementSize     -= 1;
    }

    /**
     * This effectively swipes the whole cache.
     */
    public void invalidate() {
        /*
         * Invalidate all internal caches first
         */
        if (subCache != null) {
            subCache.invalidate();
        }

        index.clear();
        head = null;
        tail = null;
        physicalSize = 0;
        elementSize = 0;
    }

    /*
     * Implementation-dependent stuff
     */

    /**
     * This returns a cache-specific CacheItem. This is the place when
     * it's data may be additionally processed, e.g. for saving to the disk
     * or whatever.
     */
    protected abstract CacheItem createCacheItem(Object key, Cacheable value);

    /**
     * Creates a new CacheItem for the pair and adds it to top of the cache.
     * If there isn't enough space, runs GC.
     * 
     * @throws CacheException
     */
    protected void add(Object key, Cacheable value)
            throws CacheException {

        /*
         * Make room for it if necessary
         */
        makeFreeSpace(value.getPhysicalSize());

        /*
         * Now we are ready to add it to the cache
         */
        CacheItem item = createCacheItem(key, value);

        /*
         * Add it to the index
         */
        index.put(key, item);

        /*
         * Add it to the list
         */
        if (head != null) {
            head.previous = item;
        }

        item.next = head;
        head = item;

        if (tail == null) {
            tail = head;
        }

        physicalSize += value.getPhysicalSize();
        elementSize++;
    }

    /**
     * Runs GC if there's not enough physical space / element slots
     *
     * @return true if GC was run, false if GC wasn't needed
     */
    protected boolean makeFreeSpace(final int additionalSpaceNeeded)
            throws CacheException {

        final int spaceNeeded = physicalSize + additionalSpaceNeeded;

        if (spaceNeeded > physicalCapacity
                || elementSize >= elementCapacity) {

            /*
             * Note: Generally, elementSize should always be <= elementCapacity
             * thus it would've been enough to test for
             * elementSize == elementCapacity
             * However, this is a preventive measure in case an extra element
             * has slipped in.
             */

            if (additionalSpaceNeeded > physicalCapacity) {
                throw new CacheException(
                        "Cannot add a value that's bigger than the " +
                        "cache itself."
                );
            }

            if (additionalSpaceNeeded > targetPhysicalSize) {
                /*
                 * The new object needs loads of space. Trash the caches
                 */
                invalidate();
                return true;
            }

            final int spaceLimit = targetPhysicalSize - additionalSpaceNeeded;

            /*
             * Start cleaning up from the tail
             */
            CacheItem current = tail;

            while(current != null) {

                /*
                 * This is always valid to do
                 */
                current.next = null;

                if (physicalSize <= spaceLimit
                        && elementSize < targetElementSize) {
                    /*
                     * Note: it should be a strict inequality because
                     * we need to have one element LESS than the max, because
                     * one would be adding a new item just after this method
                     */

                    /*
                     * Freed enough space, no need to delete more
                     */
                    break;
                }

                if (remove(current)) {
                    /*
                     * Just removed the head, nothing else to free
                     */
                    current = null;
                    break;
                }

                current = current.previous;
            }

            tail = current;
            return true;
        }

        return false;
    }

    /**
     * This moves the item from the cache to the subcache, thus effectively
     * making space for new items in the current cache.
     *
     * Note that this would NOT work correctly on it's own, it needs to be
     * called only from the makeFreeSpace() method
     *
     * @throws CacheException
     */
    protected boolean remove(CacheItem item) throws CacheException {
        Object key = item.key;

        /*
         * Delete the bottommost item
         */
        index.remove(key);

        /*
         * Move it to the subCache
         */
        if(subCache != null) {
            subCache.put(key, item.getValue());
        }

        /*
         * update the counters
         */
        physicalSize -= item.getValuePhysicalSize();
        elementSize--;

        /*
         * Now delete it from the list
         */
        if (item == head) {
            head = null;
            return true;
        }

        return false;
    }
}
