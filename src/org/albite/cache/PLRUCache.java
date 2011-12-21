/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.cache;

/**
 *
 * @author Albus Dumbledore
 */
public abstract class PLRUCache extends Cache {
    public Cacheable get(Object key) {
        CacheItem item = (CacheItem) index.get(key);

        if (item != null) {
            /*
             * Found it
             */

            if (head != item) {
                /*
                 * It's not the first item, bring it to the front then
                 */

                /*
                 * The item is not the first one, i.e item.previous is not null
                 */
                item.previous.next = item.next;

                if (tail == item) {
                    /*
                     * Need to update the tail
                     */
                    tail = item.previous;
                } else {
                    /*
                     * There's a next item
                     */
                    item.next.previous = item.previous;
                }

                item.previous = null;
                item.next = head;
                head.previous = item;
                head = item;
            }

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


    public Cacheable getMostRecentlyUsed() {
        return head.getValue();
    }
}
