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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author storandt, niklas
 */
public class ZoomPanel extends JPanel {

    public static final long serialVersionUID = 1;
    private Rectangle2D.Double area = new Rectangle2D.Double(17, 44, 302, 469);

    private Rectangle localZoomRect = null;
    private PrioResult priores;
    private CoreGraph core;
    private Transformer trans = new Transformer();
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
    public boolean paintZoomRect = false;
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
        System.err.println("Got " + core.getEdgeCount() + " core edges for " + core.getNodeCount() + " nodes");
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

                g2D.drawLine(trans.transformX(path.getX1(pathElement), bbox, area), trans.transformY(path.getY1(pathElement), bbox, area),
                        trans.transformX(path.getX2(pathElement), bbox, area), trans.transformY(path.getY2(pathElement), bbox, area));
            }
        }
        
        if (priores == null) {
            System.err.println("Priores is null");
            return;
        }
        System.err.println("Got " + priores.edges.size() + " edges");
        trans.transformToScreenSize(bbox, area, priores);
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

    private void setView() {
        int x = 6;
        int y = 49;
        int width = (int) (getWidth() * 50.0);
        int height = (int) (getHeight() * 50.0);
        bbox = new Rectangle2D.Double(x, y, width, height);
    }

    private void extractGraph(Rectangle2D.Double range) {
        Rectangle2D.Double extendedRange = new Rectangle2D.Double();
        double factor = bbox.getWidth() / area.width;
        extendedRange.x = range.x - (extendFactor - 1) / 2 * range.width;
        extendedRange.y = range.y - (extendFactor - 1) / 2 * range.height;
        extendedRange.width = extendFactor * range.width;
        extendedRange.height = extendFactor * range.height;
        if (localZoomRect == null) {
            long time = System.currentTimeMillis();

            //ids = prioDings.getNodeSelection(extendedRange, minPriority);
            try {
                System.err.println("Requesting " + bbox);
                priores = tp.bbBundleRequest(bbox, minPriority);
            } catch (IOException e) {
                e.printStackTrace();
            }
            long time2 = System.currentTimeMillis();
            //System.out.println("PST:" + (time2 - time) + " with " + ids.size());
        } else {
            Rectangle2D.Double local = new Rectangle2D.Double();
            local.x = (int) (bbox.x + (localZoomRect.x - xBorder) * factor);
            local.y = (int) (bbox.y + (localZoomRect.y - yBorder) * factor);
            local.width = (int) (localZoomRect.width * factor);
            local.height = (int) (localZoomRect.height * factor);
            System.out.println(local);
            try {
                System.err.println("Requesting local " + bbox);
                priores = tp.bbBundleRequest(bbox, minPriority);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        originalX = evt.getX();
        originalY = evt.getY();
        System.out.println("new x y: " + originalX + ", " + originalY);
    }

    private void zoomPanelMouseReleased(java.awt.event.MouseEvent evt) {
        if (!area.contains(new Point(evt.getX(), evt.getY()))){
            return;
        }
        int dx = evt.getX() - originalX;
        int dy = evt.getY() - originalY;
        double factor = bbox.getWidth() / area.width;
        if (dx == 0 && dy == 0 && justDragged == false) {
            return;
        }
        justDragged = false;
        dx = (int) (dx * factor);
        dy = (int) (dy * factor);
        System.out.println("deltas: " + dx + ", " + dy);

        bbox.x -= dx;
        bbox.y -= dy;
        extractGraph(bbox);
        repaint();
    }

    private void zoomPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
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

        minPriority = slider.getValue();
        System.out.println("MIN P " + minPriority);
        extractGraph(bbox);
        repaint();
    }

    private void zoomPanelMouseDragged(java.awt.event.MouseEvent evt) {
        zoomPanelMouseReleased(evt);
        originalX = evt.getX();
        originalY = evt.getY();

        justDragged = true;
        repaint();
    }

}
