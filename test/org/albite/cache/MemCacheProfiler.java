/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.cache;

import java.util.Enumeration;

/**
 *
 * @author Albus Dumbledore
 */
public class MemCacheProfiler extends MemCache {

    private String tag = "";

    /*
     * How many times the cache couldn't return the value
     */
    private int globalMisses = 0;

    /*
     * How many times the cache didn't directly have the value, but
     * perhaps the subcache(s) did.
     */
    private int localMisses = 0;

    /*
     * How many times the cache returned the value
     */
    private int globalHits = 0;

    /*
     * How many time the cache directly had the value
     */
    private int localHits = 0;

    /*
     * How many times an item has been added to the cache
     */
    private int additions = 0;

    /*
     * How many times the cache was fully invalidated, i.e. trashed
     */
    private int trashes = 0;

    /*
     * How many times an item from the cache was invaildated
     */
    private int invalidations = 0;

    /*
     * How many times an item from the cache was removed because of the
     * lack of space
     */
    private int garbaged = 0;

    /*
     * How many times garbage collection has been run, i.e.
     * how many times it has been necessary to tidy up the cache
     */
    private int gcruns = 0;

    /*
     * How many times it has been validated
     */
    private int validityChecks = 0;

    private String method = "unknown";
    private boolean verbose = true;

    public MemCacheProfiler(
            final String tag,
            final Cache subCache,
            final int   targetPhysicalSize,
            final int   physicalCapacity,
            final int   targetElementSize,
            final int   elementCapacity) {

        super(subCache, targetPhysicalSize, physicalCapacity, targetElementSize, elementCapacity);
        if (tag == null) {
            verbose = false;
        } else {
            verbose = true;
            this.tag = tag + ":  ";
        }
    }

    @Override
    public void invalidate() {
        method = "invalidate()";

        checkValidity();

        if (verbose) {
            print("Invalidating cache...");
        }
        trashes++;
        super.invalidate();

        checkValidity();
    }

    @Override
    public void invalidate(Object key) {
        method = "invalidate(key)";

        checkValidity();

        if (index.get(key) != null) {
            if (verbose) {
                print("Invalidated " + key);
            }
            invalidations++;
            super.invalidate(key);
        } else {
            if (verbose) {
                print("No need to invalidate " + key);
            }
        }

        checkValidity();
    }

    @Override
    public Cacheable get(Object key) {
        method = "get(key)";

        checkValidity();

        if (verbose) {
            print("Getting " + key);
        }

        if (index.get(key) == null) {
            if (verbose) {
                print("Missed locally " + key);
            }
            localMisses++;
        } else {
            if (verbose) {
                print("Hit locally " + key);
            }
            localHits++;
        }
        
        Cacheable item = super.get(key);

        if (item == null) {
            if (verbose) {
                print("Missed " + key);
            }
            globalMisses++;
        } else {
            if (verbose) {
                print("Hit " + key);
            }
            globalHits++;
        }

        checkValidity();

        return item;
    }

    @Override
    public void put(Object key, Cacheable value) throws CacheException {
        method = "put(key, value)";

        checkValidity();

        if (verbose) {
            print("Adding " + key + " (" + value.getPhysicalSize() + "b)");
        }
        additions++;

        super.put(key, value);

        checkValidity();
    }

    @Override
    protected void add(Object key, Cacheable value)
            throws CacheException {

        method = "add(key, value)";
        
        checkValidity();

        super.add(key, value);

        checkValidity();
    }

    @Override
    protected boolean makeFreeSpace(final int additionalSpaceNeeded)
            throws CacheException {

        method = "makeFreeSpace(extraSpace)";

        checkValidity();

        if (verbose) {
            print("Running garbage collection...");
        }

        if (super.makeFreeSpace(additionalSpaceNeeded)) {

            gcruns++;
            
            /*
             * Do a second check for the available size
             */
            final int spaceLimit = targetPhysicalSize - additionalSpaceNeeded;
            if (physicalSize > spaceLimit
                || elementSize > targetElementSize) {

                throw new CacheException(tag +
                        "Couldn't free up space in the cache " +
                        "for some unknown reason."
                );
            }

            checkValidity();
            return true;
        }

        if (verbose) {
            print("GC skipped");
        }
        checkValidity();
        return false;
    }

