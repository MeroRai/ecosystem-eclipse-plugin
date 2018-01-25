/******************************************************************************
 * Copyright (c) 2018 Oracle
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.glassfish.tools.sdk.admin;

import java.io.File;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.glassfish.tools.GlassFishServer;
import org.eclipse.glassfish.tools.sdk.GlassFishIdeException;
import org.eclipse.glassfish.tools.sdk.logging.Logger;

/**
 * Command registers resources defined in provided xml file on specified target.
 * <p/>
 * @author Peter Benedikovic, Tomas Kraus
 */
@RunnerHttpClass(runner=RunnerHttpAddResources.class)
@RunnerRestClass(runner=RunnerRestAddResources.class)
public class CommandAddResources extends CommandTarget {
    
    ////////////////////////////////////////////////////////////////////////////
    // Class attributes                                                       //
    ////////////////////////////////////////////////////////////////////////////

    /** Logger instance for this class. */
    private static final Logger LOGGER = new Logger(CommandAddResources.class);

    /** Command string for create-cluster command. */
    private static final String COMMAND = "add-resources";
    
    ////////////////////////////////////////////////////////////////////////////
    // Static methods                                                         //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Add resource to target server.
     * <p/>
     * @param server              GlassFish server entity.
     * @param xmlResourceFile     File object pointing to XML file containing
     *                            resources to be added.
     * @param target              GlassFish server target.
     * @return Add resource task response.
     * @throws GlassFishIdeException When error occurred during administration
     *         command execution.
     */
    public static ResultString addResource(final GlassFishServer server,
            final File xmlResourceFile, final String target)
            throws GlassFishIdeException {
        final String METHOD = "addResource";
        Command command = new CommandAddResources(xmlResourceFile, target);
        Future<ResultString> future =
                ServerAdmin.<ResultString>exec(server, command);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException
                | CancellationException ie) {
            throw new GlassFishIdeException(
                    LOGGER.excMsg(METHOD, "exception"), ie);
        }
    }

    /**
     * Add resource to target server.
     * <p/>
     * @param server              GlassFish server entity.
     * @param xmlResourceFile     File object pointing to XML file containing
     *                            resources to be added.
     * @param target              GlassFish server target.
     * @param timeout             Administration command execution timeout [ms].
     * @return Add resource task response.
     * @throws GlassFishIdeException When error occurred during administration
     *         command execution.
     */
    public static ResultString addResource(final GlassFishServer server,
            final File xmlResourceFile, final String target, final long timeout)
            throws GlassFishIdeException {
        final String METHOD = "addResource";
        Command command = new CommandAddResources(xmlResourceFile, target);
        Future<ResultString> future =
                ServerAdmin.<ResultString>exec(server, command);
        try {
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException
                | CancellationException ie) {
            throw new GlassFishIdeException(
                    LOGGER.excMsg(METHOD, "exception"), ie);
        } catch (TimeoutException te) {
            throw new GlassFishIdeException(LOGGER.excMsg(METHOD,
                    "exceptionWithTimeout", Long.toString(timeout)), te);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Instance attributes                                                    //
    ////////////////////////////////////////////////////////////////////////////
    
    /** File object pointing to xml file that contains resources to be added. */
    File xmlResFile;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors                                                           //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs an instance of GlassFish server add-resources command entity.
     * <p/>
     * @param xmlResourceFile     File object pointing to XML file containing
     *                            resources to be added.
     * @param target              GlassFish server target.
     */
    public CommandAddResources(
            final File xmlResourceFile, final String target) {
        super(COMMAND, target);
        xmlResFile = xmlResourceFile;
    }
}