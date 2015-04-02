/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chrenderclient;

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
        coreSize = 400;
        this.tp = tp;
        initComponents();
        Dimension dim = new Dimension(1900, 1000);
        this.setPreferredSize(dim);
        //this.resize(new Dimension(400, 660));
        priorityLabel.setText("20");
        prioritySlider.setValue(20);
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        prioritySlider = new javax.swing.JSlider();
        zoomPanel = new ZoomPanel(tp, coreSize);
        priorityLabel = new javax.swing.JLabel();
        fixedMinPrioLabel = new javax.swing.JLabel();
        MenuBar = new javax.swing.JMenuBar();
        ImportMenu = new javax.swing.JMenu();
        LoadGraphItem = new javax.swing.JMenuItem();
        ExportMenu = new javax.swing.JMenu();
        SaveImageItem = new javax.swing.JMenuItem();
        SelectionMenu = new javax.swing.JMenu();
        ClearMarkerMenuItem = new javax.swing.JMenuItem();
        UndoLastMarkerMenuItem = new javax.swing.JMenuItem();
        AlgorithmsMenu = new javax.swing.JMenu();
        RunDijkstraMenuItem = new javax.swing.JMenuItem();
        OptionMenu = new javax.swing.JMenu();
        ShowPriorityNodesMenuItem = new javax.swing.JMenuItem();
        TestMenu = new javax.swing.JMenu();
        RangeTreeTestMenuItem = new javax.swing.JMenuItem();
        PstTestMenuItem = new javax.swing.JMenuItem();
        GridTestMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        prioritySlider.setMinorTickSpacing(5);
        prioritySlider.setPaintLabels(true);
        prioritySlider.setValue(0);
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

        LoadGraphItem.setText("Load Graph");
        LoadGraphItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoadGraphItemActionPerformed(evt);
            }
        });
        ImportMenu.add(LoadGraphItem);

        MenuBar.add(ImportMenu);

        ExportMenu.setText("Export");

        SaveImageItem.setText("Save As PNG");
        SaveImageItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomPanel.SaveImage(evt);
            }
        });
        ExportMenu.add(SaveImageItem);

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
                RunDijkstraMenuItemActionPerformed(evt);
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
        OptionMenu.add(ShowPriorityNodesMenuItem);

        MenuBar.add(OptionMenu);

        TestMenu.setText("Test");

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
    }// </editor-fold>//GEN-END:initComponents


    private void LoadGraphItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoadGraphItemActionPerformed
        System.out.println("clicked");
        zoomPanel.loadCore();
    }//GEN-LAST:event_LoadGraphItemActionPerformed







    private void ClearMarkerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ClearMarkerMenuItemActionPerformed
        repaint();
    }//GEN-LAST:event_ClearMarkerMenuItemActionPerformed

    private void UndoLastMarkerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UndoLastMarkerMenuItemActionPerformed
        repaint();
    }//GEN-LAST:event_UndoLastMarkerMenuItemActionPerformed


    private void RunDijkstraMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RunDijkstraMenuItemActionPerformed
    }//GEN-LAST:event_RunDijkstraMenuItemActionPerformed



    private void ShowPriorityNodesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShowPriorityNodesMenuItemActionPerformed
        zoomPanel.showPriorityNodes = !zoomPanel.showPriorityNodes;
        repaint();
    }//GEN-LAST:event_ShowPriorityNodesMenuItemActionPerformed

    private void GridTestMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GridTestMenuItemActionPerformed

    }//GEN-LAST:event_GridTestMenuItemActionPerformed


    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        final TPClient tp = new TPClient("http://localhost:8080");
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
    private javax.swing.JMenuItem GridTestMenuItem;
    private javax.swing.JMenu ImportMenu;
    private javax.swing.JMenuItem LoadGraphItem;
    private javax.swing.JMenuBar MenuBar;
    private javax.swing.JMenu OptionMenu;
    private javax.swing.JMenuItem PstTestMenuItem;
    private javax.swing.JMenuItem RangeTreeTestMenuItem;
    private javax.swing.JMenuItem RunDijkstraMenuItem;
    private javax.swing.JMenuItem SaveImageItem;
    private javax.swing.JMenu SelectionMenu;
    private javax.swing.JMenuItem ShowPriorityNodesMenuItem;
    private javax.swing.JMenuItem SmartPhoneMenuItem;
    private javax.swing.JMenuItem TabletMenuItem;
    private javax.swing.JMenu TestMenu;
    private javax.swing.JMenuItem UndoLastMarkerMenuItem;
    private javax.swing.JLabel fixedMinPrioLabel;
    private javax.swing.JLabel priorityLabel;
    private javax.swing.JSlider prioritySlider;
    private ZoomPanel zoomPanel;
    // End of variables declaration//GEN-END:variables
}
