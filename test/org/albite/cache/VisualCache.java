/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.cache;

import org.albite.test.Rect;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

/**
 *
 * @author Albus Dumbledore
 */
public class VisualCache extends MRUCache {
    public final Rect dimensions;
    final int borderLine;
    int removeSpaceBorderLine = 0;
    public final String cacheLabel;
    private final Component repaintMaster;
    private Color allItemsColor = null;

    private int localHits = 0;
    private int globalHits = 0;
    private int lookups = 0;

    private boolean connectToSubcache = false;
    private Color connectToSubcacheColor = COLOR_CONNECTIONS;
    
    String stateLabel = "";

    private EfficientStringDrawer stringDrawer = new EfficientStringDrawer();

    public static final Color COLOR_NORMAL = Color.GRAY;
    public static final Color COLOR_JUST_ADDED = Color.GREEN;
    public static final Color COLOR_FOUND_ITEM = Color.YELLOW;
    public static final Color COLOR_MISS = Color.BLUE;
    public static final Color COLOR_MOVING = Color.CYAN;
    public static final Color COLOR_REMOVED = Color.RED;
    public static final Color COLOR_INVALIDATED = Color.BLACK;
    public static final Color COLOR_LINE_PHYSICAL_CAPACITY = Color.BLACK;
    public static final Color COLOR_LINE_TARGETED_CAPACITY = Color.BLUE;
    public static final Color COLOR_LINE_REMOVE_UNTIL = Color.RED;
    public static final Color COLOR_CONNECTIONS = Color.BLACK;

    private volatile int waitTime = 100;

    public VisualCache(
        final Cache subCache,
        final int   targetPhysicalSize,
        final int   physicalCapacity,
        final int   targetElementSize,
        final int   elementCapacity,
        final Rect dimensions,
        final String cacheLabel,
        final Component repaintMaster) {

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
            ((VisualCache) subCache).setWaitTime(waitTime);
        }
        
