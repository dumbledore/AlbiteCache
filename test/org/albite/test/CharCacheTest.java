/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import junit.framework.TestCase;
import org.albite.cache.CacheException;
import org.albite.cache.Cacheable;
import org.albite.cache.MemCacheProfiler;

/**
 *
 * @author Albus Dumbledore
 */
public class CharCacheTest extends TestCase {

    final String filename = "alice.txt";
    MemCacheProfiler cache;
    MemCacheProfiler subCache;

//    String cacheLabel = "L1";
//    String subCacheLabel = "L2";

    String cacheLabel = null;
    String subCacheLabel = null;

    public void test() throws IOException, CacheException {
        String line;

        subCache = new MemCacheProfiler(subCacheLabel, null, 200, 256, 800, 1000); //back cache. Should be bigger
        cache = new MemCacheProfiler(cacheLabel, subCache, 80, 100, 60, 100);

        BufferedReader r =
                new BufferedReader(
                new InputStreamReader(
                getClass().getResourceAsStream(filename)));

        while ((line = r.readLine())  != null) {
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                CharKey key = new CharKey(c);
                CharData data = (CharData) cache.get(key);
                if (data == null) {
                    data = new CharData(c);
                    cache.put(key, data);
                }
//                System.out.println(c);
            }
        }

        System.out.println("----------");
        cache.printStatistics();
        System.out.println("----------");
        subCache.printStatistics();
    }

    static class CharKey extends Object{
        char c;

        public CharKey(char c) {
            this.c = c;
        }

        @Override
        public int hashCode() {
            return c;
        }

        public boolean equals(Object obj) {
            if (obj instanceof CharKey && c == ((CharKey) obj).c) {
                return true;
            }

            return false;
        }

        @Override
        public String toString() {
            return c + " (" + ((int) c) + ")";
        }
    }

    static class CharData implements Cacheable {
        char value;

        public CharData(final char value) {
            this.value = value;
        }

        public int getPhysicalSize() {
            return value % 20;
        }
    }
}
