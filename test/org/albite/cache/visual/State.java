/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.cache.visual;

import java.awt.Graphics;

/**
 *
 * @author Albus Dumbledore
 */
public abstract class State {
    protected final int waitTime;
    protected State next;

    public State(final int waitTime) {
        this.waitTime = waitTime;
    }

    public abstract void draw(Graphics g);

    public final void run(Graphics g) {
        State current = this;
        while (current != null) {
            current.draw(null);
            rest(waitTime);
            current = current.next;
        }
    }

    public static void rest(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {}
    }
}
