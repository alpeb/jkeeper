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
 * $Date: 2008-05-10 12:48:02 -0500 (Sat, 10 May 2008) $
 * $Revision: 11 $
 * $LastChangedBy: alejandro.pedraza $
 */

package jkeeper;

import javax.swing.tree.DefaultMutableTreeNode;
import org.w3c.dom.*;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.TreeWalker;

/**
 *
 * @author  Alejandro Pedraza <alejandro.pedraza at gmail>
 */
public class JKeeperXML2JTree {
    private JKeeperTreeModel treeModel;
    
    public JKeeperXML2JTree(Document document) {
        treeModel = new JKeeperTreeModel();
        
        Node root = document.getDocumentElement();
        int whattoshow = NodeFilter.SHOW_ALL;
        NodeFilter nodefilter = null;
        boolean expandreferences = false;

        DocumentTraversal traversal = (DocumentTraversal)document;
        TreeWalker walker = traversal.createTreeWalker(root, 
                                                       whattoshow, 
                                                       nodefilter, 
                                                       expandreferences);
        Node thisNode = null;
        thisNode = walker.nextNode();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
        while (thisNode != null) {
            if (thisNode.getNodeName().equals("category")) {
                Element element = (Element)thisNode;
                //System.out.println("Category " + element.getAttribute("name"));
                DefaultMutableTreeNode newCategoryNode =
                        new DefaultMutableTreeNode(new JKeeperCategory(element.getAttribute("name")));
                rootNode.add(newCategoryNode);
                Node item = walker.firstChild();
                if (item != null) {
                    while (true) {
                        Element itemElement = (Element)item;
                        //System.out.println("Item " + itemElement.getAttribute("name"));
                        Node url = walker.firstChild();
                        Node username = walker.nextSibling();
                        Node password = walker.nextSibling();
                        Node notes = walker.nextSibling();
                        JKeeperItem newItem =
                                new JKeeperItem(itemElement.getAttribute("name"),
                                    url.getTextContent(), username.getTextContent(),
                                    password.getTextContent(), notes.getTextContent());
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newItem);
                        newCategoryNode.add(newNode);
                        item = walker.nextNode();
                        if (item != null && item.getNodeType() == Node.TEXT_NODE) {
                            item = walker.nextNode();
                        }
                        if (item == null || !item.getNodeName().equals("item")) {
                            walker.previousNode();
                            break;
                        }
                    }
                }
            }
            thisNode = walker.nextNode();
        }
    }
    
    public JKeeperTreeModel getTreeModel() {
        return treeModel;
    }
}
