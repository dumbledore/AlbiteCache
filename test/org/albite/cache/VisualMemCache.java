/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.cache;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Random;
import org.albite.cache.visual.Rect;

/**
 *
 * @author Albus Dumbledore
 */
public class VisualMemCache extends MemCache {
    public final Rect dimensions;
    final int borderLine;
    int removeSpaceBorderLine = 0;
    public final String cacheLabel;
    private final Component repaintMaster;
    private Color allItemsColor = null;
    private long currentTime;
    
    String stateLabel = "";

    public static final Color COLOR_NORMAL = Color.GRAY;
    public static final Color COLOR_JUST_ADDED = Color.GREEN;
    public static final Color COLOR_FOUND_ITEM = Color.YELLOW;
    public static final Color COLOR_MISS = Color.BLUE;
    public static final Color COLOR_REMOVED = Color.RED;
    public static final Color COLOR_INVALIDATED = Color.BLACK;
    public static final Color COLOR_LINE_PHYSICAL_CAPACITY = Color.BLACK;
    public static final Color COLOR_LINE_TARGETED_CAPACITY = Color.BLUE;
    public static final Color COLOR_LINE_REMOVE_UNTIL = Color.RED;

    private volatile int waitTime = 100;

    private Random random = new Random();

    public VisualMemCache(
        final Cache subCache,
        final int   targetPhysicalSize,
        final int   physicalCapacity,
        final int   targetElementSize,
        final int   elementCapacity,
        final Rect dimensions,
        final String cacheLabel,
        final Component repaintMaster) {

        super(
                subCache,
                targetPhysicalSize, physicalCapacity,
                targetElementSize, elementCapacity);
        
        this.dimensions = dimensions;
        this.cacheLabel = cacheLabel;
        this.repaintMaster = repaintMaster;

        final float ratio = targetPhysicalSize / (float) physicalCapacity;
        this.borderLine = (int) (dimensions.height * (1.0f - ratio)) + dimensions.y;
    }

    public void setWaitTime(final int waitTime) {
        if (waitTime < 0) {
            throw new IllegalArgumentException();
        }

        if (subCache != null) {
            ((VisualMemCache) subCache).setWaitTime(waitTime);
        }
        
        this.waitTime = waitTime;
    }

    public int getWaitTime() {
        return waitTime;
    }

    @Override
    public Cacheable get(Object key) {
        startCounting();
        stateLabel = "Getting...";
        print();
        repaint();

        startCounting();
        VisualCacheItem item = (VisualCacheItem) index.get(key);
        if (item != null) {
            startCounting();
            stateLabel = "Found";
            print();
            item.color = COLOR_FOUND_ITEM;
            repaint();
            item.color = COLOR_NORMAL;
        } else {
            startCounting();
            stateLabel = "Miss";
            allItemsColor = COLOR_MISS;
            repaint();
            allItemsColor = null;
        }

        startCounting();
        Cacheable result = super.get(key);
        returnToNormal();
        return result;
    }
    
    @Override
    public void put(Object key, Cacheable value) throws CacheException {
        startCounting();
        stateLabel = "Putting...";
        print();
        repaint();

        startCounting();
        super.put(key, value);
        returnToNormal();
    }

    @Override
    public void invalidate(Object key) {
        startCounting();
        VisualCacheItem item = (VisualCacheItem) index.get(key);
        
        if (item != null) {
            stateLabel = "Invalidating...";
            print();
            repaint();

            startCounting();
            item.color = COLOR_INVALIDATED;
            repaint();

            startCounting();
            translate((VisualCacheItem) item.next, 0, item.box.height - 1);
            super.invalidate(key);
            returnToNormal();
        }
    }

    @Override
    public void invalidate() {
        startCounting();
        stateLabel = "Trashing...";
        print();
        repaint();

        startCounting();
        allItemsColor = COLOR_INVALIDATED;
        repaint();

        startCounting();
        super.invalidate();
        returnToNormal();
    }

