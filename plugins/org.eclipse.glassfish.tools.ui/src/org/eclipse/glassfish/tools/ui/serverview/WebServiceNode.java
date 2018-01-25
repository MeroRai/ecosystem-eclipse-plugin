/******************************************************************************
 * Copyright (c) 2018 Oracle
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.glassfish.tools.ui.serverview;

import java.util.ArrayList;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import org.eclipse.glassfish.tools.GlassFishServer;
import org.eclipse.glassfish.tools.serverview.WSDesc;

/**
 * A deployed web service node in the server view
 * 
 * @author Ludovic Champenois
 *
 */
public class WebServiceNode extends TreeNode{

	DeployedWebServicesNode parent;
	GlassFishServer server = null;
	TreeNode[] modules = null;
	WSDesc app = null;
	public WebServiceNode(DeployedWebServicesNode root, GlassFishServer server, WSDesc app) {
		super(app.getName(), null, root);
		this.server = server;
		this.app = app;
	}
	
	public GlassFishServer getServer(){
		return this.server;
	}
	
	public WSDesc getWSInfo(){
		return this.app;
	}
	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
        ArrayList< IPropertyDescriptor > properties = new ArrayList< IPropertyDescriptor >();
        PropertyDescriptor pd;


                pd = new TextPropertyDescriptor( "testurl", "Test URL" );
                properties.add( pd );
                pd = new TextPropertyDescriptor( "name", "name" );
                properties.add( pd );        
                pd = new TextPropertyDescriptor( "wsdlurl", "WSDL URL" );
                properties.add( pd );        
        

        return properties.toArray( new IPropertyDescriptor[ 0 ] );
	}
	@Override
	public Object getPropertyValue(Object id) {
	       if ( id.equals( "testurl" ))
               return app.getTestURL();
	       if ( id.equals( "name" ))
                   return app.getName();
	       if ( id.equals( "wsdlurl" ))
               return app.getWsdlUrl();

     

		
		return null;
	}   	
}