/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chrenderclient;

import chrenderclient.clientgraph.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author storandt, niklas
 */
public class ZoomPanel extends JPanel {

    public static final long serialVersionUID = 1;
    private Rectangle2D.Double area = new Rectangle2D.Double(17, 44, 302, 469);

    private ArrayList<Bundle> bundles;
    private Router router;
    private CoreGraph core;
    private Rectangle2D.Double bbox = new Rectangle2D.Double();
    private TPClient tp;
    private ArrayList<Point> points;
    private ArrayDeque<RefinedPath> paths;

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
        this.core = null;
        this.paths = null;
        this.tp = tpClient;
        this.coreSize = coreSize;
        this.points = new ArrayList<Point>();
        this.bundles = new ArrayList<Bundle>();
        this.paths = new ArrayDeque<RefinedPath>();
        this.router = null;

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
        final Transformer trans = new Transformer(bbox, area);
        point = new Point(trans.toMasterX(point.x), trans.toMasterY(point.y));
        paintPoint(point, this.getGraphics());
        points.add(point);
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
        for (Point p : points) {
            paintPoint(p, g);
        }
    }

    public void paintMap(Graphics g) {
        Graphics2D g2D = (Graphics2D) g;
        // TODO with Antialiasing drawing is awfully slow
        //g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        //        RenderingHints.VALUE_ANTIALIAS_ON);
        // First paint the core
        final Transformer trans = new Transformer(bbox, area);
        int coreLines = 0;
        long start = System.nanoTime();
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
                g2D.drawLine(trans.toSlaveX(path.getX1(pathElement)), trans.toSlaveY(path.getY1(pathElement)),
                        trans.toSlaveX(path.getX2(pathElement)), trans.toSlaveY(path.getY2(pathElement)));
            }
        }
        System.out.println(Utils.took("Drawing Core", start));

        if (bundles.isEmpty()) {
            System.err.println("Priores is null");
            return;
        }
        start = System.nanoTime();
        int prioresUpLines = 0;
        Color bundleBaseColor = Color.YELLOW;
        for (Bundle bundle : bundles) {
            for (int i = 0; i < bundle.upEdges.length; i++) {
                RefinedPath path = bundle.upEdges[i].path;
                for (int pathElement = 0; pathElement < path.size(); pathElement++) {
                    g2D.setColor(bundleBaseColor);
                    g2D.setStroke(mediumStreetStroke);

                /*if (paths.getType(pathElement) <= 2) {
                    g2D.setColor(Color.YELLOW);
                    g2D.setStroke(largeStreetStroke);
                } else if (paths.getType(pathElement) <= 9) {
                    g2D.setColor(Color.WHITE);
                    g2D.setStroke(mediumStreetStroke);
                }*/
                    prioresUpLines++;
                    g2D.drawLine(trans.toSlaveX(path.getX1(pathElement)), trans.toSlaveY(path.getY1(pathElement)),
                            trans.toSlaveX(path.getX2(pathElement)), trans.toSlaveY(path.getY2(pathElement)));
                }
            }

            int prioresDownLines = 0;
            for (int i = 0; i < bundle.downEdges.length; i++) {
                RefinedPath path = bundle.downEdges[i].path;
                for (int pathElement = 0; pathElement < path.size(); pathElement++) {
                    g2D.setColor(bundleBaseColor);
                    g2D.setStroke(mediumStreetStroke);

                /*if (paths.getType(pathElement) <= 2) {
                    g2D.setColor(Color.YELLOW);
                    g2D.setStroke(largeStreetStroke);
                } else if (paths.getType(pathElement) <= 9) {
                    g2D.setColor(Color.WHITE);
                    g2D.setStroke(mediumStreetStroke);
                }*/
                    prioresDownLines++;
                    g2D.drawLine(trans.toSlaveX(path.getX1(pathElement)), trans.toSlaveY(path.getY1(pathElement)),
                            trans.toSlaveX(path.getX2(pathElement)), trans.toSlaveY(path.getY2(pathElement)));
                }
            }
            System.out.println(Utils.took("Drawing Bundles", start));
            System.out.println("Drew " + core.getEdgeCount() + " coreEdges with " + coreLines + " lines\n" +
                    bundle.upEdges.length + "(" + prioresUpLines + ") PrioRes upEdges and " + bundle.downEdges.length + "(" + prioresDownLines + ") downEdges");
            bundleBaseColor = bundleBaseColor.darker();
        }


        if (paths != null) {
            for (RefinedPath path : paths) {
                for (int pathElement = 0; pathElement < path.size(); pathElement++) {
                    g2D.setColor(Color.BLUE);
                    g2D.setStroke(largeStreetStroke);
                    g2D.drawLine(trans.toSlaveX(path.getX1(pathElement)), trans.toSlaveY(path.getY1(pathElement)),
                            trans.toSlaveX(path.getX2(pathElement)), trans.toSlaveY(path.getY2(pathElement)));
                }
            }
        }
    }

    private void paintPoint(Point point, Graphics g) {
        final Transformer trans = new Transformer(bbox, area);
        g.setColor(Color.BLUE);
        g.drawRect(trans.toSlaveX((int) point.getX()), trans.toSlaveY((int) point.getY()), 2, 2);
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

        if (Math.hypot(x - xx, y - yy) < 20)
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
        long start = System.nanoTime();
        Rectangle2D.Double extendedRange = new Rectangle2D.Double();
        extendedRange.x = range.x - (extendFactor - 1) / 2 * range.width;
        extendedRange.y = range.y - (extendFactor - 1) / 2 * range.height;
        extendedRange.width = extendFactor * range.width;
        extendedRange.height = extendFactor * range.height;

        try {
            System.err.println("Requesting " + extendedRange);
            // TODO proper multi bundle management
            if(bundles.isEmpty()){
                bundles.add(tp.bbBundleRequest(extendedRange, minPriority));
            } else {
                bundles.set(0, tp.bbBundleRequest(extendedRange, minPriority));
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(Utils.took("extractGraph", start));

    }

    public void loadCore() {
        try {
            setView();
            core = tp.getCore(coreSize);
            this.router = new Router(core, bundles);
            extractGraph(bbox);
            repaint();

        } catch (IOException e) {
            // TODO do something useful
            e.printStackTrace();
        }
    }

    private void zoomPanelMousePressed(java.awt.event.MouseEvent evt) {
        System.out.println("Mouse pressed, Button: " + evt.getButton());
        originalX = evt.getX();
        originalY = evt.getY();
        System.out.println("new x y: " + originalX + ", " + originalY);
        Point pointOnCanvas = new Point(evt.getX(), evt.getY());
        if (evt.getButton() == 3 && area.contains(pointOnCanvas)) {
            addPaintPoint(pointOnCanvas);
        }
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

        if (notches > 0) {
            // TODO translate points
            x = x * factor;
            y = y * factor;
            bbox.x -= (changeFactor - 1) * x;
            bbox.y -= (changeFactor - 1) * y;
            bbox.width *= changeFactor;
            bbox.height *= changeFactor;
        } else {

            if (bbox.width > 100 && bbox.height > 100) {
                // TODO translate points
                x = x * factor;
                y = y * factor;
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

    public void routeRequested(java.awt.event.ActionEvent evt) {
        System.out.println("Route requested");
        paths.clear();
        for (int pointNum = 0; pointNum < points.size() - 1; ++pointNum) {
            Point pSrc = points.get(pointNum);
            Point pTrgt = points.get(pointNum + 1);
            long start = System.nanoTime();
            ArrayDeque<RefinedPath> path = (router != null)? router.route(pSrc.x, pSrc.y, pTrgt.x, pTrgt.y) : null;
            System.out.println(Utils.took("route", start));
            if (path == null) {
                JOptionPane.showMessageDialog(null, "Path not found", "Routing unsuccesfull", JOptionPane.INFORMATION_MESSAGE);
                break;
            }
            paths.addAll(path);

        }
        repaint();
    }

    public void clearPoints() {
        points.clear();
    }
}
