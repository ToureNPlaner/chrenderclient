/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chrenderclient;

import chrenderclient.clientgraph.*;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author storandt, niklas
 */
public final class ZoomPanel extends JPanel {

    public static final long serialVersionUID = 1;
    private Rectangle2D.Double drawArea = new Rectangle2D.Double(17, 44, 302, 469);

    private BundleCache bundles;
    private Router router;
    private CoreGraph core;
    private BoundingBox bbox = new BoundingBox();
    private TPClient tp;
    private ArrayList<Point> points;
    private ArrayList<DrawData> paths;

    private int coreSize;

    private int xBorder = 17;
    private int yBorder = 44;
    private int originalX = -1;
    private int originalY = -1;

    private int minLen = 10;
    private int maxLen = 40;
    public int lastLevel = Integer.MAX_VALUE;
    private boolean AUTO = true;
    private boolean justDragged = false;

    public boolean showPriorityNodes = false;
    public boolean showBundleRects = false;
    private double extendFactor = 3;
    private double changeFactor = 1.8;


    private static final BasicStroke smallStreetStroke = new BasicStroke(1F);
    private static BasicStroke mediumStreetStroke = new BasicStroke(1.1F);
    private static BasicStroke largeStreetStroke = new BasicStroke(1.4F);
    private static BasicStroke pathStroke = new BasicStroke(2.4F);


    public ZoomPanel(TPClient tpClient, int coreSize) {
        this.core = null;
        this.paths = null;
        this.tp = tpClient;
        this.coreSize = coreSize;
        this.points = new ArrayList<Point>();
        this.bundles = new BundleCache(7, 0.9);
        this.paths = new ArrayList<>();
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
        this.drawArea = new Rectangle2D.Double(0, 0, 1800, 900);
        setView();
    }

    public void addPaintPoint(Point point) {
        final Transformer trans = new Transformer(bbox, drawArea);
        point = new Point(trans.toRealX(point.x), trans.toRealY(point.y));
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

        g.fillRect((int) drawArea.getX(), (int) drawArea.getY(), (int) drawArea.getWidth(), (int) drawArea.getHeight());
        paintMap(g);
        for (Point p : points) {
            paintPoint(p, g);
        }
    }

    public void toggleAutoLevel(ActionEvent evt) {
        this.AUTO = !this.AUTO;
    }

    private static class DrawInfo {
        public String name;
        public String coreRequestSize;
        public BoundingBox bbox;
        public BoundingBox extendedBBox;
        public BoundingBox coreBBox;
        public int coreNodes;
        public int coreEdges;
        public int coreLines;
        public int coreLinesDrawn;
        public java.util.List<BundleDrawInfo> bundles = new ArrayList<>();

        public DrawInfo() {
            name = "urar_map_"+System.currentTimeMillis();
        }
    }

    private static class BundleDrawInfo {
        public String requestSize;
        public int level;
        public int nodes;
        public int upEdges;
        public int downEdges;
        public int linesDrawn;
        public int lines;
        public BoundingBox bundleBBox;
    }

    private void drawCoreNodes(Graphics2D g, Transformer trans) {
        g.setColor(Color.RED);
        for(int i = 0; i < core.getNodeCount(); ++i) {
            g.fillRect(trans.toDrawX(core.getX(i))-2, trans.toDrawY(core.getY(i))-2, 4, 4);
        }
    }

    private void drawBundleRect(Graphics2D g, Transformer trans, Bundle bundle) {
        g.setColor(Color.YELLOW);
        BoundingBox bbox = bundle.bbox;
        g.drawRect(trans.toDrawX(bbox.x), trans.toDrawY(bbox.y), trans.toDrawDist(bbox.width), trans.toDrawDist(bbox.height));
    }

    private void drawBundleNodes(Graphics2D g, Transformer trans, Bundle bundle) {

        for (int i = 0; i < bundle.nodes.length; ++i) {
            Node n = bundle.nodes[i];
            if(n.hasCoordinates()) {
                BoundingBox bbox = bundle.bbox;
                if(!bbox.contains(n.getX(), n.getY())){
                    g.setColor(Color.CYAN);
                } else {
                    g.setColor(Color.YELLOW);
                }
                g.fillRect(trans.toDrawX(n.getX()) - 2, trans.toDrawY(n.getY()) - 2, 4, 4);
            }

        }
    }

