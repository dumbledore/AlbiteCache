/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.cache;

import java.awt.Color;
import java.awt.Graphics;

/**
 *
 * @author Albus Dumbledore
 */
public class MemCacheVisualiser extends MemCache {
    public MemCacheVisualiser(
            final Cache subCache,
            final int   targetPhysicalSize,
            final int   physicalCapacity,
            final int   targetElementSize,
            final int   elementCapacity) {

        super(subCache, targetPhysicalSize, physicalCapacity, targetElementSize, elementCapacity);
    }

    public void paint(Graphics g, int width, int height) {
        /*
         * bg
         */
        drawRect(g, 0, 0, width, height, Color.BLACK, Color.DARK_GRAY);

        int x = 0;
        for (CacheItem current = head; current != null; current = current.next) {
            final int size = current.getValuePhysicalSize();
            final float ratio = size / (float) physicalCapacity;
            final int advance = (int) (width * ratio);

            drawBox(g, x, 0, advance, height, Color.BLACK, Color.PINK,
                    size + "",
                    current.key.toString());
            x += advance;
        }
    }

    private void drawRect(
            Graphics g,
            int x, int y, int width, int height,
            Color border, Color fill) {

        g.setColor(fill);
        g.fillRect(x, y, width, height);

        g.setColor(border);
        g.drawRect(x, y, width - 1, height - 1);
    }

    private void drawBox(
            Graphics g,
            int x, int y, int width, int height,
            Color border, Color fill,
            String label,
            String sublabel) {

        drawRect(g, x, y, width, height, border, fill);

        g.setColor(border);
        g.drawString(label, x + 2, y + (height / 3));
        g.drawString(sublabel, x + 2, y + (height / 2));
    }
//
//    @Override
//    protected boolean makeFreeSpace(int additinalSpaceNeeded)
//            throws CacheException {
//
//        if (additinalSpaceNeeded + physicalSize > physicalCapacity)
//        {
//            System.out.println("Freeing space for " + additinalSpaceNeeded
//                    + "...  " + physicalSize
//                    + " of " + physicalCapacity + " full");
//        }
//
//        return super.makeFreeSpace(additinalSpaceNeeded);
//    }
}
