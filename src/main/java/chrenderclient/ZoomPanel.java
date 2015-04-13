/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chrenderclient;

import chrenderclient.clientgraph.CoreGraph;
import chrenderclient.clientgraph.Bundle;
import chrenderclient.clientgraph.RefinedPath;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author storandt, niklas
 */
public class ZoomPanel extends JPanel {

    public static final long serialVersionUID = 1;
    private Rectangle2D.Double area = new Rectangle2D.Double(17, 44, 302, 469);

    private Bundle priores;
    private CoreGraph core;
    private Rectangle2D.Double bbox = new Rectangle2D.Double();
    private TPClient tp;

    private int coreSize;

    private int xBorder = 17;
    private int yBorder = 44;
    private int originalX = -1;
    private int originalY = -1;


    public int minPriority = 0;
    private boolean justDragged = false;

    public boolean showPriorityNodes = false;
    private double extendFactor = 5;
    private double changeFactor = 1.8;


    private static final BasicStroke smallStreetStroke = new BasicStroke(1F);
    private static BasicStroke mediumStreetStroke = new BasicStroke(1.1F);
    private static BasicStroke largeStreetStroke = new BasicStroke(1.4F);

    public ZoomPanel(TPClient tpClient, int coreSize) {
        this.priores = null;
        this.core = null;
        this.tp = tpClient;
        this.coreSize = coreSize;

        this.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                zoomPanelMouseWheelMoved(evt);
            }
        });

        this.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                zoomPanelMousePressed(evt);
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                zoomPanelMouseReleased(evt);
            }
        });

        this.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                zoomPanelMouseDragged(evt);
            }
        });

        this.setPreferredSize(new Dimension(1800, 900));
        this.area = new Rectangle2D.Double(0, 0, 1800, 900);
        setView();
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
    }

    public void paintMap(Graphics g) {
        Graphics2D g2D = (Graphics2D) g;
        // TODO with Antialiasing drawing is awfully slow
        //g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        //        RenderingHints.VALUE_ANTIALIAS_ON);
        // First paint the core
        final Transformer trans = new Transformer(bbox, area);
        int coreLines = 0;
        long time0 = System.nanoTime();
        if (core == null) {
            System.err.println("core is null");
            return;
        }
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
                coreLines++;
                g2D.drawLine(trans.tX(path.getX1(pathElement)), trans.tY(path.getY1(pathElement)),
                        trans.tX(path.getX2(pathElement)), trans.tY(path.getY2(pathElement)));
            }
        }

        long time1 = System.nanoTime();
        double coreTime = (time1 - time0) / 1000000.0;

        if (priores == null) {
            System.err.println("Priores is null");
            return;
        }
        time0 = System.nanoTime();
        int prioresUpLines = 0;
        for (int i = 0; i < priores.upEdges.length; i++) {
            RefinedPath path = priores.upEdges[i].path;
            for (int pathElement = 0; pathElement < path.size(); pathElement++) {
                g2D.setColor(Color.YELLOW);
                g2D.setStroke(mediumStreetStroke);

                /*if (path.getType(pathElement) <= 2) {
                    g2D.setColor(Color.YELLOW);
                    g2D.setStroke(largeStreetStroke);
                } else if (path.getType(pathElement) <= 9) {
                    g2D.setColor(Color.WHITE);
                    g2D.setStroke(mediumStreetStroke);
                }*/
                prioresUpLines++;
                g2D.drawLine(trans.tX(path.getX1(pathElement)), trans.tY(path.getY1(pathElement)),
                        trans.tX(path.getX2(pathElement)), trans.tY(path.getY2(pathElement)));
            }
        }
        int prioresDownLines = 0;
        for (int i = 0; i < priores.downEdges.length; i++) {
            RefinedPath path = priores.downEdges[i].path;
            for (int pathElement = 0; pathElement < path.size(); pathElement++) {
                g2D.setColor(Color.RED);
                g2D.setStroke(mediumStreetStroke);

                /*if (path.getType(pathElement) <= 2) {
                    g2D.setColor(Color.YELLOW);
                    g2D.setStroke(largeStreetStroke);
                } else if (path.getType(pathElement) <= 9) {
                    g2D.setColor(Color.WHITE);
                    g2D.setStroke(mediumStreetStroke);
                }*/
                prioresDownLines++;
                g2D.drawLine(trans.tX(path.getX1(pathElement)), trans.tY(path.getY1(pathElement)),
                        trans.tX(path.getX2(pathElement)), trans.tY(path.getY2(pathElement)));
            }
        }
        time1 = System.nanoTime();
        double prioresTime = (time1 - time0) / 1000000.0;
        System.out.println("Drew " + core.getEdgeCount() + " coreEdges with " + coreLines + " lines in " + coreTime + " ms and\n" +
                priores.upEdges.length + "(" + prioresUpLines + ") PrioRes upEdges and " + priores.downEdges.length + "(" + prioresDownLines + ") downEdges in " + prioresTime + " ms");
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

    private void setView() {
        int x = 6;
        int y = 49;
        int width = (int) (getWidth() * 50.0);
        int height = (int) (getHeight() * 50.0);
        bbox = new Rectangle2D.Double(x, y, width, height);
    }

    private void extractGraph(Rectangle2D.Double range) {
        long time = System.nanoTime();
        Rectangle2D.Double extendedRange = new Rectangle2D.Double();
        extendedRange.x = range.x - (extendFactor - 1) / 2 * range.width;
        extendedRange.y = range.y - (extendFactor - 1) / 2 * range.height;
        extendedRange.width = extendFactor * range.width;
        extendedRange.height = extendFactor * range.height;

        try {
            System.err.println("Requesting " + extendedRange);
            priores = tp.bbBundleRequest(extendedRange, minPriority);
        } catch (IOException e) {
            e.printStackTrace();
        }
        long time2 = System.nanoTime();
        System.out.println("extractGraph: " + (double) (time2 - time) / 1000000.0 + " ms");

    }

    public void loadCore() {
        try {
            setView();
            core = tp.getCore(coreSize);
            extractGraph(bbox);
            repaint();

        } catch (IOException e) {
            // TODO do something useful
            e.printStackTrace();
        }
    }

    private void zoomPanelMousePressed(java.awt.event.MouseEvent evt) {
        System.out.println("Mouse pressed");
        originalX = evt.getX();
        originalY = evt.getY();
        System.out.println("new x y: " + originalX + ", " + originalY);
    }

    private void zoomPanelMouseReleased(java.awt.event.MouseEvent evt) {
        System.out.println("Mouse released");
        if (!area.contains(new Point(evt.getX(), evt.getY()))) {
            return;
        }
        int dx = evt.getX() - originalX;
        int dy = evt.getY() - originalY;
        double factor = bbox.getWidth() / area.width;
        extractGraph(bbox);
        if (dx == 0 && dy == 0 && !justDragged) {
            return;
        }
        justDragged = false;
        dx = (int) (dx * factor);
        dy = (int) (dy * factor);
        System.out.println("deltas: " + dx + ", " + dy);

        bbox.x -= dx;
        bbox.y -= dy;
        repaint();
    }

    private void zoomPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
        System.out.println("Mouse wheel moved");
        int notches = evt.getWheelRotation();
        System.out.println("notch: " + notches);
        double x = evt.getX() - xBorder;
        double y = evt.getY() - yBorder;
        System.out.println("pos: " + x + ", " + y);
        double factor = bbox.getWidth() / area.width;
        x = x * factor;
        y = y * factor;
        if (notches > 0) {
            bbox.x -= (changeFactor - 1) * x;
            bbox.y -= (changeFactor - 1) * y;
            bbox.width *= changeFactor;
            bbox.height *= changeFactor;
        } else {

            if (bbox.width > 100 && bbox.height > 100) {
                bbox.x += x * (1 - 1.0 / changeFactor);
                bbox.y += y * (1 - 1.0 / changeFactor);
                bbox.width = bbox.width / changeFactor;
                bbox.height = bbox.height / changeFactor;
            }
        }
        extractGraph(bbox);
        repaint();
    }

    public String save() {
        JFileChooser fc = new JFileChooser();
        int result = fc.showSaveDialog(this);
        if (result == JFileChooser.CANCEL_OPTION) return "";
        try {
            File file = fc.getSelectedFile();
            return file.toString();

        } catch (Exception e) {
            return "";
        }
    }

    public void SaveImage(java.awt.event.ActionEvent evt) {
        try {
            String name = save();
            if ("".equals(name))
                name = bbox.width / 1000.0 + "x" + bbox.height / 1000.0 + "_P" + minPriority + ".png";
            saveImage(name);
        } catch (IOException ex) {
            Logger.getLogger(ZoomForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void prioritySliderStateChanged(javax.swing.event.ChangeEvent evt) {
        JSlider slider = (JSlider) evt.getSource();
        if (!slider.getValueIsAdjusting()) {

            minPriority = slider.getValue();
            System.out.println("MIN P " + minPriority);
            extractGraph(bbox);
            repaint();
        }
    }

    private void zoomPanelMouseDragged(java.awt.event.MouseEvent evt) {

        if (!area.contains(new Point(evt.getX(), evt.getY()))) {
            return;
        }
        System.out.println("Mouse dragged");
        int dx = evt.getX() - originalX;
        int dy = evt.getY() - originalY;
        if (dx == 0 && dy == 0) {
            return;
        }
        double factor = bbox.getWidth() / area.width;
        dx = (int) (dx * factor);
        dy = (int) (dy * factor);
        System.out.println("deltas: " + dx + ", " + dy);

        bbox.x -= dx;
        bbox.y -= dy;
        originalX = evt.getX();
        originalY = evt.getY();
        justDragged = true;
        repaint();
    }

}
