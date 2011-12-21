/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.boot;

import java.io.IOException;

import com.caucho.config.ConfigException;
import com.caucho.env.repository.CommitBuilder;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

public class ConfigLsCommand extends AbstractRepositoryCommand {
  private static final L10N L = new L10N(ConfigLsCommand.class);
  
  @Override
  public boolean isDefaultArgsAccepted()
  {
    return true;
  }
  
  @Override
  public String getDescription()
  {
    return "lists the configuration files";
  }
  
  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       WebAppDeployClient deployClient)
  {
    String fileName = args.getDefaultArg();
    
    String name = args.getArg("-name");
    
    CommitBuilder commit = new CommitBuilder();
    commit.type("config");
    
    String stage = args.getArg("-stage");
    
    if (stage != null)
      commit.stage(stage);
    
    if (name == null) {
      name = "resin";
    }
    
    commit.tagKey(name);

    try {
      String []files = deployClient.listFiles(commit.getId(), fileName);
      
      if (files != null) {
        for (String file : files) {
          System.out.println(file);
        }
      }
    } catch (IOException e) {
      ConfigException.create(e);
    }

    return 0;
  }
  
  @Override
  public String getUsageArgs()
  {
    return " <filename>";
  }
}
