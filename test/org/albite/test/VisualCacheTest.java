/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.swing.JFrame;
import org.albite.cache.CacheException;
import org.albite.cache.VisualCache;
import org.albite.cache.VisualCache.EfficientStringDrawer;
import org.albite.test.CharCacheTest.CharData;
import org.albite.test.CharCacheTest.CharKey;

/**
 *
 * @author Albus Dumbledore
 */
public class VisualCacheTest extends JFrame {

    private static final String FILENAME = "alice.txt";
    private static final Dimension DIMENSIONS = new Dimension(720, 540);
    private static final String DEFAULT_FONT_FAMILY_NAME = "sansserif";
    private static final int DEFAULT_FONT_SIZE = 12;
    
    private static final int[] updateTimes = {
        0,
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

    private final Font font = createFont();
    private int progress = 0;

    private int waitTime;
    private String waitTimeString;

    VisualCache cache;
    Rect areaDimensions;

    LegendItem[] legend;

    EfficientStringDrawer stringDrawer = new EfficientStringDrawer();

    public VisualCacheTest(final Dimension dimensions)
    {
        super("Visual Cache");

        Rect subCacheDimensions = new Rect(300, 30, 150, 460);
        VisualCache subCache = new VisualCache(null, 160, 200, 500, 1000, subCacheDimensions, "Back Cache (200)", this);

        Rect cacheDimensions = new Rect(50, 260, 150, 230);
        cache = new VisualCache(subCache, 80, 100, 80, 100, cacheDimensions, "Main Cache (100)", this);

        updateWaitTime();

        areaDimensions = new Rect(
                0, 0,
                subCacheDimensions.x + subCacheDimensions.width + 50,
                subCacheDimensions.y + subCacheDimensions.height);

        int x = areaDimensions.width + 20;
        int y = LegendItem.LENGTH + 20;

        legend = new LegendItem[] {
            new LegendItem(true, "Neutral state",              VisualCache.COLOR_NORMAL,                     x, y),
            new LegendItem(true, "Hit",                        VisualCache.COLOR_FOUND_ITEM,                 x, 2 * y),
            new LegendItem(true, "Miss",                       VisualCache.COLOR_MISS,                       x, 3 * y),
            new LegendItem(true, "Just added",                 VisualCache.COLOR_JUST_ADDED,                 x, 4 * y),
            new LegendItem(true, "About to be removed",        VisualCache.COLOR_REMOVED,                    x, 5 * y),
            new LegendItem(true, "Invalidated",                VisualCache.COLOR_INVALIDATED,                x, 6 * y),

            new LegendItem(false, "Physical Capacity",          VisualCache.COLOR_LINE_PHYSICAL_CAPACITY,    x, 8 * y),
            new LegendItem(false, "Targeted Capacity on GC",    VisualCache.COLOR_LINE_TARGETED_CAPACITY,    x, 9 * y),
            new LegendItem(false, "Items for removal",          VisualCache.COLOR_LINE_REMOVE_UNTIL,         x, 10 * y),
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

    private static Font createFont() {
        String[] fonts = Toolkit.getDefaultToolkit().getFontList();
        String fontString = fonts[0];

        for (int i = 0; i < fonts.length; i++) {
            if (DEFAULT_FONT_FAMILY_NAME.equalsIgnoreCase(fonts[i])) {
                fontString = fonts[i];
                break;
            }
        }

        return new Font(fontString, Font.PLAIN, DEFAULT_FONT_SIZE);
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

        g.setFont(font);

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

        final StringBuilder builder = stringDrawer.builder;
        builder.setLength(0);
        builder.append("Progress: ");
        builder.append(progress);
        builder.append("%");
        stringDrawer.draw(g, areaDimensions.width + 20, height - 30);

        cache.paintSecondLayer(g);
    }


    @Override
    public void repaint() {
        final Graphics g = getBufferStrategy().getDrawGraphics();
        paint(g);
        g.dispose();
        getBufferStrategy().show();
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
        final char[] chars;
        {
            String line;

            StringBuilder buffer = new StringBuilder();

            BufferedReader r =
                    new BufferedReader(
                    new InputStreamReader(
                    getClass().getResourceAsStream(FILENAME)));

            while ((line = r.readLine())  != null) {
                buffer.append(line);
            }
            chars = new char[buffer.length()];
            buffer.getChars(0, chars.length, chars, 0);
        }

        for (int i = 0; i < chars.length; i++) {
            progress = (int) ((i / (float) chars.length) * 100);

            char c = chars[i];
            CharKey key = new CharKey(c);
            CharData data = (CharData) cache.get(key);

            if (data == null) {
                data = new CharData(c);
                cache.put(key, data);
            }
        }

        repaint();
    }

    public static void main(String s[]) throws IOException, CacheException {
        VisualCacheTest visual = new VisualCacheTest(DIMENSIONS);
        visual.createBufferStrategy(2);
        visual.run();
    }
}
