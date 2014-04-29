/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA 02111-1307 USA
 * 
 * Copyright 2008 Alejandro Pedraza. All rights reserved.
 * 
 * $Date: 2008-05-15 21:37:50 -0500 (Thu, 15 May 2008) $
 * $Revision: 14 $
 * $LastChangedBy: alejandro.pedraza $
 */

package jkeeper;

import org.jdesktop.application.Action;
import org.jdesktop.application.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.EventObject;
import javax.crypto.Cipher;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

/**
 * The application's main frame.
 */
public class JKeeperView extends FrameView {
    private File file = null;
    private char[] password = null;
    
    public JKeeperView(SingleFrameApplication app, String[] args) {
        super(app);
        ResourceMap resourceMap = getResourceMap();
        String resourcesDir = resourceMap.getResourcesDir();
        String frameIconFile = resourcesDir + "lock.png";
        URL frameIconURL = resourceMap.getClassLoader().getResource(frameIconFile);
        getFrame().setIconImage((new ImageIcon(frameIconURL)).getImage());

        initComponents();
        
        jTree1.setShowsRootHandles(true);
        jKeeperTreeModel = new JKeeperTreeModel();
        jKeeperTreeModel.addCategory("Bank sites");
        jKeeperTreeModel.addCategory("Credit cards");
        jKeeperTreeModel.addCategory("Web sites");
        jKeeperTreeModel.addCategory("E-mail");
        jKeeperTreeModel.addCategory("Instant messaging");
        jKeeperTreeModel.addCategory("Devices");
        jKeeperTreeModel.addCategory("Documents");
        jKeeperTreeModel.addCategory("Safes");
        jKeeperTreeModel.addCategory("Computer accounts");
        jKeeperTreeModel.addCategory("Others");
        jKeeperTreeModel.setHasChanged(false);
        
        jTree1.setModel(jKeeperTreeModel);
        jTree1.setCellEditor(new DefaultTreeCellEditor(jTree1,
                            (DefaultTreeCellRenderer) jTree1.getCellRenderer()) {
            @Override
            protected boolean canEditImmediately(EventObject event) {
                if (!super.canEditImmediately(event)) {
                    return false;
                }
                
                TreePath treePath = jTree1.getSelectionPath();
                if (treePath != null) {
                    DefaultMutableTreeNode selectedNode = 
                            (DefaultMutableTreeNode) treePath.getLastPathComponent();
                    if (selectedNode.getUserObject() instanceof JKeeperItem) {
                        return false;
                    }
                }
                
                return true;
            }
        });
        
        MouseListener ml = new MouseAdapter() {
            @Override
             public void mousePressed(MouseEvent e) {
                 int selRow = jTree1.getRowForLocation(e.getX(), e.getY());
                 TreePath selPath = jTree1.getPathForLocation(e.getX(), e.getY());
                 if (selPath == null) {
                     return;
                 }
                 DefaultMutableTreeNode selectedNode = 
                         (DefaultMutableTreeNode)selPath.getLastPathComponent();
                 if(selRow != -1) {
                     if(e.getClickCount() == 2) {
                        if (!(selectedNode.getUserObject() instanceof JKeeperItem)) {
                            return;
                        }

                        JFrame mainFrame = JKeeperApp.getApplication().getMainFrame();
                        JKeeperNewItem newItemBox = new JKeeperNewItem(mainFrame, true);
                        newItemBox.setLocationRelativeTo(mainFrame);
                        newItemBox.setNode((DefaultMutableTreeNode)selectedNode);
                        newItemBox.setParentNode(
                                (DefaultMutableTreeNode) selectedNode.getParent());
                        JKeeperApp.getApplication().show(newItemBox);
                        jTree1.updateUI();
                     }
                 }
             }
        };
        jTree1.addMouseListener(ml);
        
        jTree1.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        jTree1.updateUI();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
        
        if (args.length > 0 && args[0].equals(new String("-open"))) {
            File tFile = new File(args[1]);
            if (!open(tFile, false)) {
                app.exit();
            }
        }
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = JKeeperApp.getApplication().getMainFrame();
            aboutBox = new JKeeperAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        JKeeperApp.getApplication().show(aboutBox);
    }
    
