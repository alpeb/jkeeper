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

import java.util.Enumeration;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author  Alejandro Pedraza <alejandro.pedraza at gmail>
 */
public class JKeeperXMLizer {
    JTree tree;
    Document document;
    
    public JKeeperXMLizer(JTree tree) throws Exception {
        this.tree = tree;
        DefaultMutableTreeNode rootNode =
                (DefaultMutableTreeNode) tree.getModel().getRoot();
        final DocumentBuilder builder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        document = builder.newDocument();
        final Element root = document.createElement("tree");
        root.setAttribute("version", "0.1.0");
        addNode(document, root, rootNode);
        document.appendChild(root);
    }
    
    public Document getDocument() {
        return document;
    }

    private void addNode(Document document, Element parentNode,
                        DefaultMutableTreeNode treeNode) {
        Enumeration children = treeNode.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            Element element = createElement(node, document);
            parentNode.appendChild(element);
            addNode(document, element, node);
        }
    }

    private Element createElement(DefaultMutableTreeNode node, Document document) {
        final Object data = node.getUserObject();
        final TreePath path = new TreePath(node.getPath());
        Element element;
        String tagName = data.toString();
        tagName = tagName.replaceAll("[\\s]", "_");
        if (data instanceof JKeeperCategory) {
            //System.out.println("Category " + ((JKeeperCategory)data).getName());
            element = document.createElement("category");
            element.setAttribute("name", ((JKeeperCategory)data).getName());
        } else {
            //System.out.println("Item " + ((JKeeperItem)data).getName());
            element = document.createElement("item");
            element.setAttribute("name", ((JKeeperItem)data).getName());
            
            Element itemUrlElement = document.createElement("url");
            itemUrlElement.setTextContent(((JKeeperItem)data).getUrl());
            element.appendChild(itemUrlElement);
            
            Element itemUsernameElement = document.createElement("username");
            itemUsernameElement.setTextContent(((JKeeperItem)data).getUsername());
            element.appendChild(itemUsernameElement);
            
            Element itemPasswordElement = document.createElement("password");
            itemPasswordElement.setTextContent(((JKeeperItem)data).getPassword());
            element.appendChild(itemPasswordElement);
            
            Element itemNotesElement = document.createElement("notes");
            itemNotesElement.setTextContent(((JKeeperItem)data).getNotes());
            element.appendChild(itemNotesElement);
        }
        element.setAttribute("expanded", Boolean.toString(tree.isExpanded(path)));
        element.setAttribute("selected", Boolean.toString(tree.isPathSelected(path)));
        return element;
    }
}
