/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chrenderclient;

import javax.swing.*;
import java.awt.*;

/**
 * @author storandt, schnelle
 */
public class ZoomForm extends javax.swing.JFrame {
    public static final long serialVersionUID = 42;
    private TPClient tp;
    private int coreSize;

    int xBorder = 17;
    int yBorder = 44;
    public int originalX = -1;
    public int originalY = -1;


    public int minPriority = 0;
    boolean justDragged = false;

    public int width = 0;
    public int height = 0;



    /**
     * Creates new form zoomForm
     *
     * @param tp
     */
    public ZoomForm(TPClient tp) {
        coreSize = 1000;
        this.tp = tp;
        initComponents();
        Dimension dim = new Dimension(1900, 1080);
        this.setPreferredSize(dim);
        //this.resize(new Dimension(400, 660));
        final int startLevel = 20;
        priorityLabel.setText(Integer.toString(startLevel));
        prioritySlider.setValue(startLevel);
    }


    /**
     * This method is called from within the constructor to initialize the form.
     */
    private void initComponents() {


        zoomPanel = new ZoomPanel(tp, coreSize);
        priorityLabel = new javax.swing.JLabel();
        fixedMinPrioLabel = new javax.swing.JLabel();
        MenuBar = new javax.swing.JMenuBar();
        ImportMenu = new javax.swing.JMenu();
        LoadCoreItem = new javax.swing.JMenuItem();
        ExportMenu = new javax.swing.JMenu();
        SaveImageItem = new javax.swing.JMenuItem();
        AutoListImageItem = new javax.swing.JMenuItem();
        SelectionMenu = new javax.swing.JMenu();
        ClearMarkerMenuItem = new javax.swing.JMenuItem();
        UndoLastMarkerMenuItem = new javax.swing.JMenuItem();
        AlgorithmsMenu = new javax.swing.JMenu();
        RunDijkstraMenuItem = new javax.swing.JMenuItem();
        OptionMenu = new javax.swing.JMenu();
        ShowPriorityNodesMenuItem = new javax.swing.JMenuItem();
        ShowBundleRectsMenuItem = new javax.swing.JMenuItem();
        ToggleAutoLevelMenuItem = new javax.swing.JMenuItem();
        TestMenu = new javax.swing.JMenu();
        TestCoreLevelSize = new javax.swing.JMenuItem();
        TestBundleCoreSize = new javax.swing.JMenuItem();
        TestBundleRendering = new javax.swing.JMenuItem();
        TestRouting = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        prioritySlider = new javax.swing.JSlider();
        prioritySlider.setMinorTickSpacing(1);
        prioritySlider.setMajorTickSpacing(5);
        prioritySlider.setPaintLabels(true);
        prioritySlider.setMinimum(0);
        prioritySlider.setMaximum(400);
        prioritySlider.setSnapToTicks(false);
        prioritySlider.setPaintTicks(true);
        prioritySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                zoomPanel.prioritySliderStateChanged(evt);
            }
        });

        zoomPanel.setBackground(java.awt.Color.white);


        javax.swing.GroupLayout zoomPanelLayout = new javax.swing.GroupLayout(zoomPanel);
        zoomPanel.setLayout(zoomPanelLayout);
        zoomPanelLayout.setHorizontalGroup(
                zoomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 335, Short.MAX_VALUE)
        );
        zoomPanelLayout.setVerticalGroup(
                zoomPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 556, Short.MAX_VALUE)
        );

        priorityLabel.setText("0");
        priorityLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        fixedMinPrioLabel.setText("min prio");
        fixedMinPrioLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        ImportMenu.setText("Import");

        LoadCoreItem.setText("Load Core only");
        LoadCoreItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoadGraphItemActionPerformed(evt);
            }
        });
        ImportMenu.add(LoadCoreItem);

        MenuBar.add(ImportMenu);

        ExportMenu.setText("Export");

        SaveImageItem.setText("Save As PNG");
        SaveImageItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomPanel.SaveImage(evt);
            }
        });
        AutoListImageItem.setText("Auto Extract Framing List");
        AutoListImageItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomPanel.AutoExtractFramingList(evt);
            }
        });
        ExportMenu.add(SaveImageItem);
        ExportMenu.add(AutoListImageItem);

        MenuBar.add(ExportMenu);

        SelectionMenu.setText("Selection");

        ClearMarkerMenuItem.setText("Clear Marker");
        ClearMarkerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ClearMarkerMenuItemActionPerformed(evt);
            }
        });
        SelectionMenu.add(ClearMarkerMenuItem);

        UndoLastMarkerMenuItem.setText("Undo Last Marker");
        UndoLastMarkerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UndoLastMarkerMenuItemActionPerformed(evt);
            }
        });
        SelectionMenu.add(UndoLastMarkerMenuItem);

        MenuBar.add(SelectionMenu);

        AlgorithmsMenu.setText("Algorithms");

        RunDijkstraMenuItem.setText("Run Dijkstra");
        RunDijkstraMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomPanel.routeRequested(evt);
            }
        });
        AlgorithmsMenu.add(RunDijkstraMenuItem);

        MenuBar.add(AlgorithmsMenu);

        OptionMenu.setText("Options");

        ShowPriorityNodesMenuItem.setText("Show Priority Nodes");
        ShowPriorityNodesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShowPriorityNodesMenuItemActionPerformed(evt);
            }
        });

        ShowBundleRectsMenuItem.setText("Show Bundle Rects");
        ShowBundleRectsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShowBundleRectsNodesMenuItemActionPerformed(evt);
            }
        });

        ToggleAutoLevelMenuItem.setText("Toggle Auto Level");
        ToggleAutoLevelMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomPanel.toggleAutoLevel(evt);
            }
        });
        OptionMenu.add(ShowPriorityNodesMenuItem);
        OptionMenu.add(ShowBundleRectsMenuItem);
        OptionMenu.add(ToggleAutoLevelMenuItem);

        MenuBar.add(OptionMenu);

        TestMenu.setText("Test");
        TestCoreLevelSize.setText("Core vs Size Test (takes long)");
        TestCoreLevelSize.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomPanel.CoreSizeLevelTest(evt);
            }
        });
        TestBundleCoreSize.setText("Bundle Size vs Core Size Test (takes long)");
        TestBundleCoreSize.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomPanel.BundleCoreSizeTest(evt);
            }
        });

        TestBundleRendering.setText("Bundle Rendering vs Level)");
        TestBundleRendering.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomPanel.BundleRenderingTest(evt);
            }
        });
        TestRouting.setText("Routing Test");
        TestRouting.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomPanel.RoutingTest(evt);
            }
        });
        TestMenu.add(TestRouting);
        TestMenu.add(TestBundleCoreSize);
        TestMenu.add(TestBundleRendering);
        TestMenu.add(TestCoreLevelSize);

        MenuBar.add(TestMenu);

        setJMenuBar(MenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                        .addComponent(prioritySlider, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(fixedMinPrioLabel)
                                                                .addComponent(priorityLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addComponent(zoomPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        ));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                        .addComponent(zoomPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                        .addComponent(fixedMinPrioLabel)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(priorityLabel))
                                                .addComponent(prioritySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        ));

        pack();
        zoomPanel.loadCore();
    }// </editor-fold>//GEN-END:initComponents



    private void LoadGraphItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoadGraphItemActionPerformed
        System.out.println("clicked");
        zoomPanel.loadCore();
    }//GEN-LAST:event_LoadGraphItemActionPerformed







    private void ClearMarkerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClearMarkerMenuItemActionPerformed
        zoomPanel.clearPoints();
        repaint();
    }//GEN-LAST:event_ClearMarkerMenuItemActionPerformed

    private void UndoLastMarkerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UndoLastMarkerMenuItemActionPerformed
        repaint();
    }//GEN-LAST:event_UndoLastMarkerMenuItemActionPerformed






    private void ShowPriorityNodesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShowPriorityNodesMenuItemActionPerformed
        zoomPanel.setPriorityNodesVisibility(!zoomPanel.getPriorityNodesVisibility());
        repaint();
    }//GEN-LAST:event_ShowPriorityNodesMenuItemActionPerformed

    private void ShowBundleRectsNodesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShowPriorityNodesMenuItemActionPerformed
        zoomPanel.setBundleRectsVisibility(!zoomPanel.getBundleRectsVisibility());
        repaint();
    }//GEN-LAST:event_ShowPriorityNodesMenuItemActionPerformed

    private void GridTestMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GridTestMenuItemActionPerformed

    }//GEN-LAST:event_GridTestMenuItemActionPerformed


    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        String url = JOptionPane.showInputDialog("Server URL:", "http://plankton:8080");
        final TPClient tp = new TPClient(url);
	    /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ZoomForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ZoomForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ZoomForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ZoomForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ZoomForm(tp).setVisible(true);
            }
        });

    }

    @Override
    public void paint(Graphics g) {
        paintComponents(g);
        if (getBufferStrategy() != null) {
            g = getBufferStrategy().getDrawGraphics();
            zoomPanel.paint(g);
        }
        //getBufferStrategy().show();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu AlgorithmsMenu;
    private javax.swing.JMenuItem ClearMarkerMenuItem;
    private javax.swing.JMenu DisplayMenu;
    private javax.swing.JMenu ExportMenu;
    private javax.swing.JMenu ImportMenu;
    private javax.swing.JMenuItem LoadCoreItem;
    private javax.swing.JMenuBar MenuBar;
    private javax.swing.JMenu OptionMenu;
    private javax.swing.JMenuItem RunDijkstraMenuItem;
    private javax.swing.JMenuItem SaveImageItem;
    private javax.swing.JMenuItem AutoListImageItem;
    private javax.swing.JMenu SelectionMenu;
    private javax.swing.JMenuItem ShowPriorityNodesMenuItem;
    private javax.swing.JMenuItem ShowBundleRectsMenuItem;
    private javax.swing.JMenuItem ToggleAutoLevelMenuItem;
    private javax.swing.JMenu TestMenu;
    private javax.swing.JMenuItem TestCoreLevelSize;
    private javax.swing.JMenuItem TestBundleCoreSize;
    private javax.swing.JMenuItem TestBundleRendering;
    private javax.swing.JMenuItem TestRouting;
    private javax.swing.JMenuItem UndoLastMarkerMenuItem;
    private javax.swing.JLabel fixedMinPrioLabel;
    private javax.swing.JLabel priorityLabel;
    private javax.swing.JSlider prioritySlider;
    private ZoomPanel zoomPanel;
    // End of variables declaration//GEN-END:variables
}