    private final Color typeToColor(int type){
        if(type <= 3) {
            return Color.black;
        } else if (type <= 5){
            return Color.lightGray;
        } else if (type <= 6){
            return Color.yellow;
        } else if (type <= 8){
            return Color.blue;
        } else if (type <= 10){
            return Color.cyan;
        } else if (type <= 15){
            return Color.red;
        } else {
            return Color.black;
        }
    }

    private final int drawBundle(Graphics2D g2D, Bundle bundle, Transformer trans, BasicStroke stroke, float hue) {
        int linesDrawn = 0;
        DrawData draw = bundle.getDraw();
        BoundingBox bbox = bundle.bbox;
        for (int drawElement = 0; drawElement < draw.size(); drawElement++) {
            int x1 = draw.getX1(drawElement);
            int y1 = draw.getY1(drawElement);
            int x2 = draw.getX2(drawElement);
            int y2 = draw.getY2(drawElement);
            // Don't draw edges outside the request bbox, they are only for drawing paths
            if(bbox.contains(x1, y1) || bbox.contains(x2, y2) ) {
                int dx1 = trans.toDrawX(x1);
                int dy1 = trans.toDrawY(y1);
                int dx2 = trans.toDrawX(x2);
                int dy2 = trans.toDrawY(y2);
                if (drawArea.contains(dx1, dy1) || drawArea.contains(dx2, dy2)) {
                    g2D.setColor(typeToColor(draw.getType(drawElement)));
                    g2D.setStroke(stroke);
                    g2D.drawLine(dx1, dy1, dx2, dy2);
                    linesDrawn++;
                }
            }
        }
        return linesDrawn;
    }

    private final int draw(Graphics2D g2D, DrawData draw, Transformer trans, BasicStroke stroke, float hue) {
        int linesDrawn = 0;
        for (int drawElement = 0; drawElement < draw.size(); drawElement++) {
            int x1 = trans.toDrawX(draw.getX1(drawElement));
            int y1 = trans.toDrawY(draw.getY1(drawElement));
            int x2 = trans.toDrawX(draw.getX2(drawElement));
            int y2 = trans.toDrawY(draw.getY2(drawElement));
            if (drawArea.contains(x1, y1) || drawArea.contains(x2, y2)) {
                g2D.setColor(typeToColor(draw.getType(drawElement)));
                g2D.setStroke(stroke);
                g2D.drawLine(x1, y1, x2, y2);
                linesDrawn++;
            }
        }
        return linesDrawn;
    }

    public DrawInfo paintMap(Graphics g) {
        Graphics2D g2D = (Graphics2D) g;
        // TODO with Antialiasing drawing is awfully slow
        //g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        //        RenderingHints.VALUE_ANTIALIAS_ON);
        // First paint the core
        final Transformer trans = new Transformer(bbox, drawArea);
        DrawInfo drawInfo = new DrawInfo();
        long start = System.nanoTime();
        if (core == null) {
            System.err.println("core is null");
            return drawInfo;
        }
        drawInfo.coreEdges = core.getEdgeCount();
        drawInfo.coreNodes = core.getNodeCount();
        drawInfo.coreRequestSize = Utils.sizeForHumans(core.requestSize);
        drawInfo.bbox = bbox;
        drawInfo.extendedBBox = computeExtendedBBox(bbox);
        drawInfo.coreBBox = core.getDraw().getBbox();
        drawInfo.coreLines = core.getDraw().size();
        drawInfo.coreLinesDrawn = draw(g2D, core.getDraw(), trans, largeStreetStroke, 0.7f);


        System.out.println(Utils.took("Drawing Core", start));

        if (bundles.isEmpty()) {
            System.err.println("Priores is empty");
            return drawInfo;
        }

        for (Bundle bundle : bundles) {
            // Draw only visible bundles, that are finer than what we currently want
            if (!bundle.bbox.intersect(bbox).isEmpty()
                    && bundle.minLen <= trans.toRealDist(minLen)
                    && bundle.maxLen <= trans.toRealDist(maxLen)) {
                start = System.nanoTime();
                BundleDrawInfo bundleDraw = new BundleDrawInfo();
                bundleDraw.requestSize = Utils.sizeForHumans(bundle.requestSize);
                bundleDraw.level = bundle.level;
                bundleDraw.nodes = bundle.nodes.length;
                bundleDraw.bundleBBox = bundle.bbox;
                bundleDraw.upEdges = bundle.upEdges.length;
                bundleDraw.downEdges = bundle.downEdges.length;
                bundleDraw.lines = bundle.getDraw().size();
                bundleDraw.linesDrawn = drawBundle(g2D, bundle, trans, mediumStreetStroke, 0.6f);
                System.out.println(Utils.took("Drawing Bundle", start));
                drawInfo.bundles.add(bundleDraw);
                //bundleBaseColor = bundleBaseColor.darker();
                if (showPriorityNodes) {
                    drawBundleNodes(g2D, trans, bundle);
                }
                if (showBundleRects) {
                    drawBundleRect(g2D, trans, bundle);
                }
            }
        }


        if (paths != null) {
            for(DrawData path : paths) {
                start = System.nanoTime();
                draw(g2D, path, trans, pathStroke, 0.3F);
                System.out.println(Utils.took("Drawing Path", start));
            }
        }
        if(showPriorityNodes) {
            drawCoreNodes(g2D, trans);
        }
        return drawInfo;
    }

