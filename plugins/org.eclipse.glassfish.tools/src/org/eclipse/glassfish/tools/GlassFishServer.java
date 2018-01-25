/******************************************************************************
 * Copyright (c) 2018 Oracle
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.glassfish.tools;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.glassfish.tools.facets.IGlassfishWebDeploymentDescriptor;
import org.eclipse.glassfish.tools.facets.internal.GlassfishDeploymentDescriptorFactory;
import org.eclipse.glassfish.tools.sdk.data.GlassFishAdminInterface;
import org.eclipse.glassfish.tools.sdk.server.parser.HttpData;
import org.eclipse.glassfish.tools.sdk.server.parser.HttpListenerReader;
import org.eclipse.glassfish.tools.sdk.server.parser.NetworkListenerReader;
import org.eclipse.glassfish.tools.sdk.server.parser.TargetConfigNameReader;
import org.eclipse.glassfish.tools.sdk.server.parser.TreeParser;
import org.eclipse.glassfish.tools.utils.ModuleUtil;
import org.eclipse.glassfish.tools.utils.Utils;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.jst.server.core.IEnterpriseApplication;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.internal.J2EEUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.sapphire.Property;
import org.eclipse.sapphire.PropertyBinding;
import org.eclipse.sapphire.PropertyDef;
import org.eclipse.sapphire.Resource;
import org.eclipse.sapphire.ValuePropertyBinding;
import org.eclipse.sapphire.Version;
import org.eclipse.sapphire.modeling.Path;
import org.eclipse.wst.common.componentcore.internal.util.IModuleConstants;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.IMonitoredServerPort;
import org.eclipse.wst.server.core.internal.IServerMonitorManager;
import org.eclipse.wst.server.core.internal.ServerMonitorManager;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.eclipse.wst.server.core.util.SocketUtil;

@SuppressWarnings("restriction")
public final class GlassFishServer extends ServerDelegate implements IURLProvider {

    public static final String TYPE_ID = "glassfish.server";
    public static final IServerType TYPE = ServerCore.findServerType( TYPE_ID );
    
	private static final String DEFAULT_DOMAIN_DIR_NAME = "domains"; //$NON-NLS-N$
	private static final String DEFAULT_DOMAIN_NAME = "domain1"; //$NON-NLS-N$
	public static final int DEFAULT_DEBUG_PORT = 9009;
	
	public static final String ATTR_SERVER_ADDRESS = "server.address"; //$NON-NLS-1$

	public static final String ATTR_SERVERPORT = "glassfish.serverportnumber"; //$NON-NLS-1$
	public static final String ATTR_ADMINPORT = "glassfish.adminserverportnumber"; //$NON-NLS-1$
	public static final String ATTR_DEBUG_PORT = "glassfish.debugport";
	public static final String ATTR_USECUSTOMTARGET = "glassfish.usecustomtarget";
	//public static final String TARGET = "glassfish.target";
	public static final String ATTR_DOMAINPATH = "glassfish.domainpath"; //$NON-NLS-1$
	public static final String ATTR_ADMIN = "glassfish.adminname"; //$NON-NLS-1$
	public static final String ATTR_ADMINPASS = "glassfish.adminpassword"; //$NON-NLS-1$
	public static final String ATTR_KEEPSESSIONS = "glassfish.keepSessions"; //$NON-NLS-1$
	public static final String ATTR_JARDEPLOY = "glassfish.jarDeploy"; //$NON-NLS-1$
	public static final String ATTR_USEANONYMOUSCONNECTIONS = "glassfish.useAnonymousConnection"; //$NON-NLS-1$

	public static final String SAMPLEDBDIR = "glassfish.sampledbdir"; //$NON-NLS-1$

	// used only for v2, populated from project properties or module name with
	// no space
	public static final String CONTEXTROOT = "glassfish.contextroot"; //$NON-NLS-1$

	public static final String DOMAINUPDATE = "domainupdate"; //$NON-NLS-1$
	
	private List<PropertyChangeListener> propChangeListeners;

	private IGlassfishServerModel model;
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.wst.server.core.model.ServerDelegate#initialize()
	 */
	@Override
	protected void initialize() {
		GlassfishToolsPlugin
				.logMessage("in GlassFishServer initialize" + this.getServer().getName()); //$NON-NLS-1$
		
		if( getServerWorkingCopy() != null )
		{
		    readDomainConfig();
		}
		
		this.model = IGlassfishServerModel.TYPE.instantiate(new ConfigResource(getServerWorkingCopy()));
	}

	public GlassFishServerBehaviour getServerBehaviourAdapter() {
		GlassFishServerBehaviour serverBehavior = (GlassFishServerBehaviour) getServer()
				.getAdapter(GlassFishServerBehaviour.class);
		if (serverBehavior == null) {
			serverBehavior = (GlassFishServerBehaviour) getServer()
					.loadAdapter(GlassFishServerBehaviour.class,
							new NullProgressMonitor());
		}
		return serverBehavior;
	}
	
	public IGlassfishServerModel getModel() {
		return this.model;
	}
	
	public static IPath getDefaultDomainDir(IPath serverLocation) {
		return serverLocation.append(DEFAULT_DOMAIN_DIR_NAME).append(DEFAULT_DOMAIN_NAME);
	}
	
	public static String createServerNameWithDomain(String serverName, Path domain) {
		int domainStartPos = serverName.lastIndexOf("[");
		if (domainStartPos == -1) {
			return serverName + " [" + domain.lastSegment() + "]";
		} else {
			return serverName.substring(0, domainStartPos) + "[" + domain.lastSegment() + "]";
		}
		
	}
	
	public boolean isRemote() {
		return (getServer().getServerType().supportsRemoteHosts() && !SocketUtil
				.isLocalhost(getServer().getHost()));
	}
	
