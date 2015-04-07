/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chrenderclient;

import java.awt.geom.Rectangle2D;

/**
 * @author storandt
 */
public class Transformer {
    private final double scale;
    private final Rectangle2D.Double master;
    private final Rectangle2D.Double slave;

    public Transformer(Rectangle2D.Double master, Rectangle2D.Double slave) {
        this.master = master;
        this.slave = slave;
        double scaleWidth = (master.getWidth()) / slave.getWidth();
        double scaleHeight = (master.getHeight()) / slave.getHeight();
        scale = Math.max(scaleWidth, scaleHeight);
    }


    public int tX(int x) {
        return (int)((x - master.x) / scale + slave.x);
    }

    public int tY(int y) {
        return (int)((y - master.y) / scale + slave.y);
    }

}
