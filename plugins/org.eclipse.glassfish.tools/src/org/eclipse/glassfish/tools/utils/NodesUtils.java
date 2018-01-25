/******************************************************************************
 * Copyright (c) 2018 Oracle
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.glassfish.tools.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.glassfish.tools.GlassFishServer;
import org.eclipse.glassfish.tools.sdk.TaskState;
import org.eclipse.glassfish.tools.sdk.admin.Command;
import org.eclipse.glassfish.tools.sdk.admin.CommandListComponents;
import org.eclipse.glassfish.tools.sdk.admin.CommandListResources;
import org.eclipse.glassfish.tools.sdk.admin.CommandListWebServices;
import org.eclipse.glassfish.tools.sdk.admin.ResultList;
import org.eclipse.glassfish.tools.sdk.admin.ResultMap;
import org.eclipse.glassfish.tools.sdk.admin.ServerAdmin;
import org.eclipse.glassfish.tools.sdk.data.IdeContext;
import org.eclipse.glassfish.tools.serverview.AppDesc;
import org.eclipse.glassfish.tools.serverview.ResourceDesc;
import org.eclipse.glassfish.tools.serverview.WSDesc;

public class NodesUtils {

	public static List<ResourceDesc> getResources(GlassFishServer server, String type) {
        List<String> result = Collections.emptyList();
        LinkedList<ResourceDesc> retVal = null;
        try {
        	Command command = new CommandListResources(CommandListResources.command(
                    type), null);
                Future<ResultList<String>> future = ServerAdmin.<ResultList<String>>
                exec(server, command, new IdeContext());
            ResultList<String> res = future.get();
            if (TaskState.COMPLETED.equals(res.getState())) {
                result = res.getValue();
            }
            retVal = new LinkedList<ResourceDesc>();
            for (String rsc : result) {
            	retVal.add(new ResourceDesc(rsc, type));
            }
        } catch (InterruptedException ex) {
            Logger.getLogger("glassfish").log(Level.INFO, ex.getMessage(), ex);
        } catch (Exception ex) {
            Logger.getLogger("glassfish").log(Level.INFO, ex.getMessage(), ex);
        }
        return retVal;
    }
	
	
	public static Map<String, List<AppDesc>> getApplications(
			GlassFishServer server, String container) {
		Map<String, List<String>> apps = Collections.emptyMap();
		Command command = new CommandListComponents(null);
		Future<ResultMap<String, List<String>>> future = ServerAdmin
				.<ResultMap<String, List<String>>> exec(server, command,
						new IdeContext());
		ResultMap<String, List<String>> result = null;
		try {
			result = future.get(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Logger.getLogger("glassfish").log(Level.INFO, e.getMessage(), e); // NOI18N
		} catch (ExecutionException e) {
			Logger.getLogger("glassfish").log(Level.INFO, e.getMessage(), e); // NOI18N
		} catch (TimeoutException e) {
			Logger.getLogger("glassfish").log(Level.INFO, e.getMessage(), e); // NOI18N
		}
		if (result != null && result.getState().equals(TaskState.COMPLETED)) {
			apps = result.getValue();
		}
		if (apps == null || apps.isEmpty()) {
			return Collections.emptyMap();
		}
		return processApplications(apps);
		// ServerCommand.GetPropertyCommand getCmd = new
		// ServerCommand.GetPropertyCommand("applications.application.*");
		// serverCmd = getCmd;
		// task = executor().submit(this);
		// state = task.get(30, TimeUnit.SECONDS);
		// if (state == OperationState.COMPLETED) {
		// result = processApplications(apps, getCmd.getData());
		// }
	}

	private static Map<String, List<AppDesc>> processApplications(
			Map<String, List<String>> appsList) {
		Map<String, List<AppDesc>> result = new HashMap<String, List<AppDesc>>();
		for( final Map.Entry<String,List<String>> entry : appsList.entrySet() ) {
			final String engine = entry.getKey();
			final List<String> apps = entry.getValue();
			for (int i = 0; i < apps.size(); i++) {
				String name = apps.get(i).trim();
				// String appname = "applications.application." + name; //
				// NOI18N
				// String contextKey = appname + ".context-root"; // NOI18N
				// String pathKey = appname + ".location"; // NOI18N

				List<AppDesc> appList = result.get(engine);
				if (appList == null) {
					appList = new ArrayList<AppDesc>();
					result.put(engine, appList);
				}
				appList.add(new AppDesc(name, null, null, engine));
			}
		}
		return result;
	}

	/**
	 * Sends list-web-services command to server (synchronous)
	 * 
	 * @return String array of names of deployed applications.
	 */
	public static List<WSDesc> getWebServices(GlassFishServer server) {
		List<String> wssList = Collections.emptyList();
		Command command = new CommandListWebServices();
		Future<ResultList<String>> future = ServerAdmin
				.<ResultList<String>> exec(server, command, new IdeContext());
		ResultList<String> result = null;
		try {
			result = future.get(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Logger.getLogger("glassfish").log(Level.INFO, e.getMessage(), e); // NOI18N
		} catch (ExecutionException e) {
			Logger.getLogger("glassfish").log(Level.INFO, e.getMessage(), e); // NOI18N
		} catch (TimeoutException e) {
			Logger.getLogger("glassfish").log(Level.INFO, e.getMessage(), e); // NOI18N
		}
		if (result != null && result.getState().equals(TaskState.COMPLETED)) {
			wssList = result.getValue();
		}
		if (wssList == null || wssList.isEmpty()) {
			return Collections.emptyList();
		}
		return processWebServices(wssList);

	}

	private static List<WSDesc> processWebServices(List<String> wssList) {
		List<WSDesc> result = new ArrayList<WSDesc>();
		for (String a : wssList) {
			result.add(new WSDesc(a, a + "?wsdl", a + "?Tester")); // NOI18N
		}
		return result;
	}
	
	public static Map<String, String> getResourceData(GlassFishServer server, String name) {
        return ResourceUtils.getResourceData(server, name);
    }
	
	public static void putResourceData(GlassFishServer server, Map<String, String> data) throws PartialCompletionException {
        ResourceUtils.putResourceData(server, data);
    }

}