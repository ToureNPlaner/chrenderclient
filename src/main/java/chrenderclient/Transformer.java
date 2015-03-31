/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chrenderclient;

import chrenderclient.clientgraph.Edge;
import chrenderclient.clientgraph.PrioResult;
import chrenderclient.clientgraph.RefinedPath;

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
            Edge edge = priores.edges.get(i);
            for (int pathElement = 0; pathElement < edge.path.size(); pathElement++) {
                transformPointForScreen(edge.path, pathElement, slave.x, slave.y, scale, master.x, master.y);
            }
        }
    }

    public int transformX(int x, Rectangle2D.Double master, Rectangle2D.Double slave) {
        double scale = getScaleFactor(master, slave);
        return (int)((x - master.x) / scale + slave.x);
    }

    public int transformY(int y, Rectangle2D.Double master, Rectangle2D.Double slave) {
        double scale = getScaleFactor(master, slave);
        return (int)((y - master.y) / scale + slave.y);
    }

    private void transformPointForScreen(RefinedPath path, int pos, double slaveX, double slaveY, double scale, double masterX, double masterY) {
        path.setX1(pos, (int)((path.getX1(pos) - masterX) / scale + slaveX));
        path.setY1(pos, (int)((path.getY1(pos) - masterY) / scale + slaveY));

        path.setX2(pos, (int)((path.getX2(pos) - masterX) / scale + slaveX));
        path.setY2(pos, (int)((path.getY2(pos) - masterY) / scale + slaveY));
    }
}