    private void addUnsavedMark() {
        getFrame().setTitle("JKeeper (*)");
    }
    
    private void removeUnsavedMark() {
        getFrame().setTitle("JKeeper");
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new JKeeperJTree();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("mainPanel"); // NOI18N

        jToolBar1.setRollover(true);
        jToolBar1.setName("jToolBar1"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(jkeeper.JKeeperApp.class).getContext().getActionMap(JKeeperView.class, this);
        jButton1.setAction(actionMap.get("addCategory")); // NOI18N
        jButton1.setMnemonic('c');
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setName("jButton1"); // NOI18N
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton1);

        jButton2.setAction(actionMap.get("addItem")); // NOI18N
        jButton2.setMnemonic('i');
        jButton2.setFocusable(false);
        jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton2.setName("jButton2"); // NOI18N
        jButton2.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton2);

        jButton3.setAction(actionMap.get("deleteItem")); // NOI18N
        jButton3.setFocusable(false);
        jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton3.setName("jButton3"); // NOI18N
        jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton3);

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jTree1.setEditable(true);
        jTree1.setName("mainTree"); // NOI18N
        jTree1.setRootVisible(false);
        jScrollPane1.setViewportView(jTree1);

        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jToolBar1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanelLayout.createSequentialGroup()
                .add(jToolBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 58, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE)
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setMnemonic('F');
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(jkeeper.JKeeperApp.class).getContext().getResourceMap(JKeeperView.class);
        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        jMenuItem2.setAction(actionMap.get("open")); // NOI18N
        jMenuItem2.setText(resourceMap.getString("jMenuItem2.text")); // NOI18N
        jMenuItem2.setName("jMenuItem2"); // NOI18N
        fileMenu.add(jMenuItem2);

        jMenuItem3.setAction(actionMap.get("newFile")); // NOI18N
        jMenuItem3.setName("jMenuItem3"); // NOI18N
        fileMenu.add(jMenuItem3);

        jMenuItem1.setAction(actionMap.get("save")); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        fileMenu.add(jMenuItem1);

        jMenuItem5.setAction(actionMap.get("saveAs")); // NOI18N
        jMenuItem5.setName("jMenuItem5"); // NOI18N
        fileMenu.add(jMenuItem5);
        jMenuItem5.getAccessibleContext().setAccessibleName(resourceMap.getString("jMenuItem5.AccessibleContext.accessibleName")); // NOI18N

        jMenuItem4.setAction(actionMap.get("export")); // NOI18N
        jMenuItem4.setName("jMenuItem4"); // NOI18N
        fileMenu.add(jMenuItem4);

        exitMenuItem.setAction(actionMap.get("exit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setMnemonic('h');
        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusMessageLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 230, Short.MAX_VALUE)
                .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(statusMessageLabel)
                    .add(statusAnimationLabel)
                    .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    @Action
    public void addCategory() {
        jKeeperTreeModel.addCategory("New Category");
        jTree1.updateUI();
        addUnsavedMark();
    }
    
    @Action
    public void addItem() {
        TreePath selectedPath = jTree1.getSelectionPath();
        if (selectedPath == null) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Please select a category first",
                    "You need a category",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        DefaultMutableTreeNode selectedNode =
                (DefaultMutableTreeNode)selectedPath.getLastPathComponent();
        if (selectedNode.getUserObject() instanceof JKeeperItem) {
            selectedNode = (DefaultMutableTreeNode) selectedNode.getParent();
        }
        
        JFrame mainFrame = JKeeperApp.getApplication().getMainFrame();
        JKeeperNewItem newItemBox = new JKeeperNewItem(mainFrame, true);
        newItemBox.setLocationRelativeTo(mainFrame);
        newItemBox.setParentNode(selectedNode);
        JKeeperApp.getApplication().show(newItemBox);
        if (newItemBox.hasChanged()) {
            jKeeperTreeModel.setHasChanged(true);
            addUnsavedMark();
        }
        jTree1.updateUI();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTree jTree1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
    
    private JKeeperTreeModel jKeeperTreeModel;

    @Action
    public void deleteItem() {
        TreePath selectedPath = jTree1.getSelectionPath();
        if (selectedPath == null) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Please select a category or item first",
                    "You need a category or item",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        DefaultMutableTreeNode selectedNode =
                (DefaultMutableTreeNode)selectedPath.getLastPathComponent();
        if (selectedNode.getUserObject() instanceof JKeeperItem) {
            int result = JOptionPane.showConfirmDialog(mainPanel,
                    "Are you sure you want to delete the item "
                    + ((JKeeperItem) selectedNode.getUserObject()).getName()
                    + "?", "Really?", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.NO_OPTION || result == JOptionPane.CLOSED_OPTION) {
                return;
            }
        }
        if (selectedNode.getUserObject() instanceof JKeeperCategory) {
            int result = JOptionPane.showConfirmDialog(mainPanel,
                    "Are you sure you want to delete the category "
                    + (JKeeperCategory)selectedNode.getUserObject()
                    + "?\nALL ITS ITEMS WILL BE DELETED",
                    "Really?",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.NO_OPTION || result == JOptionPane.CLOSED_OPTION) {
                return;
            }
        }
        
        jKeeperTreeModel.removeNodeFromParent(selectedNode);
        addUnsavedMark();
        
        jTree1.updateUI();
    }
    
    @Action
    public void saveAs() {
        save(null, true, "passwords.jkp");
    }
    
    @Action
    public void save() {
        save(file, true, "passwords.jkp");
    }
    
    public void save(File tFile, Boolean encrypt, String suggestedName) {
        char[] newPassword = null;
        char[] newPassword2 = null;
        byte[] resultingBytes = null;
        int overwrite = 0;
        
        if (encrypt) {
            if (password != null) {
                newPassword = password;
            } else {
                newPassword = askForPassword(false);
                if (newPassword == null) return;
                newPassword2 = askForPassword(true);
                Boolean passwordsMatch = Arrays.equals(newPassword, newPassword2);
                safelyCleanupPassword(newPassword2);
                if (!passwordsMatch) {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Passwords do not match",
                            "",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }
        
        if (tFile == null) {
            tFile = new File(suggestedName);
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(tFile);
            
            do {
                int result = chooser.showSaveDialog(mainPanel);
                if (result == JFileChooser.CANCEL_OPTION) return;
                tFile = chooser.getSelectedFile();
                if (tFile.exists()) {
                    overwrite = JOptionPane.showConfirmDialog(mainPanel,
                            "File already exists. Do you want to overwrite it?",
                            "Really?",
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    if (overwrite == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                }
            } while (overwrite == JOptionPane.NO_OPTION);
            
            String saveFileName = tFile.getName();
            String ext = saveFileName.substring(saveFileName.lastIndexOf('.')+1,
                                                saveFileName.length());
            if (encrypt && !ext.equals("jkp")) {
                JOptionPane.showMessageDialog(mainPanel,
                        "Invalid file extension. You must use jkp",
                        "",
                        JOptionPane.WARNING_MESSAGE);
                save(tFile, encrypt, suggestedName);
                return;
            }
        }
        
        try {
            JKeeperXMLizer XMLizer = new JKeeperXMLizer(jTree1);
            Document document = XMLizer.getDocument();
            Transformer t = TransformerFactory.newInstance().newTransformer();
            //OutputStream outputStream = new FileOutputStream(tFile);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            
            t.transform(new DOMSource(document),
                        new StreamResult(byteArrayOutputStream));
            resultingBytes = byteArrayOutputStream.toByteArray();
            /*String debugOutput = new String(resultingBytes);
            System.out.println(debugOutput);*/
            
            if (encrypt) {
                Cipher cipher = JKeeperEncryption.getCipher(Cipher.ENCRYPT_MODE, newPassword);
                resultingBytes = cipher.doFinal(resultingBytes);
            }
            
            FileOutputStream outputStream = new FileOutputStream(tFile);
            outputStream.write(resultingBytes);
            outputStream.close();
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainPanel,
                    "OOPSS...\n\n:ERROR:\n\n" + getStackTrace(ex));
        }
        
        jKeeperTreeModel.setHasChanged(false);
        removeUnsavedMark();
        if (encrypt) {
            password = newPassword.clone();
            safelyCleanupPassword(newPassword);
        }
        
        file = tFile;
    }
    
    @Action
    public void open() {
        if (!confirmDisposal()) {
            return;
        }
        
        File tFile = null;
        
        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new JKPFilter());
        int result = chooser.showSaveDialog(mainPanel);
        if (result == JFileChooser.CANCEL_OPTION) return;
        try {
            tFile = chooser.getSelectedFile();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainPanel,
                    "OOPSS...\n\n:ERROR:\n\n" + getStackTrace(ex));
        }
        open(tFile, false);
    }

    public Boolean open(File tFile, Boolean confirmDisposal) {
       if (confirmDisposal && !confirmDisposal()) {
            return false;
        }
       
        try {
            FileInputStream fileStream = new FileInputStream(tFile);
            char[] filePassword = askForPassword(false);
            if (filePassword == null) return false;
            byte[] resultingBytes = new byte[(int)tFile.length()];
            fileStream.read(resultingBytes);
            Cipher cipher = JKeeperEncryption.getCipher(Cipher.DECRYPT_MODE, filePassword);
            try {
                resultingBytes = cipher.doFinal(resultingBytes);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainPanel,
                        "Wrong password",
                        "Can't open file",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }
            //System.out.println("XML: " + new String(resultingBytes));
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document document = parser.parse(new ByteArrayInputStream(resultingBytes));
            JKeeperXML2JTree xml2JTree = new JKeeperXML2JTree(document);
            jKeeperTreeModel = xml2JTree.getTreeModel();
            jTree1.setModel(jKeeperTreeModel);
            password = filePassword.clone();
            safelyCleanupPassword(filePassword);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainPanel,
                    "OOPSS...\n\n:ERROR:\n\n" + getStackTrace(ex));
        }
       
        file = tFile;
       
        return true;
    }
    
    public Boolean confirmDisposal() {
        if (jKeeperTreeModel.hasChanged()) {
            int result = JOptionPane.showConfirmDialog(mainPanel,
                    "Changes haven't been saved. Do you want to save them?",
                    "Really?", JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.CANCEL_OPTION) {
                return false;
            }
            if (result == JOptionPane.YES_OPTION) {
                save();
                return false;
            }
        }
        return true;
    }

    @Action
    public void newFile() {
        if (!confirmDisposal()) {
            return;
        }
        
        DefaultMutableTreeNode root =
                (DefaultMutableTreeNode) jKeeperTreeModel.getRoot();
        Enumeration children = root.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) root.getFirstChild();
            jKeeperTreeModel.removeNodeFromParent(node);
        }
        jKeeperTreeModel.setHasChanged(false);
        removeUnsavedMark();
        file = null;
        password = null;
    }
    
    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

    @Action
    public void export() {
        save(null, false, "passwords.xml");
    }

    private char[] askForPassword(Boolean secondTry) {
        final JPasswordField passField = new JPasswordField();
        passField.addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent event) {
                passField.requestFocusInWindow();
            }

            public void ancestorRemoved(AncestorEvent event) {
            }

            public void ancestorMoved(AncestorEvent event) {
            }
        });
        String message = secondTry  ? "Please enter your password again"
                                    : "Please enter your password";
        int result = JOptionPane.showOptionDialog(  mainPanel,
                                                    new Object[] {message, passField},
                                                    "Password",
                                                    JOptionPane.OK_CANCEL_OPTION,
                                                    JOptionPane.QUESTION_MESSAGE,
                                                    null,
                                                    null,
                                                    null);
        if (result != JOptionPane.OK_OPTION) return null;
        
        char[] password = passField.getPassword();
        
        if (password.length == 0) return null;
        
        return password;
    }
    
    private void safelyCleanupPassword(char[] password) {
        Arrays.fill(password, '0');
        password = null;        
    }
    
    private class JKPFilter extends FileFilter {
        public boolean accept(File f) {
            if (f.isDirectory())
                return true;
            String s = f.getName();
            int i = s.lastIndexOf('.');

            if (i > 0 && i < s.length() - 1)
                if (s.substring(i + 1).toLowerCase().equals("jkp"))
                    return true;

            return false;
        }

        @Override
        public String getDescription() {
            return "jkp files";
        }
    }

    @Action
    public void exit() {
        if (!confirmDisposal()) {
            return;
        }
        
        getApplication().exit();
    }
}