//	public void updateServerName() {
//		if (!isRemote())
//			getServerWorkingCopy().setName(createServerNameWithDomain(getServer().getName(), 
//					new Path(getDomainPath()).lastSegment()));
//	}
	
	protected String getDebugOptions(int debugPort)
	{
	    final Version version = getVersion();
	    
	    if( version.matches( "[4" ) )
	    {
	        return "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + debugPort;
	    }
	    else
	    {
	        return "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=" + debugPort;
	    }
	}

	protected void readDomainConfig() {
		if (!isRemote()) {
			if (readServerConfiguration(new File(getDomainsFolder()
					+ File.separator + getDomainName() + "/config/domain.xml"))) { //$NON-NLS-1$
				GlassfishToolsPlugin
						.logMessage("in glassfish initialize done readServerConfiguration"); //$NON-NLS-1$
				syncHostAndPortsValues();
				// this is mainly so serversection can listen and repopulate,
				// but it is not working as intended because the sunserver
				// instance to
				// which the prop change listener is attached is a different one
				// than is
				// seeing the changes. in fact, we have multiple instances of
				// this
				// object and the glassfishBehaviour object per server -
				// issue 140
				//firePropertyChangeEvent(DOMAINUPDATE, null, null);
			} else {
				GlassfishToolsPlugin
						.logMessage("in glassfish could not readServerConfiguration - probably invalid domain"); //$NON-NLS-1$
			}
		}
	}

	public String validateDomainExists(String domainPath) {

		if (isRemote()) {
			return null;
		}
		
		if ((domainPath != null) && (!domainPath.startsWith("${"))) { //only if we are correctly setup...	//$NON-NLS-1$
			File f = new File(domainPath);
			if (!f.exists()) {
				return MessageFormat.format(Messages.pathDoesNotExist,
						f.getAbsolutePath());
			}
			if (!f.isDirectory()) {
				return MessageFormat.format(Messages.pathNotDirectory,
						f.getAbsolutePath());
			}
			if (!Utils.canWrite(f)) {
				return MessageFormat.format(Messages.pathNotWritable,
						f.getAbsolutePath());
			}
			File configDir = new File(f, "config");
			if (!configDir.exists()) {
				return MessageFormat.format(Messages.pathDoesNotExist,
						configDir.getAbsolutePath());
			}
			if (!configDir.canWrite()) {
				return MessageFormat.format(Messages.pathNotWritable,
						configDir.getAbsolutePath());
			}
			File domain = new File(f, "config/domain.xml"); //$NON-NLS-1$
			if (!domain.exists()) {
				return MessageFormat.format(Messages.pathNotValidDomain,
						domain.getAbsolutePath());
			}
			return null;
		}
		return Messages.missingDomainLocation;
	}

	IStatus validateDomainLocation() {
		if (isRemote()) {
			return Status.OK_STATUS;
		}
		
		String domainPath = getDomainPath();
		String domainConfigPath = domainPath + File.separator + "config" +
				File.separator + "domain.xml";
		File domainConfigLocation = new File(domainConfigPath);
		if (!domainConfigLocation.exists()) {
			return new Status(Status.ERROR, GlassfishToolsPlugin.SYMBOLIC_NAME, Messages.pathNotValidDomain);
		}
		// check if domain and config dir are writable
		File domainLocation = domainConfigLocation.getParentFile().getParentFile();
		if (!Utils.canWrite(domainLocation)) {
			return new Status(Status.ERROR, GlassfishToolsPlugin.SYMBOLIC_NAME,
					NLS.bind(Messages.pathNotWritable, domainLocation.getAbsolutePath()));
		}
		File domainConfigDir = domainConfigLocation.getParentFile();
		if (!Utils.canWrite(domainConfigDir)) {
			return new Status(Status.ERROR, GlassfishToolsPlugin.SYMBOLIC_NAME,
					NLS.bind(Messages.pathNotWritable, domainConfigDir.getAbsolutePath()));
		}
		
		return Status.OK_STATUS;
	}
	
	/*
	 * not yet, ui nor working well for generic validation
	 */
	public IStatus validate() {

		GlassfishToolsPlugin.logMessage("in AbstractGlassfishServer validate");// +getDomainDir()+"---"+getDomainName());
		IStatus s = null;
		if( !isRemote() ){
			//validate domain before reading domain.xml
			if (!(s = validateDomainLocation()).isOK()) {
				return s;
			}
			
			
			for(IServer server : ServerCore.getServers()){
				if( server.getId().equals(this.getServer().getId()) )
					continue;
				
				if(server.getServerType()==this.getServer().getServerType()){
					GlassFishServer gfServer = (GlassFishServer)server.loadAdapter(GlassFishServer.class, null);
					File p1 = new File(getDomainPath());
					File p2 = new File(gfServer.getDomainPath());
					if( p1.equals( p2 ) ){
						return new Status(IStatus.ERROR, GlassfishToolsPlugin.SYMBOLIC_NAME,
								Messages.serverWithSameDomainPathExisting);
					}
				}
			}
			
			
			// reads ports from domain
			readDomainConfig();
	
			// validate ports
			if (getAdminPort() == -1) {
				return new Status(IStatus.ERROR, GlassfishToolsPlugin.SYMBOLIC_NAME,
						Messages.invalidPortNumbers);
			} else {
				// refresh model
				getModel().setAdminPort(getAdminPort());
				getModel().setServerPort(getPort());
				
			}
		}
		
		return Status.OK_STATUS;
	}

	private void syncHostAndPortsValues() {
		System.err.println("syncHostAndPortsValues");
	}

	/**
	 * Add a property change listener to this server.
	 * 
	 * @param listener
	 *            java.beans.PropertyChangeListener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (propChangeListeners == null)
			propChangeListeners = new ArrayList<PropertyChangeListener>();
		propChangeListeners.add(listener);
	}

	/**
	 * Remove a property change listener from this server.
	 * 
	 * @param listener
	 *            java.beans.PropertyChangeListener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		if (propChangeListeners != null)
			propChangeListeners.remove(listener);
	}

	/**
	 * Fire a property change event.
	 * 
	 * @param propertyName
	 *            a property name
	 * @param oldValue
	 *            the old value
	 * @param newValue
	 *            the new value
	 */
	public void firePropertyChangeEvent(String propertyName, Object oldValue,
			Object newValue) {
		if (propChangeListeners == null)
			return;

		PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName,
				oldValue, newValue);
		try {
			Iterator<PropertyChangeListener> iterator = propChangeListeners
					.iterator();
			while (iterator.hasNext()) {
				try {
					PropertyChangeListener listener = (PropertyChangeListener) iterator
							.next();
					listener.propertyChange(event);
				} catch (Exception e) {
					GlassfishToolsPlugin.logError(
							"Error firing property change event", e); //$NON-NLS-1$
				}
			}
		} catch (Exception e) {
			GlassfishToolsPlugin.logError("Error in property event", e); //$NON-NLS-1$
		}
	}

	public static GlassFishServer getGlassfishServerDelegate(
			IServerWorkingCopy server) {
		GlassFishServer glassfishDelegate = (GlassFishServer) server
				.getOriginal().getAdapter(GlassFishServer.class);
		if (glassfishDelegate == null) {
			glassfishDelegate = (GlassFishServer) server.getOriginal()
					.loadAdapter(GlassFishServer.class,
							new NullProgressMonitor());
		}
		return glassfishDelegate;
	}

	public ServerPort[] getServerPorts() {
		try {
			ServerPort[] sp = new ServerPort[2];
			sp[0] = new ServerPort("adminserver", "Admin Server Port",
					getAdminPort(), "HTTP");
			sp[1] = new ServerPort("server", "Server Port",
					getPort(), "HTTP");

			return sp;
		} catch (Exception e) {
			return new ServerPort[0];
		}
	}

	protected boolean readServerConfiguration(File domainXml) {
		boolean result = false;

		final Map<String, HttpData> httpMap = new LinkedHashMap<String, HttpData>();

		if (domainXml.exists()) {
			//JmxConnectorReader jmxReader = new JmxConnectorReader();
			TargetConfigNameReader configNameReader = new TargetConfigNameReader();
			TreeParser.readXml(domainXml, configNameReader);
			String configName = configNameReader.getTargetConfigName();
			if (configName == null) {
				return false;
			}
			HttpListenerReader httpListenerReader = new HttpListenerReader(configName);
			NetworkListenerReader networkListenerReader = new NetworkListenerReader(configName);
			try {
				TreeParser.readXml(domainXml, httpListenerReader,
						networkListenerReader);
				//jmxPort = jmxReader.getResult();

				httpMap.putAll(httpListenerReader.getResult());
				httpMap.putAll(networkListenerReader.getResult());
				// !PW This probably more convoluted than it had to be, but
				// while
				// http-listeners are usually named "http-listener-1",
				// "http-listener-2", ...
				// technically they could be named anything.
				//
				// For now, the logic is as follows:
				// admin port is the one named "admin-listener"
				// http port is the first non-secure enabled port - typically
				// http-listener-1
				// https port is the first secure enabled port - typically
				// http-listener-2
				// disabled ports are ignored.
				//
				HttpData adminData = httpMap.remove("admin-listener"); //$NON-NLS-1$
				int adminPort = adminData != null ? adminData.getPort() : -1;
				setAttribute(
						ATTR_ADMINPORT,
						String.valueOf(adminPort) ); 
				GlassfishToolsPlugin
						.logMessage("reading from domain.xml adminServerPortNumber=" + getAdminPort()); //$NON-NLS-1$
				
				HttpData httpPortData = httpMap.remove("http-listener-1"); //$NON-NLS-1$
				int httpPort = httpPortData != null ? httpPortData.getPort() : -1 ;
				setAttribute(
						ATTR_SERVERPORT,
						String.valueOf(httpPort)); 

				result = adminPort != -1;
			} catch (IllegalStateException ex) {
				GlassfishToolsPlugin.logError("error IllegalStateException ", ex); //$NON-NLS-1$
			}
		}
		return result;
	}

	public String getDomainConfigurationFilePath() {
		return getDomainPath().trim() + "/config/domain.xml";
	}

	public int getDebugPort() {
		return getAttribute(ATTR_DEBUG_PORT, -1);
	}
	
	/* *************************************************************
	 * Implementation of adapter methods used by tooling SDK library.
	 */
	public int getAdminPort() {	
		return getAttribute(ATTR_ADMINPORT, -1);
	}

	public String getAdminUser() {
		return getAttribute(ATTR_ADMIN, "admin");
	}

	public String getDomainsFolder() {
		if( !isRemote() )
			return new File(getDomainPath()).getParent();
		return null;
	}

	public String getDomainName() {
		return getDomainPath()!=null ? 
				new File(getDomainPath()).getName() : null;
	}

	public String getHost() {
		return getServer().getHost();
	}

	public String getName() {
		return getServer().getName();
	}

	public int getPort() {
		return getAttribute(ATTR_SERVERPORT, 8080);
	}

	public String getUrl() {
		return null;
	}

	public Version getVersion()
	{
        final IPath location = getServer().getRuntime().getLocation();
        
        if( location != null )
        {
            final GlassFishInstall gfInstall = GlassFishInstall.find( location.toFile() );
            
            if( gfInstall != null )
            {
                return gfInstall.version();
            }
        }
        
        return null;
	}

	public GlassFishAdminInterface getAdminInterface() {
		return GlassFishAdminInterface.HTTP;
	}

	public String getServerHome() {
		return new File(getServer().getRuntime().getLocation().toString())
				.getAbsolutePath();
	}
	
	public String getServerRoot() {
		return null;
	}
	//*********************************************************
	
	public boolean getKeepSessions() {
		return getAttribute(ATTR_KEEPSESSIONS, true);
	}
	
