/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.resin.eclipse;

import java.io.File;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jst.server.generic.core.internal.CorePlugin;
import org.eclipse.jst.server.generic.core.internal.GenericServerBehaviour;
import org.eclipse.jst.server.generic.internal.core.util.FileUtil;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;

@SuppressWarnings("restriction")
public class ResinServerBehaviour extends GenericServerBehaviour
{
  public static final String RESIN_CONFIGURATION_FILE_NAME_ID =
    "resin.configuration.file";
  
  @Override
  public void setupLaunchConfiguration(
      ILaunchConfigurationWorkingCopy workingCopy, IProgressMonitor monitor)
    throws CoreException
  {
    ServerRuntime typeDef = getRuntimeDelegate().getServerTypeDefinition();
    String filename = 
      PublisherUtil.getPublisherData(typeDef, ResinGitPublisher.PUBLISHER_ID,
                                     RESIN_CONFIGURATION_FILE_NAME_ID);
    if (filename != null) {
      File configFile = PublisherUtil.locateBundleFile(typeDef, filename);
      VariableUtil.setVariable(RESIN_CONFIGURATION_FILE_NAME_ID, 
                               configFile.toString());

      // create a webapp deploy directory in case we're doing hot deploy
      String dir = CorePlugin.getDefault().getStateLocation().toOSString(); 
      File tempFile = FileUtil.createTempFile("webapps", dir);

      VariableUtil.setVariable("wtp.webapp.deploydir", tempFile.toString());
    }

    super.setupLaunchConfiguration(workingCopy, monitor);
  }


  @Override
  public void stop(boolean force)
  {
    // change the default behaviour and always force the stop,
    // which causes eclipse to just terminate the process and
    // not run the <stop> defined in the serverdef file
    super.stop(true);
  }
}