    @Override
    protected boolean remove(CacheItem item) throws CacheException {
        method = "remove";

        if (verbose) {
            print("Removing " + item.key + " (" + item.getValuePhysicalSize() + "b)");
        }

        garbaged++;
        return super.remove(item);
    }

    public void printStatistics() {
        method = "statistics";

        printMem();
        for (int i = 0; i < 10; i++) {
            Runtime.getRuntime().gc();
        }
        printMem();

        print("Global Hits: " + globalHits);
        print("Global Misses: " + globalMisses);
        print("Local Hits: " + localHits);
        print("Local Misses: " + localMisses);
        print("Additions: " + additions);
        print("Trashes: " + trashes);
        print("Invalidations: " + invalidations);
        print("Garbaged: " + garbaged);
        print("GC Runs: " + gcruns);
        print("Validity checks: " + validityChecks);
    }

    private void printMem() {
        final long freeMem = Runtime.getRuntime().freeMemory();
        final long totalMem = Runtime.getRuntime().totalMemory();
        print("used " + (totalMem - freeMem) + " out of " + totalMem);
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("[--");

        CacheItem current = head;

        while (current != null) {
            buffer.append("(");
            buffer.append(current.key);
            buffer.append(", ");
            buffer.append(current.getValue().getPhysicalSize());
            buffer.append(")--");

            current = current.next;
        }

        buffer.append("--]");

        return buffer.toString();
    }

    protected void checkValidity() {
        if (verbose) {
            print(this.toString());
        }
        checkIntegrity();
        checkIndex();
        validityChecks++;
    }

    private void checkIntegrity() {
        CacheItem previous = null;
        CacheItem current = head;
        int elements = 0;
        int data = 0;

        /*
         * Iterate the list
         */
        while (current != null) {
            /*
             * Check if double linking is OK
             */
            if (current.previous != previous) {
                throw new RuntimeException(tag + "Links are not OK.");
            }

            /*
             * Update the data/count statistics
             */
            data        += current.getValuePhysicalSize();
            elements    += 1;

            if (!index.contains(current)) {
                throw new RuntimeException(tag +
                        "Index doesn't contain the item " + current.key + " from the list");
            } else {
                if ((CacheItem) index.get(current.key) != current) {
                    throw new RuntimeException(tag +
                            "Index contains incorrect data for " + current.key);
                }
            }

            previous = current;
            current = current.next;
        }

        /*
         * Check data/count
         */
        if (tail != previous) {
            throw new RuntimeException(tag + "Tail is not OK.");
        }

        if (elements != elementSize) {
            throw new RuntimeException(
                    tag + "Element count is not OK: elements = "
                    + elements + ", elementSize = " + elementSize);
        }

        if (elementSize > elementCapacity) {
            throw new RuntimeException(tag + "elements > elementCapacity");
        }

        if (data != physicalSize) {
            throw new RuntimeException(tag + "physical size is not correct");
        }

        if (physicalSize > physicalCapacity) {
            throw new RuntimeException(tag + "physicalSize > physicalCapacity");
        }
    }

    private void checkIndex() {
        Enumeration e = index.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            CacheItem current = (CacheItem) index.get(key);
            if (current == null) {
                throw new RuntimeException(tag + "Missing item for key " + key + " in index");
            }

            if (!listContainsValue(current)) {
                throw new RuntimeException(tag + "List doesn't contain item for key " + key);
            }
        }
    }

    private boolean listContainsValue(CacheItem item) {
        CacheItem current = head;
        while (current != null) {
            if (current == item) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    private void print(String message) {
        System.out.println(tag + method + "  " + message);
    }
}