    private void paintPoint(Point point, Graphics g) {
        final Transformer trans = new Transformer(bbox, drawArea);
        g.setColor(Color.BLUE);
        g.fillRect(trans.toDrawX((int) point.getX())-2, trans.toDrawY((int) point.getY())-2, 5, 5);
    }

    private void saveImageInfo(String name, DrawInfo info) {
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(name));
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(out,info);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    DrawInfo saveImage(String fileName) throws IOException {
        BufferedImage img = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.setColor(new Color(255, 205, 205));
        g.fillRect((int) drawArea.getX(), (int) drawArea.getY(), (int) drawArea.getWidth(), (int) drawArea.getHeight());
        Graphics2D g2D = (Graphics2D) g;
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        DrawInfo info = paintMap(g2D);
        ImageIO.write(img, "png", new File(fileName));
        return info;
    }

    public void paintRectangleSelectionTool(Graphics g) {
        g.setColor(Color.PINK);
        g.drawRect((int) (drawArea.width + drawArea.x - 30), (int) (drawArea.y + 10), 20, 20);
        g.setColor(Color.BLACK);
        g.drawString("R", (int) (drawArea.width + drawArea.x - 23), (int) drawArea.y + 25);
    }

    boolean clickedOnRectangleSelection(int x, int y) {
        if (x < drawArea.width + drawArea.x - 30)
            return false;
        if (x > drawArea.width + drawArea.x - 10)
            return false;
        if (y < drawArea.y + 10)
            return false;
        if (y > drawArea.y + 30)
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
        bbox = new BoundingBox(x, y, width, height);
    }

    private void extractGraph(BoundingBox bbox) {
        long start = System.nanoTime();
        BoundingBox extendedBBox = computeExtendedBBox(bbox);

        try {
            System.err.println("Requesting " + extendedBBox);
            // TODO proper multi bundle management
            final Transformer t = new Transformer(this.bbox, drawArea);
            Bundle freshBundle = tp.bbBundleRequest(extendedBBox, coreSize, lastLevel, t.toRealDist(minLen), t.toRealDist(maxLen), 0.01, AUTO);
            this.lastLevel = freshBundle.level;
            bundles.offer(freshBundle);

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(Utils.took("extractGraph", start));

    }

    private BoundingBox computeExtendedBBox(BoundingBox bbox) {
        BoundingBox extendedBBox = new BoundingBox();
        extendedBBox.x = (int) (bbox.x - (extendFactor - 1) / 2 * bbox.width);
        extendedBBox.y = (int) (bbox.y - (extendFactor - 1) / 2 * bbox.height);
        extendedBBox.width = (int) (extendFactor * bbox.width);
        extendedBBox.height = (int) (extendFactor * bbox.height);
        return extendedBBox;
    }

    public void loadCore() {
        try {
            if(bbox.width < 10 || bbox.height < 10) {
                setView();
            }
            core = tp.coreRequest(coreSize, 50, 1000, 0.01);
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
        if (evt.getButton() == 3 && drawArea.contains(pointOnCanvas)) {
            addPaintPoint(pointOnCanvas);
        }
    }

    private void zoomPanelMouseReleased(java.awt.event.MouseEvent evt) {
        System.out.println("Mouse released");
        if (!drawArea.contains(new Point(evt.getX(), evt.getY()))) {
            return;
        }
        int dx = evt.getX() - originalX;
        int dy = evt.getY() - originalY;

        extractGraph(bbox);
        if (dx == 0 && dy == 0 && !justDragged) {
            return;
        }
        justDragged = false;
        final Transformer t = new Transformer(bbox, drawArea);
        dx = t.toRealDist(dx);
        dy = t.toRealDist(dy);

        System.out.println("deltas: " + dx + ", " + dy);

        bbox.x -= dx;
        bbox.y -= dy;
        repaint();
    }

    private void zoomPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
        System.out.println("Mouse wheel moved");
        int notches = evt.getWheelRotation();
        System.out.println("notch: " + notches);
        int dx = evt.getX() - xBorder;
        int dy = evt.getY() - yBorder;

        Transformer t = new Transformer(bbox, drawArea);
        dx = t.toRealDist(dx);
        dy = t.toRealDist(dy);
        System.out.println("pos: " + dx + ", " + dy);

        if (notches > 0) {
            bbox.x -= (changeFactor - 1) * dx;
            bbox.y -= (changeFactor - 1) * dy;
            bbox.width *= changeFactor;
            bbox.height *= changeFactor;
        } else {
            if (bbox.width > 100 && bbox.height > 100) {
                bbox.x += ((double)dx * (1 - 1.0 / changeFactor));
                bbox.y += ((double)dy * (1 - 1.0 / changeFactor));
                bbox.width = (int) (bbox.width / changeFactor);
                bbox.height = (int) (bbox.height / changeFactor);
            }
        }
        extractGraph(bbox);
        repaint();
    }

    public String fileDialog() {
        JFileChooser fc = new JFileChooser();
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.CANCEL_OPTION) return "";
        try {
            File file = fc.getSelectedFile();
            return file.toString();

        } catch (Exception e) {
            return "fileDialog";
        }
    }