    @Override
    protected Cacheable searchInternalCache(Object key) {
        if (subCache != null) {
            startCounting();
            stateLabel = "Deep Searching...";
            print();
            repaint();

            startCounting();
            Cacheable result = super.searchInternalCache(key);
            returnToNormal();
            return result;
        }

        return null;
    }

    @Override
    protected void add(Object key, Cacheable value)
            throws CacheException {

        startCounting();
        stateLabel = "Adding...";
        print();
        repaint();

        startCounting();
        super.add(key, value);

        VisualCacheItem item = (VisualCacheItem) head;
        final int height = item.box.height - 1;

        if (head.next != null) {
            VisualCacheItem next = (VisualCacheItem) head.next;
            translate(next, 0, - height);
            repaint();
            startCounting();
        }

        /*
         * Now we've got the new item at the top
         */
        item.color = COLOR_JUST_ADDED;
        item.translate(0, - height);
        repaint();

        startCounting();
        item.color = COLOR_NORMAL;
        returnToNormal();
    }

    @Override
    protected boolean makeFreeSpace(final int additionalSpaceNeeded)
            throws CacheException {

        final int spaceNeeded = physicalSize + additionalSpaceNeeded;

        if (spaceNeeded > physicalCapacity
                || elementSize >= elementCapacity) {

            startCounting();
            final int spaceLimit = targetPhysicalSize - additionalSpaceNeeded;
            final float ratio = spaceLimit / (float) physicalCapacity;
            removeSpaceBorderLine =
                    (int) (dimensions.height * (1.0f - ratio)) + dimensions.y;

            stateLabel = "Freeing up space...";
            print();
            repaint();

            boolean result = super.makeFreeSpace(additionalSpaceNeeded);
            removeSpaceBorderLine = 0;
            startCounting();
            returnToNormal();
            return result;
        }

        return false;
    }

    @Override
    protected boolean remove(CacheItem item) throws CacheException {
        startCounting();
        stateLabel = "Removing...";
        print();
        repaint();

        startCounting();
        VisualCacheItem removed = (VisualCacheItem) item;
        removed.color = COLOR_REMOVED;
        repaint();

        startCounting();
        boolean result = super.remove(item);
        returnToNormal();
        return result;
    }

    /*
     * The main paint method
     */
    public void paint(Graphics g) {
        if (subCache != null) {
            ((VisualMemCache) subCache).paint(g);
        }

        int left = dimensions.x - 20;
        int right = dimensions.x + dimensions.width + 20;

        g.setColor(COLOR_LINE_PHYSICAL_CAPACITY);
        g.drawLine(
                0,
                dimensions.y,
                right,
                dimensions.y);

        g.setColor(COLOR_LINE_TARGETED_CAPACITY);
        g.drawLine(
                left,
                borderLine,
                right,
                borderLine);

        if (removeSpaceBorderLine > 0) {
            g.setColor(COLOR_LINE_REMOVE_UNTIL);
            g.drawLine(
                    left,
                    removeSpaceBorderLine,
                    right,
                    removeSpaceBorderLine);
        }

        g.setColor(Color.BLACK);

        if (tail != null) {
            VisualCacheItem item = (VisualCacheItem) tail;
            g.drawString(stateLabel, item.box.x, item.box.y - 10);
        } else {
            g.drawString(stateLabel,
                    dimensions.x, dimensions.y + dimensions.height - 20);
        }
        draw(g, (VisualCacheItem) head);
    }

    public void paintSecondLayer(Graphics g) {
        if (subCache != null) {
            ((VisualMemCache) subCache).paintSecondLayer(g);
        }

        g.setColor(Color.WHITE);
        g.drawString(cacheLabel,
                dimensions.x, dimensions.y + dimensions.height + 20);
    }