        this.waitTime = waitTime;
    }

    public int getWaitTime() {
        return waitTime;
    }

    @Override
    public Cacheable get(Object key) {
        lookups++;

        stateLabel = "Getting...";
        repaint();

        VisualCacheItem item = (VisualCacheItem) index.get(key);
        if (item != null) {
            localHits++;

            stateLabel = "Found";
            item.color = COLOR_FOUND_ITEM;
            repaint();

            if (item != head) {
                stateLabel = "Moving...";
                item.box.x += 20;
                item.color = COLOR_MOVING;
                repaint();

                VisualCacheItem current = (VisualCacheItem) head;
                while (current != item) {
                    current.translate(0, - item.box.height);
                    current = (VisualCacheItem) current.next;
                }
                item.box.y = dimensions.y + dimensions.height - item.box.height;
                repaint();

                item.box.x = dimensions.x;
            }
            item.color = COLOR_NORMAL;
        } else {
            stateLabel = "Miss";
            allItemsColor = COLOR_MISS;
            repaint();
            allItemsColor = null;
        }

        Cacheable result = super.get(key);
        if (result != null) {
            globalHits++;
        }
        returnToNormal();
        return result;
    }
    
    @Override
    public void put(Object key, Cacheable value) throws CacheException {
        stateLabel = "Putting...";
        repaint();

        super.put(key, value);
        returnToNormal();
    }

    @Override
    public void invalidate(Object key) {
        VisualCacheItem item = (VisualCacheItem) index.get(key);
        
        if (item != null) {
            stateLabel = "Invalidating...";
            repaint();

            item.color = COLOR_INVALIDATED;
            repaint();

            connectToSubcache = true;
            connectToSubcacheColor = COLOR_INVALIDATED;
            translate((VisualCacheItem) item.next, 0, item.box.height);
            super.invalidate(key);
            returnToNormal();
            connectToSubcache = false;
        }
    }

    @Override
    public void invalidate() {
        stateLabel = "Trashing...";
        repaint();

        allItemsColor = COLOR_INVALIDATED;
        repaint();

        connectToSubcache = true;
        connectToSubcacheColor = COLOR_INVALIDATED;
        super.invalidate();
        returnToNormal();
        connectToSubcache = false;
    }

    @Override
    protected Cacheable searchInternalCache(Object key) {
        if (subCache != null) {
            connectToSubcache = true;
            connectToSubcacheColor = COLOR_MISS;
            stateLabel = "Deep Searching...";
            repaint();

            Cacheable result = super.searchInternalCache(key);
            if (result != null) {
                connectToSubcacheColor = COLOR_FOUND_ITEM;
                stateLabel = "Found in back cache";
                repaint();
            }

            returnToNormal();
            connectToSubcache = false;
            return result;
        }

        return null;
    }

    @Override
    protected void add(Object key, Cacheable value)
            throws CacheException {

        stateLabel = "Adding...";
        repaint();

        super.add(key, value);

        VisualCacheItem item = (VisualCacheItem) head;
        final int height = item.box.height;

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
        returnToNormal();
    }

    @Override
    protected boolean makeFreeSpace(final int additionalSpaceNeeded)
            throws CacheException {

        final int spaceNeeded = physicalSize + additionalSpaceNeeded;

        if (spaceNeeded > physicalCapacity
                || elementSize >= elementCapacity) {

            final int spaceLimit = targetPhysicalSize - additionalSpaceNeeded;
            final float ratio = spaceLimit / (float) physicalCapacity;
            removeSpaceBorderLine =
                    (int) (dimensions.height * (1.0f - ratio)) + dimensions.y;

            stateLabel = "Freeing up space...";
            repaint();

            boolean result = super.makeFreeSpace(additionalSpaceNeeded);
            removeSpaceBorderLine = 0;
            returnToNormal();
            return result;
        }

        return false;
    }

    @Override
    protected boolean remove(CacheItem item) throws CacheException {
        stateLabel = "Removing...";
        repaint();

        VisualCacheItem removed = (VisualCacheItem) item;
        removed.color = COLOR_REMOVED;
        repaint();

        connectToSubcache = true;
        connectToSubcacheColor = COLOR_REMOVED;
        repaint();

        connectToSubcacheColor = COLOR_JUST_ADDED;
        boolean result = super.remove(item);
        tail = item.previous;
        returnToNormal();

        connectToSubcache = false;
        return result;
    }

    /*
     * The main paint method
     */
    public void paint(Graphics g) {
        if (subCache != null) {
            ((VisualCache) subCache).paint(g);
            paintConnection(g);
        }

        paintLines(g);
        paintStateLabel(g);
        draw(g, (VisualCacheItem) head);
    }

    private void paintConnection(Graphics g) {
        if (connectToSubcache) {
            final int leftY = getTailY();
            final int rightY = ((VisualCache) subCache).getTailY();

            final int left = dimensions.x + dimensions.width;
            final int right = ((VisualCache) subCache).dimensions.x;

            final int top = Math.min(leftY, rightY);

            g.setColor(connectToSubcacheColor);
            g.drawLine(left, leftY, left, top);
            g.drawLine(left, top, right, top);
            g.drawLine(right, top, right, rightY);
        }
    }

    private int getTailY() {
        if (tail != null) {
            return ((VisualCacheItem) tail).box.y;
        } else {
            return dimensions.y + dimensions.height;
        }
    }

    private void paintLines(Graphics g) {
        int left = dimensions.x - 10;
        int right = dimensions.x + dimensions.width + 10;

        g.setColor(COLOR_LINE_PHYSICAL_CAPACITY);
        g.drawLine(
                left,
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
    }

    private void paintStateLabel(Graphics g) {
        g.setColor(Color.BLACK);

        if (tail != null) {
            VisualCacheItem item = (VisualCacheItem) tail;
            g.drawString(stateLabel, item.box.x, item.box.y - 10);
        } else {
            g.drawString(stateLabel,
                    dimensions.x, dimensions.y + dimensions.height - 20);
        }
    }

    public void paintSecondLayer(Graphics g) {
        if (subCache != null) {
            ((VisualCache) subCache).paintSecondLayer(g);
        }

        int height = dimensions.y + dimensions.height + 20;

        g.setColor(Color.WHITE);
        g.drawString(cacheLabel,
                dimensions.x, height);

        final float localHitRatio = localHits / (float) lookups;
        final int localHitRate = (int) (localHitRatio * 100);

        final float globalHitRatio = globalHits / (float) lookups;
        final int globalHitRate = (int) (globalHitRatio * 100);

        final StringBuilder builder = stringDrawer.builder;
        builder.setLength(0);
        builder.append("Local hit rate: ");
        builder.append(localHitRate);
        builder.append("%   Global hit rate:");
        builder.append(globalHitRate);
        builder.append("%");
        stringDrawer.draw(g, dimensions.x, height + 20);
    }

    public static class EfficientStringDrawer {
        public static final int BUFFER_SIZE = 200;
        public final StringBuilder builder = new StringBuilder(BUFFER_SIZE);
        private char[] buffer = new char[BUFFER_SIZE];

        public void draw(Graphics g, int x, int y) {
            final int length = Math.min(BUFFER_SIZE, builder.length());
            builder.getChars(0, length, buffer, 0);
            g.drawChars(buffer, 0, length, x, y);
        }
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

        g.setColor(fill);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);

        g.setColor(border);
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    private void returnToNormal() {
        stateLabel = "";
        repaint();
    }
    
    private void repaint() {
        repaintMaster.repaint();
        rest();
    }

    private void rest() {
        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {}
        }
    }
}
