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

    public Transformer() {
    }

    public double getScaleFactor(Rectangle2D.Double master, Rectangle2D.Double slave) {
        double scaleWidth = (master.getWidth()) / slave.getWidth();
        double scaleHeight = (master.getHeight()) / slave.getHeight();
        return Math.max(scaleWidth, scaleHeight);
    }

    public void transformToScreenSize(Rectangle2D.Double master, Rectangle2D.Double slave, PrioResult priores) {
        double scale = getScaleFactor(master, slave);
        System.err.println(scale);
        for (int i = 0; i < priores.edges.size(); i++) {
            PrioResult.Edge edge = priores.edges.get(i);
            for (int j = 0; j < edge.draw.size(); j++) {
                transformPointForScreen(edge.draw.get(j), slave.x, slave.y, scale, master.x, master.y);
            }
        }
    }

    private void transformPointForScreen(PrioResult.DrawEdge e, double slaveX, double slaveY, double scale, double masterX, double masterY) {
        e.p1.x = (int) ((e.p1.getX() - masterX) / scale + slaveX);
        e.p1.y = (int) ((e.p1.getY() - masterY) / scale + slaveY);
        e.p2.x = (int) ((e.p2.getX() - masterX) / scale + slaveX);
        e.p2.y = (int) ((e.p2.getY() - masterY) / scale + slaveY);
    }
}
