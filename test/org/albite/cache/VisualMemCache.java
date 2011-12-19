/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.cache;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import org.albite.cache.visual.Rect;

/**
 *
 * @author Albus Dumbledore
 */
public class VisualMemCache extends MemCache {
    public final Rect dimensions;
    public final String cacheLabel;
    private final Component repaintMaster;
    
    String stateLabel = "Working...";

    private static final Color COLOR_FREE = Color.DARK_GRAY;
    private static final Color COLOR_NORMAL = Color.GRAY;
    private static final Color COLOR_JUST_ADDED = Color.RED;

    private static final int STANDARD_WAIT_TIME = 100;

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
    }

    @Override
    public Cacheable get(Object key) {
        stateLabel = "Getting...";
        print();

        Cacheable result = super.get(key);
        repaint();
        return result;
    }
    
    @Override
    public void put(Object key, Cacheable value) throws CacheException {
        stateLabel = "Putting...";
        print();

        super.put(key, value);
        repaint();
    }

    @Override
    public void invalidate(Object key) {
        if (index.get(key) != null) {
            stateLabel = "Invalidating...";
            print();

            super.invalidate(key);
            repaint();
        }
    }

    @Override
    public void invalidate() {
        stateLabel = "Trashing...";
        print();

        super.invalidate();
        repaint();
    }

    @Override
    protected Cacheable searchInternalCache(Object key) {
        if (subCache != null) {
            stateLabel = "Deep Searching...";
            print();

            Cacheable result = super.searchInternalCache(key);
            repaint();
            return result;
        }

        return null;
    }

    @Override
    protected void add(Object key, Cacheable value)
            throws CacheException {

        stateLabel = "Adding...";
        print();

        super.add(key, value);

        VisualCacheItem item = (VisualCacheItem) head;
        final int height = item.box.height - 1;

        if (head.next != null) {
            VisualCacheItem next = (VisualCacheItem) head.next;
            translate(next, 0, - height);
            repaint();
        }

        /*
         * Now we've got the new item at the top
         */
        item.color = COLOR_JUST_ADDED;
        item.translate(0, - height);
        repaint();

        item.color = COLOR_NORMAL;
        repaint();
    }

    @Override
    protected boolean makeFreeSpace(final int additionalSpaceNeeded)
            throws CacheException {

        final int spaceNeeded = physicalSize + additionalSpaceNeeded;

            if (spaceNeeded > physicalCapacity
                    || elementSize >= elementCapacity) {

            stateLabel = "Freeing up space...";
            print();

            boolean result = super.makeFreeSpace(additionalSpaceNeeded);
            repaint();
            return result;
        }

        return false;
    }

    @Override
    protected boolean remove(CacheItem item) throws CacheException {
        stateLabel = "Removing...";
        print();
        
        boolean result = super.remove(item);
        repaint();
        return result;
    }

    /*
     * The main paint method
     */
    public void paint(Graphics g) {
        if (subCache != null) {
            ((VisualMemCache) subCache).paint(g);
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
            final int height = (int) (dimensions.height * ratio);

            // put it at top
            box = new Rect(
                    dimensions.x,
                    dimensions.y + dimensions.height,
                    dimensions.width,
                    height);
        }

        void draw (Graphics g) {
            //TODO: Use some more fancy drawing
            drawRect(g, box, Color.BLACK, color);
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

        apply(head,
            new ItemOperation() {

            @Override
            void operate(VisualCacheItem item) {
                item.draw(g);
            }
        });
    }
    
    private static void drawRect(
            Graphics g,
            Rect rect,
            Color border, Color fill) {

        g.setColor(fill);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);

        g.setColor(border);
        g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
    }
    
    private void repaint() {
        repaintMaster.repaint();
        rest(STANDARD_WAIT_TIME);
    }

    private static void rest(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            System.out.println("WOWY?");
        }
    }
}
