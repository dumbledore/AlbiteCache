/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.swing.JFrame;
import org.albite.cache.CacheException;
import org.albite.cache.MemCacheVisualiser;
import org.albite.test.CharCacheTest.CharData;
import org.albite.test.CharCacheTest.CharKey;

/**
 *
 * @author Albus Dumbledore
 */
public class MemCachePanel extends JFrame {

    public static final String FILENAME = "alice.txt";
    public static final int GRAPHICS_WIDTH = 720;
    public static final int GRAPHICS_HEIGHT = 540;

    public static final int HIT_REST = 1;
    public static final int MISS_REST = 2;

    private static final CacheType CACHE_TYPE =
            CacheType.CACHE_MULTI_LEVEL;

    enum CacheType {
        CACHE_NORMAL,
        CACHE_LOW_DATA_TARGET_SIZE,
        CACHE_LOW_DATA_CAPACITY,
        CACHE_LOW_ELEMENT_TARGET_SIZE,
        CACHE_LOW_ELEMENT_CAPACITY,
        CACHE_MULTI_LEVEL,
    }

    MemCacheVisualiser cache;

    public MemCachePanel(final String title)
    {
        super(title);

        switch (CACHE_TYPE) {
            case CACHE_NORMAL:
            {
                 cache = new MemCacheVisualiser(null, 200, 256, 500, 1000);
                 break;
            }

            case CACHE_LOW_DATA_TARGET_SIZE:
            {
                cache = new MemCacheVisualiser(null, 20, 256, 500, 1000);
                break;
            }

            case CACHE_LOW_DATA_CAPACITY:
            {
                cache = new MemCacheVisualiser(null, 20, 35, 500, 1000);
                break;
            }

            case CACHE_LOW_ELEMENT_TARGET_SIZE:
            {
                cache = new MemCacheVisualiser(null, 200, 256, 5, 30);
                break;
            }

            case CACHE_LOW_ELEMENT_CAPACITY:
            {
                cache = new MemCacheVisualiser(null, 20, 256, 4, 10);
                break;
            }

            case CACHE_MULTI_LEVEL:
            {
                cache = new MemCacheVisualiser(
                        new MemCacheVisualiser(null,
                        200, 256, 800, 1000), //back cache. Should be bigger
                        80, 100, 60, 100);
                break;
            }

            default:
                throw new RuntimeException("Unknown cache type.");
        }
    }

    @Override
    public synchronized void paint(Graphics g) {
        cache.paint(g, getWidth(), getHeight());
    }

    private void run() throws IOException, CacheException {
        String line;

        BufferedReader r =
                new BufferedReader(
                new InputStreamReader(
                getClass().getResourceAsStream(FILENAME)));
        
        while ((line = r.readLine())  != null) {
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                CharKey key = new CharKey(c);
                CharData data = (CharData) cache.get(key);

                if (data == null) {
                    data = new CharData(c);
                    cache.put(key, data);

                    /*
                     * Something's happening. Do a repaint and a longer sleep
                     */
                    repaint();
                    rest(MISS_REST);
                } else {
                    /*
                     * Nothing of interest. Do a short sleep
                     */
                    rest(HIT_REST);
                }
            }
        }

        repaint();
    }

    private static void rest(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
        }

    }

    public static void main(String s[]) throws IOException, CacheException {
        WindowAdapter window =
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        };

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }
        };

        MemCachePanel visual = new MemCachePanel("Memory Cache");
        visual.addWindowListener(window);
        visual.addMouseListener(mouse);
        visual.pack();
        visual.setSize(new Dimension(GRAPHICS_WIDTH, GRAPHICS_HEIGHT));
        visual.setVisible(true);

        visual.run();
    }
}
