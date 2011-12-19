/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.cache;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import org.albite.cache.visual.Positionable;
import org.albite.cache.visual.Rect;
import org.albite.cache.visual.State;

/**
 *
 * @author Albus Dumbledore
 */
public class VisualMemCache extends MemCache {
    final Component repaintMaster;
    final Rect dimensions;
    State currentState = null;

    private static final Color COLOR_FREE = Color.DARK_GRAY;
    private static final Color COLOR_NORMAL = Color.GRAY;

    private static final int STANDARD_WAIT_TIME = 40;

    public VisualMemCache(
        final Cache subCache,
        final int   targetPhysicalSize,
        final int   physicalCapacity,
        final int   targetElementSize,
        final int   elementCapacity,
        final Rect dimensions,
        final Component repaintMaster) {

        super(
                subCache,
                targetPhysicalSize, physicalCapacity,
                targetElementSize, elementCapacity);
        
        this.dimensions = dimensions;
        this.repaintMaster = repaintMaster;
    }

    @Override
    public Cacheable get(Object key) {
        Cacheable result = super.get(key);
        repaint();
        return result;
    }
    
    @Override
    public void put(Object key, Cacheable value) throws CacheException {
        super.put(key, value);
        repaint();
    }

    @Override
    public void invalidate(Object key) {
        super.invalidate(key);
        repaint();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        repaint();
    }

    @Override
    protected Cacheable searchInternalCache(Object key) {
        Cacheable result = super.searchInternalCache(key);
        repaint();
        return result;
    }

    @Override
    protected void add(Object key, Cacheable value)
            throws CacheException {

        VisualCacheItem item = (VisualCacheItem) head;
        translate(item, 0, - item.box.height + 1);
        repaint();

        super.add(key, value);

        /*
         * Now we've got the new item at the top
         */
        item = (VisualCacheItem) head;
        item.translate(0, - item.box.height + 1);
        repaint();
    }

    @Override
    protected boolean makeFreeSpace(final int additionalSpaceNeeded)
            throws CacheException {

        boolean result = super.makeFreeSpace(additionalSpaceNeeded);
        repaint();
        return result;
    }

    @Override
    protected boolean remove(CacheItem item) throws CacheException {
        boolean result = super.remove(item);
        repaint();
        return result;
    }

    /*
     * The main paint method
     */
    public void paint(Graphics g) {
        if (currentState == null) {
            /*
             * Just a simple draw
             */
            draw(g, (VisualCacheItem) head, COLOR_NORMAL);
        } else {
            currentState.draw(g);
        }
    }

    /*
     * Here come the states
     */
    class CacheState extends State {
        final String label;

        CacheState(final String label, final int waitTime) {
            super(waitTime);
            this.label = label;
        }
        
        @Override
        public void draw(Graphics g) {
            g.drawString(label, dimensions.x, dimensions.y - 20);
        }
    }

    class GettingState extends CacheState {
        final Positionable caller;

        GettingState(Positionable caller) {
            super("Getting...", STANDARD_WAIT_TIME);
            
            this.next = new SearchState();
            this.caller = caller;
        }

        @Override
        public void draw(Graphics g) {
            /*
             * Draw the get arrow
             */
            
        }
    }

    class SearchState extends CacheState {
        public SearchState() {
            super("Searching...", STANDARD_WAIT_TIME);
        }

        @Override
        public void draw(Graphics g) {
        }
    }

//    class 

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

        void draw (Graphics g, Color c) {
            //TODO: Use some more fancy drawing
            drawRect(g, box, Color.BLACK, c);
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
            final VisualCacheItem head,
            final Color color) {

        apply(head,
            new ItemOperation() {

            @Override
            void operate(VisualCacheItem item) {
                item.draw(g, color);
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
        State.rest(STANDARD_WAIT_TIME);
    }
}
