/******************************************************************************
 * Copyright (c) 2018 Oracle
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.payara.tools.handlers;

import static org.eclipse.payara.tools.utils.WtpUtil.load;
import static org.eclipse.wst.server.core.IServer.STATE_STARTED;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.payara.tools.server.GlassFishServer;
import org.eclipse.wst.server.core.IServer;

public class GlassFishStateTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		IServer server = (IServer) receiver;
		
		if (property.equals("isRunning")) {
			return (server.getServerState() == STATE_STARTED);
		}
		
		if (property.equals("isRemote")) {
			GlassFishServer gf = load(server, GlassFishServer.class);
			
			if (gf != null) {
				return gf.isRemote();
			}
		}

		return false;
	}

}