/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chrenderclient;

import chrenderclient.clientgraph.CoreGraph;
import chrenderclient.clientgraph.PrioResult;
import chrenderclient.clientgraph.RefinedPath;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author storandt, niklas
 */
public class ZoomPanel extends JPanel {

    public static final long serialVersionUID = 1;
    Rectangle2D.Double area = new Rectangle2D.Double(17, 44, 302, 469);

    Rectangle localZoomRect = null;
    public PrioResult priores;
    public CoreGraph core;
    public Transformer trans = new Transformer();
    Rectangle2D.Double view = new Rectangle2D.Double();

    boolean showPriorityNodes = false;
    boolean paintZoomRect = false;


    BasicStroke smallStreetStroke = new BasicStroke(1F);
    BasicStroke mediumStreetStroke = new BasicStroke(1.1F);
    BasicStroke largeStreetStroke = new BasicStroke(1.4F);

    public ZoomPanel() {
        this.priores = null;
        this.core = null;
    }

    public void addPaintPoint(Point point) {
        paintPoint(point, this.getGraphics());
    }

    public void reset() {
    }

    private AlphaComposite makeComposite(float alpha) {
        int type = AlphaComposite.SRC_OVER;
        return (AlphaComposite.getInstance(type, alpha));
    }

    @Override
    public void paint(Graphics g) {
        g.clearRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(255, 205, 205));

        g.fillRect((int) area.getX(), (int) area.getY(), (int) area.getWidth(), (int) area.getHeight());
        paintMap(g);
        paintRectangleSelectionTool(g);
        if (paintZoomRect) {
            if (localZoomRect != null)
                g.drawRect(localZoomRect.x, localZoomRect.y, localZoomRect.width, localZoomRect.height);
        }
    }

    public void paintMap(Graphics g) {
        Graphics2D g2D = (Graphics2D) g;
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        // First paint the core
        if(core == null) {
            System.err.println("core is null");
            return;
        }
        System.err.println("Got " + core.getEdgeCount() + " core edges for "+core.getNodeCount()+" nodes");
        for (int i = 0; i < core.getEdgeCount(); i++) {
            RefinedPath path = core.getRefinedPath(i);
            for (int pathElement = 0; pathElement < path.size(); pathElement++) {
                g2D.setColor(Color.LIGHT_GRAY);
                g2D.setStroke(smallStreetStroke);

                if (path.getType(pathElement) <= 2) {
                    g2D.setColor(Color.GREEN);
                    g2D.setStroke(largeStreetStroke);
                } else if (path.getType(pathElement) <= 9) {
                    g2D.setColor(Color.BLUE);
                    g2D.setStroke(mediumStreetStroke);
                }

                g2D.drawLine(trans.transformX(path.getX1(pathElement), view, area), trans.transformY(path.getY1(pathElement), view, area),
                        trans.transformX(path.getX2(pathElement), view, area), trans.transformY(path.getY2(pathElement), view, area));
            }
        }
        
        if (priores == null) {
            System.err.println("Priores is null");
            return;
        }
        System.err.println("Got " + priores.edges.size() + " edges");
        trans.transformToScreenSize(view, area, priores);
        for (int i = 0; i < priores.edges.size(); i++) {
            RefinedPath path = priores.edges.get(i).path;
            for (int pathElement = 0; pathElement < path.size(); pathElement++) {
                g2D.setColor(Color.LIGHT_GRAY);
                g2D.setStroke(smallStreetStroke);

                if (path.getType(pathElement) <= 2) {
                    g2D.setColor(Color.YELLOW);
                    g2D.setStroke(largeStreetStroke);
                } else if (path.getType(pathElement) <= 9) {
                    g2D.setColor(Color.WHITE);
                    g2D.setStroke(mediumStreetStroke);
                }

                g2D.drawLine(path.getX1(pathElement), path.getY1(pathElement), path.getX2(pathElement), path.getY2(pathElement));
            }
        }
    }

    private void paintPoint(Point point, Graphics g) {
        g.setColor(Color.BLUE);
        if (area.contains(point))
            g.drawRect((int) point.getX(), (int) point.getY(), 1, 1);
    }

    void saveImage(String fileName) throws IOException {
        BufferedImage img = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.setColor(new Color(255, 205, 205));
        g.fillRect((int) area.getX(), (int) area.getY(), (int) area.getWidth(), (int) area.getHeight());
        Graphics2D g2D = (Graphics2D) g;
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        ImageIO.write(img, "png", new File(fileName));
    }

    public void paintRectangleSelectionTool(Graphics g) {
        g.setColor(Color.PINK);
        g.drawRect((int) (area.width + area.x - 30), (int) (area.y + 10), 20, 20);
        g.setColor(Color.BLACK);
        g.drawString("R", (int) (area.width + area.x - 23), (int) area.y + 25);
    }

    boolean clickedOnRectangleSelection(int x, int y) {
        if (x < area.width + area.x - 30)
            return false;
        if (x > area.width + area.x - 10)
            return false;
        if (y < area.y + 10)
            return false;
        if (y > area.y + 30)
            return false;
        return true;
    }

    void addRectangle(Rectangle rectangle) {
        localZoomRect = rectangle;
        paintZoomRect = true;
    }

    private void drawArrow(Graphics2D g2D, int x, int y, int xx, int yy) {
        if (Math.sqrt((x - xx) * (x - xx) + (y - yy) * (y - yy)) < 20)
            return;
        int newX = ((x + xx) / 2);
        int newY = ((y + yy) / 2);
        drawArrowHead(g2D, new Point(newX, newY), new Point(x, y));
    }

    private void drawArrowHead(Graphics2D g2, Point tip, Point tail) {
        double phi = Math.toRadians(40);
        int barb = 5;
        double dy = tip.y - tail.y;
        double dx = tip.x - tail.x;
        double theta = Math.atan2(dy, dx);
        //System.out.println("theta = " + Math.toDegrees(theta));  
        double x, y, rho = theta + phi;
        for (int j = 0; j < 2; j++) {
            x = tip.x - barb * Math.cos(rho);
            y = tip.y - barb * Math.sin(rho);
            g2.draw(new Line2D.Double(tip.x, tip.y, x, y));
            rho = theta - phi;
        }
    }

}
