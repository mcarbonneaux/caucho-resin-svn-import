/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.loader;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.repository.ModuleRepository;
import com.caucho.vfs.Path;

import java.util.logging.*;
import javax.annotation.PostConstruct;

/**
 * Adds a new bundle to the current environment
 */
public class ModuleConfig
{
  private static final L10N L = new L10N(ModuleConfig.class);
  private static final Logger log
    = Logger.getLogger(ModuleConfig.class.getName());

  private Path _path;

  private String _org;
  private String _module;
  private String _rev;

  private String _artifact;

  /**
   * Sets a specific path to a jar file
   */
  public void setPath(Path path)
  {
    if (! path.getTail().endsWith(".jar"))
      throw new ConfigException(L.l("module path='{0}' must be a jar file.",
				    path));

    _path = path;
  }

  /**
   * Sets the org for an archive name.
   */
  public void setOrg(String org)
  {
    _org = org;
  }

  /**
   * Sets the module name
   */
  public void setModule(String module)
  {
    _module = module;
  }

  /**
   * Sets the artifact name (defaults to module)
   */
  public void setArtifact(String artifact)
  {
    _artifact = artifact;
  }

  /**
   * Sets the module version
   */
  public void setRev(String rev)
  {
    _rev = rev;
  }

  /**
   * Sets true if the bundle should be started automatically.
   */
  public void setStart(boolean isStart)
  {
  }

  @PostConstruct
  public void init()
  {
    if (_path == null && _module == null)
      throw new ConfigException(L.l("module requires either a 'path' or a 'module' attribute"));

    Path path = null;
    
    if (_path != null) {
      path = _path;
    }
    else {
      ModuleRepository repository = ModuleRepository.getCurrent();

      if (repository == null)
	throw new IllegalStateException(L.l("ModuleRepository is not properly initialized"));

      path = repository.findArtifact(_org, _module, _artifact, _rev, "jar");
    }

    if (path == null)
      throw new ConfigException(L.l("Can't find module '{0}'",
				    _module));

    EnvironmentClassLoader loader = Environment.getEnvironmentClassLoader();

    loader.addJar(path);
  }
}
