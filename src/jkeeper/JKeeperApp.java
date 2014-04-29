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

import java.util.EventObject;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class JKeeperApp extends SingleFrameApplication {
    private String[] args;

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        final JKeeperView jKeeperView = new JKeeperView(this, args);
        show(jKeeperView);
        
        addExitListener(new ExitListener() {
          public boolean canExit(EventObject e) {
              return jKeeperView.confirmDisposal();
          }
          public void willExit(EventObject event) {
          }
      });
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }
    
    @Override protected void initialize(String[] args) {
        this.args = args;
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of JKeeperApp
     */
    public static JKeeperApp getApplication() {
        return Application.getInstance(JKeeperApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(JKeeperApp.class, args);
    }
}
