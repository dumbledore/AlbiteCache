/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.test;

import java.awt.Color;
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
import org.albite.cache.VisualMemCache;
import org.albite.cache.visual.Rect;
import org.albite.test.CharCacheTest.CharData;
import org.albite.test.CharCacheTest.CharKey;

/**
 *
 * @author Albus Dumbledore
 */
public class MemCacheVisualTest extends JFrame {

    private static final String FILENAME = "alice.txt";
    private static final Dimension DIMENSIONS = new Dimension(720, 540);
    private static final int[] updateTimes = {
        9,
        10,
        20,
        50,
        100,
        200,
        500,
        1000
    };
    private int updateTimeIndex = 4;

    private int waitTime;
    private String waitTimeString;

    VisualMemCache cache;
    Rect areaDimensions;

    LegendItem[] legend;

    public MemCacheVisualTest(final Dimension dimensions)
    {
        super("Visual Cache");

        Rect subCacheDimensions = new Rect(300, 30, 150, 460);
        VisualMemCache subCache = new VisualMemCache(null, 160, 200, 500, 1000, subCacheDimensions, "Back Cache (200)", this);

        Rect cacheDimensions = new Rect(50, 260, 150, 230);
        cache = new VisualMemCache(subCache, 80, 100, 80, 100, cacheDimensions, "Main Cache (100)", this);

        updateWaitTime();

        areaDimensions = new Rect(
                0, 0,
                subCacheDimensions.x + subCacheDimensions.width + 50,
                subCacheDimensions.y + subCacheDimensions.height);

        int x = areaDimensions.width + 20;
        int y = LegendItem.LENGTH + 20;

        legend = new LegendItem[] {
            new LegendItem(true, "Neutral state",              VisualMemCache.COLOR_NORMAL,                     x, y),
            new LegendItem(true, "Hit",                        VisualMemCache.COLOR_FOUND_ITEM,                 x, 2 * y),
            new LegendItem(true, "Miss",                       VisualMemCache.COLOR_MISS,                       x, 3 * y),
            new LegendItem(true, "Just added",                 VisualMemCache.COLOR_JUST_ADDED,                 x, 4 * y),
            new LegendItem(true, "About to be removed",        VisualMemCache.COLOR_REMOVED,                    x, 5 * y),
            new LegendItem(true, "Invalidated",                VisualMemCache.COLOR_INVALIDATED,                x, 6 * y),

            new LegendItem(false, "Physical Capacity",          VisualMemCache.COLOR_LINE_PHYSICAL_CAPACITY,    x, 8 * y),
            new LegendItem(false, "Targeted Capacity on GC",    VisualMemCache.COLOR_LINE_TARGETED_CAPACITY,    x, 9 * y),
            new LegendItem(false, "Items for removal",          VisualMemCache.COLOR_LINE_REMOVE_UNTIL,         x, 10 * y),
        };

        WindowAdapter window =
                new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        };

        MouseAdapter mouse =
                new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    updateWaitTime(-1);
                } else {
                    updateWaitTime(+1);
                }
            }
        };

        addWindowListener(window);
        addMouseListener(mouse);
        pack();
        setSize(dimensions);
        setVisible(true);
    }

    private synchronized void updateWaitTime(final int indexOffset) {
        updateTimeIndex += indexOffset;
        updateWaitTime();
    }

    private synchronized void updateWaitTime() {
        if (updateTimeIndex < 0) {
            updateTimeIndex = 0;
        }

        if (updateTimeIndex >= updateTimes.length) {
            updateTimeIndex = updateTimes.length - 1;
        }
        
        this.waitTime = updateTimes[updateTimeIndex];
        waitTimeString = "Update interval: " + waitTime;
        cache.setWaitTime(waitTime);
    }

    @Override
    public void paint(Graphics g) {
        final int width = getWidth();
        final int height = getHeight();

        g.setColor(Color.WHITE);
        g.fillRect(
                0,
                0,
                areaDimensions.width,
                areaDimensions.height);

        cache.paint(g);

        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, areaDimensions.height, width, height - areaDimensions.height);
        g.fillRect(areaDimensions.width, 0, width - areaDimensions.width, height);

        for (int i = 0; i < legend.length; i++) {
            legend[i].draw(g);
        }

        g.drawString(waitTimeString, areaDimensions.width + 20, height - 50);

        cache.paintSecondLayer(g);
    }

    class LegendItem {
        static final int LENGTH = 20;
        static final int LENGTH2 = LENGTH / 2;

        String title;
        Color color;
        int x, y;
        boolean square;

        LegendItem(boolean square, String title, Color color, int x, int y) {
            this.title = title;
            this.color = color;
            this.x = x;
            this.y = y;
            this.square = square;
        }

        void draw(Graphics g) {
            g.setColor(color);
            if (square) {
                g.fillRect(x, y, LENGTH, LENGTH);
            } else {
                g.drawLine(x, y + LENGTH2, x + LENGTH, y + LENGTH2);
            }
            g.setColor(Color.WHITE);
            g.drawString(title, x + LENGTH + 10, y + LENGTH - 5);
        }
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
                }
            }
        }

        repaint();
    }

    public static void main(String s[]) throws IOException, CacheException {
        MemCacheVisualTest visual = new MemCacheVisualTest(DIMENSIONS);
        visual.run();
    }
}
