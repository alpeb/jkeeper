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
 * $Date: 2008-05-10 11:03:42 -0500 (Sat, 10 May 2008) $
 * $Revision: 9 $
 * $LastChangedBy: alejandro.pedraza $
 */

package jkeeper;

import javax.swing.tree.*;

/**
 *
 * @author  Alejandro Pedraza <alejandro.pedraza at gmail>
 */
public class JKeeperTreeModel extends DefaultTreeModel {
    
    private Boolean hasChanged = false;
    
    public JKeeperTreeModel() {
        this(new DefaultMutableTreeNode(), true);
    }
    
    public JKeeperTreeModel(TreeNode root, boolean asksAllowsChildren) {
        super(root, asksAllowsChildren);
    }

    public void addCategory(String catName) {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) this.getRoot();
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new JKeeperCategory(catName));
        rootNode.add(newNode);
        hasChanged = true;
    }
    
    @Override
    public boolean isLeaf(Object node) {
        return ((DefaultMutableTreeNode) node).getUserObject() instanceof JKeeperItem;
    }

    /**
     * I need to override this for when the name of the node is edited,
     * avoid its userobject to become a string
     * 
     * @param path
     * @param newValue
     */
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) { 
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
        String value = (String)newValue;
        node.setUserObject(new JKeeperCategory(value));
        nodeChanged(node); 
    } 
    
    @Override
    public void removeNodeFromParent(MutableTreeNode node) {
        super.removeNodeFromParent(node);
        hasChanged = true;
    }
    
    public Boolean hasChanged() {
        return hasChanged;
    }
    
    public void setHasChanged(Boolean hasChanged) {
        this.hasChanged = hasChanged;
    }
}