    /*
     * The special sort of CacheItem we need. It's different from the
     * one used in MemCache with that that it can be drawn, i.e.
     * it contains info about it's position and size
     */
    @Override
    protected CacheItem createCacheItem(Object key, Cacheable value) {
        return new VisualCacheItem(key, value);
    }

    private class VisualCacheItem extends CacheItem {
        final Cacheable value;
        final Rect box;
        Color color = COLOR_NORMAL;

        VisualCacheItem(final Object key, final Cacheable value) {
            this.key = key;
            this.value = value;

            final int size = value.getPhysicalSize();
            final float ratio = size / (float) physicalCapacity;
            final int height = Math.max((int) (dimensions.height * ratio), 3);

            box = new Rect(
                    dimensions.x,
                    dimensions.y + dimensions.height,
                    dimensions.width,
                    height);
        }

        void draw(Graphics g) {
            drawRect(g, box, Color.BLACK, color);
        }

        void draw(Graphics g, Color c) {
            drawRect(g, box, Color.BLACK, allItemsColor);
        }

        void translate(final int x, final int y) {
//            System.out.println(
//                    "(" + box.x + ", " + box.y + ") -> ("
//                    + (box.x + x) + ", " + (box.y + y) + ")"
//                    + "  [" + box.width + ", " + box.height + "]");

            box.x += x;
            box.y += y;
        }

        @Override
        Cacheable getValue() {
            return value;
        }
    }

    /*
     * Helper methods
     */
    private void print() {
//        System.out.println(stateLabel);
    }

    private static void apply(
            CacheItem head,
            ItemOperation operation) {

        VisualCacheItem current = (VisualCacheItem) head;
        while (current != null) {
            operation.operate(current);
            current = (VisualCacheItem) current.next;
        }
    }

    abstract class ItemOperation {
        abstract void operate(VisualCacheItem item);
    }

    private void translate(final VisualCacheItem head,
            final int x, final int y) {

        apply(head,
            new ItemOperation() {
            @Override
            void operate(VisualCacheItem item) {
                item.translate(x, y);
            }
        });
    }

    private void draw(
            final Graphics g,
            final VisualCacheItem head) {

        if (allItemsColor == null) {
            apply(head,
                new ItemOperation() {

                @Override
                void operate(VisualCacheItem item) {
                    item.draw(g);
                }
            });
        } else {
            apply(head,
                new ItemOperation() {

                @Override
                void operate(VisualCacheItem item) {
                    item.draw(g, allItemsColor);
                }
            });
        }
    }
    
    private static void drawRect(
            Graphics g,
            Rect rect,
            Color border, Color fill) {

//        final int width = rect.width - 1;
//        final int height = rect.height - 1;
//        final int roundCorner = height / 3;

//        g.setColor(Color.DARK_GRAY);
//        g.fillRect(rect.x, rect.y, rect.width, rect.height);

        g.setColor(fill);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
//        g.fillRoundRect(rect.x, rect.y, rect.width - 1, rect.height - 1, roundCorner, roundCorner);
//        g.fillOval(rect.x, rect.y, rect.width, rect.height);

        g.setColor(border);
        g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
//        g.drawRoundRect(rect.x, rect.y, rect.width - 1, rect.height - 1, roundCorner, roundCorner);
//        g.drawOval(rect.x, rect.y, rect.width - 1, rect.height - 1);
    }

    private void returnToNormal() {
        stateLabel = "";
        repaint();
    }
    
    private void repaint() {
        repaintMaster.repaint();
        rest(waitTime);
    }

    private void startCounting() {
        currentTime = System.currentTimeMillis();
    }

    private void rest(final int millis) {
        final long elapsed = Math.max(0, System.currentTimeMillis() - currentTime);
        final long haveToWait = Math.max(0, millis - elapsed);

        if (haveToWait > 0) {
            try {
                Thread.sleep(haveToWait);
            } catch (InterruptedException e) {
                System.out.println("WOWY?");
            }
        }
    }
}
