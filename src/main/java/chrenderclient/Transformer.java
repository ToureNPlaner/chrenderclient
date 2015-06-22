/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chrenderclient;

import chrenderclient.clientgraph.BoundingBox;

import java.awt.geom.Rectangle2D;

/**
 * @author storandt
 */
public class Transformer {
    private final double scale;
    private final BoundingBox bbox;
    private final Rectangle2D.Double drawArea;

    public Transformer(BoundingBox bbox, Rectangle2D.Double drawArea) {
        this.bbox = bbox;
        this.drawArea = drawArea;
        double scaleWidth = (bbox.width) / drawArea.getWidth();
        double scaleHeight = (bbox.height) / drawArea.getHeight();
        scale = Math.max(scaleWidth, scaleHeight);
    }

    public int toRealDist(int d) {
        return (int) (d*scale);
    }

    public int toDrawDist(int d) {
        return (int) (d/scale);
    }


    public int toDrawX(int x) {
        return (int) ((x - bbox.x) / scale + drawArea.x);
    }

    public int toDrawY(int y) {
        return (int) ((y - bbox.y) / scale + drawArea.y);
    }

    public int toRealX(int x) {
        return (int) (bbox.x + (x - drawArea.x) * scale);
    }

    public int toRealY(int y) {
        return (int) (bbox.y + (y - drawArea.y) * scale);
    }

}
