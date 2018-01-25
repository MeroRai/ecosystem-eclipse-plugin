/******************************************************************************
 * Copyright (c) 2018 Oracle
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.glassfish.tools.utils;

import org.eclipse.wst.server.core.ServerCore;

/**
 * @author <a href="mailto:konstantin.komissarchik@oracle.com">Konstantin Komissarchik</a>
 */

public final class WtpUtil
{
    public static final String findUniqueServerName( final String base )
    {
        int i = 1;
        String name = base;
        
        while( ServerCore.findServer( name ) != null )
        {
            i++;
            name = base + " (" + i + ")";
        }
        
        return name;
    }
    	
    public static final String findUniqueRuntimeName( final String base )
    {
        int i = 1;
        String name = base;
        
        while( ServerCore.findRuntime( name ) != null )
        {
            i++;
            name = base + " (" + i + ")";
        }
        
        return name;
    }
}


