/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chrenderclient;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author storandt, niklas
 */
public class ZoomPanel extends JPanel {

    public static final long serialVersionUID = 1;
    private Image smphone;
    private Image tablet;
    Rectangle2D.Double area = new Rectangle2D.Double(17, 44, 302, 469);

    Rectangle localZoomRect = null;
    PrioResult priores;

    boolean showPriorityNodes = false;
    boolean isDisplayTablet = false;
    boolean paintZoomRect = false;


    BasicStroke smallStreetStroke = new BasicStroke(1F);
    BasicStroke mediumStreetStroke = new BasicStroke(1.1F);
    BasicStroke largeStreetStroke = new BasicStroke(1.4F);

    public ZoomPanel() {
        getSmartPhoneImage();
        getTabletImage();
    }

    public void addPaintPoint(Point point) {
        paintPoint(point, this.getGraphics());
    }

    public void reset() {
    }

    private void getSmartPhoneImage() {
        MediaTracker tracker = new MediaTracker(this);
        Toolkit toolkit = getToolkit();
        smphone = toolkit.createImage("images/smphone.png");
        tracker.addImage(smphone, 1);
        try {
            tracker.waitForAll();
        } catch (Exception exception) {
        }
    }

    private void getTabletImage() {
        MediaTracker tracker = new MediaTracker(this);
        Toolkit toolkit = getToolkit();
        tablet = toolkit.createImage("images/tablet.png");
        tracker.addImage(tablet, 1);
        try {
            tracker.waitForAll();
        } catch (Exception exception) {
        }
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
        if (!isDisplayTablet)
            paintPhone(g);
        else
            paintTablet(g);
    }

    public void paintMap(Graphics g) {
        Graphics2D g2D = (Graphics2D) g;
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (priores == null) {
            System.err.println("Priores is null");
            return;
        }
        System.err.println("Got " + priores.edges.size() + " edges");
        for (int i = 0; i < priores.edges.size(); i++) {
            ArrayList<PrioResult.DrawEdge> drawEdges = priores.edges.get(i).draw;
            for (int j = 0; j < drawEdges.size(); j++) {
                PrioResult.DrawEdge drawEdge = drawEdges.get(j);
                g2D.setColor(Color.LIGHT_GRAY);
                g2D.setStroke(smallStreetStroke);

                if (drawEdge.type <= 2) {
                    g2D.setColor(Color.YELLOW);
                    g2D.setStroke(largeStreetStroke);
                } else if (drawEdge.type <= 9) {
                    g2D.setColor(Color.WHITE);
                    g2D.setStroke(mediumStreetStroke);
                }

                g2D.drawLine((int) drawEdge.p1.getX(), (int) drawEdge.p1.getY(), (int) drawEdge.p2.getX(), (int) drawEdge.p2.getY());
            }
        }
    }


    public void paintPhone(Graphics g) {
        g.drawImage(smphone, 0, 0, this);
    }

    public void paintTablet(Graphics g) {
        g.drawImage(tablet, 0, 0, this);
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

        if (!isDisplayTablet)
            g.drawImage(smphone, 0, 0, this);
        else
            g.drawImage(tablet, 0, 0, this);
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