    public void SaveImage(java.awt.event.ActionEvent evt) {
        try {
            String name = JOptionPane.showInputDialog("Frame Name:");;
            DrawInfo info = saveImage(name+".png");
            info.name = name;
            saveImageInfo(name+"_info.json", info);
        } catch (IOException ex) {
            Logger.getLogger(ZoomForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static class Framing {
        public String name;
        public int coreSize;
        public int level;
        public BoundingBox bbox;
    }

    public void AutoExtractFramingList(java.awt.event.ActionEvent evt) {
        try {
            String listFile = fileDialog();
            InputStream in = new BufferedInputStream(new FileInputStream(listFile));
            ObjectMapper mapper = new ObjectMapper();
            java.util.List<Framing> framingList = mapper.readValue(in, new TypeReference<java.util.List<Framing>>(){});
            loadCore();
            for (Framing frame: framingList){
                this.bbox = frame.bbox;
                this.lastLevel = (AUTO)?Integer.MAX_VALUE:frame.level;
                this.coreSize = frame.coreSize;
                extractGraph(bbox);
                DrawInfo info = saveImage(frame.name+".png");
                info.name = frame.name;
                saveImageInfo(frame.name+"_info.json", info);
            }
        } catch (IOException ex) {
            Logger.getLogger(ZoomForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void prioritySliderStateChanged(javax.swing.event.ChangeEvent evt) {
        JSlider slider = (JSlider) evt.getSource();
        if (!slider.getValueIsAdjusting()) {

            lastLevel = slider.getValue();
            System.out.println("MIN P " + lastLevel);
            extractGraph(bbox);
            repaint();
        }
    }

    private void zoomPanelMouseDragged(java.awt.event.MouseEvent evt) {

        if (!drawArea.contains(new Point(evt.getX(), evt.getY()))) {
            return;
        }
        System.out.println("Mouse dragged");
        int dx = evt.getX() - originalX;
        int dy = evt.getY() - originalY;
        if (dx == 0 && dy == 0) {
            return;
        }

        final Transformer t = new Transformer(bbox, drawArea);
        dx = t.toRealDist(dx);
        dy = t.toRealDist(dy);
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
            DrawData path = (router != null)? router.route(pSrc.x, pSrc.y, pTrgt.x, pTrgt.y) : null;
            System.out.println(Utils.took("route", start));
            if (path == null) {
                JOptionPane.showMessageDialog(null, "Path not found", "Routing unsuccesfull", JOptionPane.INFORMATION_MESSAGE);
                break;
            }
            paths.add(path);

        }
        repaint();
    }

    public void clearPoints() {
        points.clear();
    }
}