//	public boolean useCustomTarget() {
//		return getAttribute(ATTR_C, false);
//	}
	
	public String getAdminPassword() {
		return getAttribute(ATTR_ADMINPASS, "");
	}

	public void setAdminPassword(String value) {
		setAttribute(ATTR_ADMINPASS, value);
//		try {
//			// this.saveConfiguration(new NullProgressMonitor());
//			this.configurationChanged();
//		} catch (Exception ex) {
//			GlassfishToolsPlugin.logMessage("error =" + ex); //$NON-NLS-1$
//		}
	}
	
	public String computePreserveSessions() {
		String ret = null;
		if (!getKeepSessions())
			return ret;
		ret = "keepstate";
		return ret;
	}

	/*
	 * JAR deploy for v3
	 */
	public boolean getJarDeploy() {
		if( isRemote() )
			return true;
		
		return getAttribute(ATTR_JARDEPLOY, false);
	}
	
	public void setPort(int port) {
		setAttribute(ATTR_SERVERPORT, port);
	}

	public String getDomainPath() {
		return getAttribute(ATTR_DOMAINPATH, "");
	}
	
	public boolean useAnonymousConnections() {
		return getAttribute(ATTR_USEANONYMOUSCONNECTIONS, true);
	}
	
	public String getServerInstallationDirectory() {
		return getServer().getRuntime().getLocation().toString();
	}

	// ************* CONFIG RESOURCE FOR BINDING FROM SAPPHIRE GUI *********************
	
	private final class ConfigResource extends Resource
    {
        private final IServerWorkingCopy wc;
        
        public ConfigResource( final IServerWorkingCopy wc )
        {
            super( null );
            
            this.wc = wc;
        }

        @Override
        protected PropertyBinding createBinding( final Property property )
        {
            final PropertyDef p = property.definition();
            
            if( p == IGlassfishServerModel.PROP_NAME )
            {
                return new ValuePropertyBinding()
                {
                    @Override
                    
                    public String read()
                    {
                        return ConfigResource.this.wc.getName();
                    }
                    
                    @Override
                    
                    public void write( final String value )
                    {
                        ConfigResource.this.wc.setName( value );
                    }
                };
            }
            else if( p == IGlassfishServerModel.PROP_HOST_NAME )
            {
                return new ValuePropertyBinding()
                {
                    private PropertyChangeListener listener;
                    
                    @Override
                    public void init( final Property property )
                    {
                        super.init( property );
                        
                        this.listener = new PropertyChangeListener()
                        {
                            @Override
                            public void propertyChange( final PropertyChangeEvent event )
                            {
                                if( "hostname".equals( event.getPropertyName() ) )
                                {
                                    property().refresh();
                                }
                            }
                        };
                        
                        ConfigResource.this.wc.addPropertyChangeListener( this.listener );
                    }

                    @Override
                    public String read()
                    {
                        return ConfigResource.this.wc.getHost();
                    }
                    
                    @Override
                    public void write( final String value )
                    {
                        ConfigResource.this.wc.setHost( value );
                    }
                    
                    @Override
                    public void dispose()
                    {
                        super.dispose();
                        
                        ConfigResource.this.wc.removePropertyChangeListener( this.listener );
                        this.listener = null;
                    }
                };
            }
            else if( p == IGlassfishServerModel.PROP_ADMIN_NAME )
            {
                return new AttributeValueBinding(wc, ATTR_ADMIN);
            }
            if( p == IGlassfishServerModel.PROP_ADMIN_PASSWORD )
            {
                return new AttributeValueBinding(wc, ATTR_ADMINPASS);
            }
            if( p == IGlassfishServerModel.PROP_ADMIN_PORT )
            {
                return new AttributeValueBinding(wc, ATTR_ADMINPORT);
            }
            if( p == IGlassfishServerModel.PROP_DEBUG_PORT )
            {
                return new AttributeValueBinding(wc, ATTR_DEBUG_PORT);
            }
            if( p == IGlassfishServerModel.PROP_SERVER_PORT )
            {
                return new AttributeValueBinding(wc, ATTR_SERVERPORT);
            }
            if( p == IGlassfishServerModel.PROP_DOMAIN_PATH )
            {
                return new AttributeValueBinding(wc, ATTR_DOMAINPATH);
            }
            if (p == IGlassfishServerModel.PROP_PRESERVE_SESSIONS) {
            	return new AttributeValueBinding(wc, ATTR_KEEPSESSIONS);
            }
            if (p == IGlassfishServerModel.PROP_USE_ANONYMOUS_CONNECTIONS) {
            	return new AttributeValueBinding(wc, ATTR_USEANONYMOUSCONNECTIONS);
            }
            if (p == IGlassfishServerModel.PROP_USE_JAR_DEPLOYMENT) {
            	return new AttributeValueBinding(wc, ATTR_JARDEPLOY);
            }
            
            throw new IllegalStateException();
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public <A> A adapt( final Class<A> adapterType )
        {
            if( adapterType == IServerWorkingCopy.class )
            {
                return (A) this.wc;
            }
            
            return super.adapt( adapterType );
        }
    };
    
    private static class AttributeValueBinding extends ValuePropertyBinding
    {
        private final IServerWorkingCopy wc;
        private final String attribute;
        
        public AttributeValueBinding( final IServerWorkingCopy wc,
                                      final String attribute )
        {
            this.wc = wc;
            this.attribute = attribute;
        }
        
        @Override
        public String read()
        {
            return this.wc.getAttribute( this.attribute, (String) null);
        }
        
        @Override
        public void write( final String value )
        {
            this.wc.setAttribute( this.attribute, value );
        }
    }

	@Override
	public IStatus canModifyModules(IModule[] add, IModule[] remove) {
		 if( add == null || add.length == 0 )
	        {
	            return Status.OK_STATUS;
	        }
	        
	        for( IModule module : add ) 
	        {
	            if (!isModuleSupported(module)) {
	            	return GlassfishToolsPlugin.createErrorStatus("Module is not supported on this server", null);
	            }
	            IStatus s = checkModule(module);
		        if (s.getSeverity() == IStatus.ERROR) {
		        	return s;
		        }
	            IModule[] root = doGetParentModules(module);
	            if( root!=null && root.length>0 && root[0]!=module)
	            	return GlassfishToolsPlugin.createErrorStatus("Web module which is part of an Ear cannot be added as top level module to this server", null);
	        }
	        
	        return Status.OK_STATUS;
	}
	
	protected boolean isModuleSupported(IModule module) {
		return ModuleUtil.isEARModule(module) ||
				ModuleUtil.isWebModule(module) ||
				ModuleUtil.isEJBModule(module);
	}

	@Override
	public IModule[] getChildModules(IModule[] modulePath) {
		if ((modulePath == null) || (modulePath.length == 0)) {
			return new IModule[0];
		}
		IModule module = modulePath[modulePath.length - 1];
		if (module != null && module.getModuleType() != null) {
			IModuleType moduleType = module.getModuleType();
			if (moduleType != null && "jst.ear".equals(moduleType.getId())) { //$NON-NLS-1$
				IEnterpriseApplication enterpriseApplication = (IEnterpriseApplication) module.loadAdapter(
						IEnterpriseApplication.class, null);
				if (enterpriseApplication != null) {
					IModule[] earModules = enterpriseApplication.getModules();
					if (earModules != null) {
						return earModules;
					}
				}
			} else if (moduleType != null && "jst.web".equals(moduleType.getId())) { //$NON-NLS-1$
				IWebModule webModule = (IWebModule) module.loadAdapter(IWebModule.class, null);
				if (webModule != null) {
					IModule[] modules = webModule.getModules();
					return modules;
				}
			}
		}
		return new IModule[0];
	}

	@Override
	public IModule[] getRootModules(IModule module) throws CoreException {
		if ( !isModuleSupported( module ) )
            return null;
        IModule[] parents = doGetParentModules(module);
        if(parents.length>0)
        	return parents;
        return new IModule[] { module };
	}
	
	private IModule[] doGetParentModules(IModule module) {
		IModule[] ears = ServerUtil.getModules("jst.ear"); //$NON-NLS-1$
		ArrayList<IModule> list = new ArrayList<IModule>();
		for (int i = 0; i < ears.length; i++) {
			IEnterpriseApplication ear = (IEnterpriseApplication)ears[i].loadAdapter(IEnterpriseApplication.class,null);
			IModule[] childs = ear.getModules();
			for (int j = 0; j < childs.length; j++) {
				if(childs[j].equals(module))
					list.add(ears[i]);
			}
		}
		return list.toArray(new IModule[list.size()]);
	}
	
	protected IStatus checkModule(final IModule module) {
    	return canSupportModule(module);
    }    
    
    @SuppressWarnings("rawtypes")
	public IStatus canSupportModule(final IModule module) {
        final IProject proj = module.getProject();
        
        if( proj == null )
        {
            return GlassfishToolsPlugin.createErrorStatus( "module type not supported", null);
        }
        
        try 
        {
            final IFacetedProject fproj 
                = ProjectFacetsManager.create( module.getProject() );
            
            if( fproj != null )
            {
                    final org.eclipse.wst.common.project.facet.core.runtime.IRuntime runtime
                        = FacetUtil.getRuntime( getServer().getRuntime() );
                    
                    if( runtime == null )
                    {
                    	return GlassfishToolsPlugin.createErrorStatus( "cannot bridge runtimes", null);
                    }
                    
                    for( Iterator itr = fproj.getProjectFacets().iterator(); itr.hasNext(); )
                    {
                        final IProjectFacetVersion fv = (IProjectFacetVersion) itr.next();
                        
                        if( ! runtime.supports( fv ) )
                        {
                            final String msg 
                                = NLS.bind( Messages.facetNotSupported, fv.toString() );
                            
                            return GlassfishToolsPlugin.createErrorStatus(msg, null);
                        }
                    }
            }
        } 
        catch( CoreException e ) 
        {
            return e.getStatus();
        }
        
        for( IModule child : getChildModules( new IModule[] { module } ) )
        {
            final IStatus st = canSupportModule( child );
            
            if( st.getSeverity() == IStatus.ERROR )
            {
                return st;
            }
        }
        

        return Status.OK_STATUS;
    }

	@Override
	/**
     * @see org.eclipse.wst.server.core.model.ServerDelegate#modifyModules(org.eclipse.wst.server.core.IModule[],
     *      org.eclipse.wst.server.core.IModule[],
     *      org.eclipse.core.runtime.IProgressMonitor)
     */
    public void modifyModules(IModule[] add, IModule[] remove,
            IProgressMonitor monitor) throws CoreException {

    }
    
	@Override
	public URL getModuleRootURL(IModule module) {
		String protocol = Utils.getHttpListenerProtocol(getHost(), getPort());
		String path = getModuleRootPath(module);
		int serverPort = getMonitorPort(getPort());
		String hostname = getHost();
		
		try {
			return new URL(protocol, hostname, serverPort, path);
		} catch (MalformedURLException e) {
			// shouldn't happen
			e.printStackTrace();
		}
		return null;
	}
	
	private String getModuleRootPath(IModule module) {
		if (module==null || module.getProject()==null )
            return "/";
		// if we are dealing with web module, look if there is root ear module
		if (Utils.hasProjectFacet(module, 
				ProjectFacetsManager.getProjectFacet(IModuleConstants.JST_WEB_MODULE))) {
			IModule[] rootEars = getRootEarModulesOnThisServer(module);
			if ((rootEars != null) && (rootEars.length > 0)) {
				return getModuleRootPath(module, rootEars[0]);
			} else {
				// try to get context root from glassfish-web.xml
				IGlassfishWebDeploymentDescriptor webDesc = GlassfishDeploymentDescriptorFactory.getWebDeploymentDescriptor(module.getProject());
				String path = webDesc.getContext();
				if( path!=null )
					return path;
			}
		}
		return "/"+ J2EEProjectUtilities.getServerContextRoot(module.getProject());
	}
	
	/**
     * Return the context root of a web module in ther parent EAR.
     * @param module: the web module
     * @param parent: the EAR module
     * @return
     */
    private String getModuleRootPath(IModule module, IModule parent ) {
        String context = Utils.getAppWebContextRoot(parent, module);
        
        if (context!=null && context.length()>0){
            try {
				context = URLEncoder.encode(context, "UTF-8");
				return "/"+context;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
        }
        return "/";
    }
	
	/**
     * Return only the root modules already being added to this Glassfish server
     */
	private IModule[] getRootEarModulesOnThisServer(IModule module) {
		// determine the root
		IModule[] ear = J2EEUtil.getEnterpriseApplications(module, null);
		if (ear != null && ear.length > 0) {
			ArrayList<IModule> ret = new ArrayList<IModule>();
			// Return only the EAR modules on current server.
			HashSet<IModule> allmodules = new HashSet<IModule>(Arrays.asList(getServer().getModules()));
			for (int i = 0; i < ear.length; i++) {
				if (allmodules.contains(ear[i])) {
					ret.add(ear[i]);
				}
			}
			return ret.toArray(new IModule[ret.size()]);
		}
		return null;
	}
	
	private int getMonitorPort(int configedPort) {
        IServerMonitorManager manager = ServerMonitorManager.getInstance();
        for (IMonitoredServerPort port: manager.getMonitoredPorts(getServer())) {
            if (port.getServerPort().getPort()==configedPort)
                return port.getMonitorPort();
        }
        return configedPort;
    }
    
}