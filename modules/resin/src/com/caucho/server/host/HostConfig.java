/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.server.host;

import java.util.ArrayList;

import java.util.logging.Logger;

import java.util.regex.Pattern;

import com.caucho.util.L10N;

import com.caucho.vfs.Path;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;
import com.caucho.config.BuilderProgram;
import com.caucho.config.BuilderProgramContainer;

import com.caucho.config.types.RawString;

import com.caucho.server.deploy.DeployController;

/**
 * The configuration for a host in the resin.conf
 */
public class HostConfig {
  static final L10N L = new L10N(HostConfig.class);
  static final Logger log = Log.open(HostConfig.class);

  // The host name
  private String _hostName;

  // The raw host aliases
  private ArrayList<String> _hostAliases = new ArrayList<String>();
  
  private ArrayList<Pattern> _hostAliasRegexps
    = new ArrayList<Pattern>();

  // The regexp pattern
  private Pattern _regexp;

  // The root dir
  private String _rootDir;

  // The startup mode
  private String _startupMode = DeployController.STARTUP_AUTOMATIC;
  
  // The configuration program
  private BuilderProgramContainer _program = new BuilderProgramContainer();

  /**
   * Sets the host name.
   */
  public void setHostName(RawString name)
    throws ConfigException
  {
    _hostName = name.getValue();

    if (_hostName.indexOf("${") < 0) {
      for (int i = 0; i < _hostName.length(); i++) {
	char ch = _hostName.charAt(i);

	if (ch == ' ' || ch == '\t' || ch == ',') {
	  throw new ConfigException(L.l("Host name `{0}' must not contain multiple names.  Use <host-alias> to specify aliases for a host.",
					_hostName));
	}
      }
    }
  }
  
  /**
   * Gets the host name.
   */
  public String getHostName()
  {
    return _hostName;
  }

  /**
   * Sets the id.
   */
  public void setId(RawString id)
    throws ConfigException
  {
    setHostName(id);
  }

  /**
   * Adds a host alias.
   */
  public void addHostAlias(RawString rawName)
    throws ConfigException
  {
    String name = rawName.getValue().trim();

    if (name.indexOf("${") < 0) {
      for (int i = 0; i < name.length(); i++) {
	char ch = name.charAt(i);

	if (ch == ' ' || ch == '\t' || ch == ',') {
	  throw new ConfigException(L.l("<host-alias> `{0}' must not contain multiple names.  Use multiple <host-alias> tags to specify aliases for a host.",
					name));
	}
      }
    }

    if (! _hostAliases.contains(name))
      _hostAliases.add(name);
  }

  /**
   * Returns the host aliases.
   */
  public ArrayList<String> getHostAliases()
  {
    return _hostAliases;
  }
  
  /**
   * Adds a host alias regexp.
   */
  public void addHostAliasRegexp(String name)
  {
    name = name.trim();

    Pattern pattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);

    if (! _hostAliasRegexps.contains(pattern))
      _hostAliasRegexps.add(pattern);
  }

  /**
   * Returns the host aliases regexps.
   */
  public ArrayList<Pattern> getHostAliasRegexps()
  {
    return _hostAliasRegexps;
  }

  /**
   * Sets the regexp.
   */
  public void setRegexp(RawString regexp)
  {
    String value = regexp.getValue();

    if (! value.endsWith("$"))
      value = value + "$";
    
    _regexp = Pattern.compile(value, Pattern.CASE_INSENSITIVE);
  }

  /**
   * Gets the regexp.
   */
  public Pattern getRegexp()
  {
    return _regexp;
  }

  /**
   * Sets the root-dir.
   */
  public void setRootDirectory(RawString rootDir)
  {
    _rootDir = rootDir.getValue();
  }

  /**
   * Sets the root-dir (obsolete).
   */
  public void setRootDir(RawString rootDir)
  {
    setRootDirectory(rootDir);
  }

  /**
   * Gets the root-dir.
   */
  public String getRootDirectory()
  {
    return _rootDir;
  }

  /**
   * Sets the lazy-init property
   */
  public void setLazyInit(boolean lazyInit)
  {
    if (lazyInit)
      _startupMode = DeployController.STARTUP_LAZY;
    else
      _startupMode = DeployController.STARTUP_AUTOMATIC;
  }

  /**
   * Sets the startup mode property
   */
  public void setStartupMode(String mode)
    throws ConfigException
  {
    _startupMode = DeployController.toStartupCode(mode);
  }

  /**
   * Gets the startup mode property
   */
  public String getStartupMode()
  {
    return _startupMode;
  }

  /**
   * Adds to the builder program.
   */
  public void addBuilderProgram(BuilderProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * Returns the program.
   */
  public BuilderProgram getBuilderProgram()
  {
    return _program;
  }

  /**
   * Initialize the config.
   */
  public void init()
  {
    if (_regexp != null && _hostName == null)
      log.config(L.l("<host regexp=\"{0}\"> should include a <host-name> tag.",
		     _regexp.pattern()));
  }
}
