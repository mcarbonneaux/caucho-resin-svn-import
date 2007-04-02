/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.config.ConfigException;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.enhancer.ByteCodeEnhancer;
import com.caucho.loader.enhancer.EnhancerRuntimeException;
import com.caucho.make.AlwaysModified;
import com.caucho.make.DependencyContainer;
import com.caucho.make.Make;
import com.caucho.make.MakeContainer;
import com.caucho.management.server.*;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.ByteBuffer;
import com.caucho.util.L10N;
import com.caucho.util.TimedCache;
import com.caucho.vfs.*;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.security.*;
import java.lang.instrument.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 *
 * <p>DynamicClassLoaders can be chained creating one virtual class loader.
 * From the perspective of the JDK, it's all one classloader.  Internally,
 * the class loader chain searches like a classpath.
 */
public class ProLoader extends SecureClassLoader
{
  private Path _resinHome;
  private Path _libexec;
  private JarPath _proJar;
  private JarPath _licenseJar;
  
  /**
   * Create a new class loader.
   *
   * @param parent parent class loader
   */
  private ProLoader(Path resinHome)
  {
    super(ClassLoader.getSystemClassLoader());

    _resinHome = resinHome;

    boolean is64bit = "64".equals(System.getProperty("sun.arch.data.model"));
                                  
    if (is64bit)
      _libexec = _resinHome.lookup("libexec64");
    else
      _libexec = _resinHome.lookup("libexec");
    
    _proJar = JarPath.create(_resinHome.lookup("lib/pro.jar"));
    _licenseJar = JarPath.create(_resinHome.lookup("lib/license.jar"));
  }

  static ProLoader create(Path resinHome)
  {
    if (resinHome.lookup("lib/pro.jar").canRead())
      return new ProLoader(resinHome);
    else
      return null;
  }
  
  /**
   * Load a class using this class loader
   *
   * @param name the classname to load
   * @param resolve if true, resolve the class
   *
   * @return the loaded classes
   */
  // XXX: added synchronized for RSN-373
  protected synchronized Class loadClass(String name, boolean resolve)
    throws ClassNotFoundException
  {
    String className = name.replace('.', '/') + ".class";

    Path path = _proJar.lookup(className);

    if (path.getLength() < 0)
      path = _licenseJar.lookup(className);

    int length = (int) path.getLength();

    if (length > 0) {
      byte []buffer = new byte[length];

      try {
        ReadStream is = null;
        try {
          is = path.openRead();

          is.readAll(buffer, 0, buffer.length);

          Class cl = defineClass(name, buffer, 0, buffer.length,
                                 (CodeSource) null);

          return cl;
        } finally {
          is.close();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    
    return super.loadClass(name, resolve);
  }

  /**
   * Load a class using this class loader
   *
   * @param name the classname using either '/' or '.'
   *
   * @return the loaded class
   */
  protected Class findClass(String name)
    throws ClassNotFoundException
  {
    return super.findClass(name);
  }

  /**
   * Returns the full library path for the name.
   */
  public String findLibrary(String name)
  {
    Path path = _libexec.lookup("lib" + name + ".so");

    if (path.canRead()) {
      return path.getNativePath();
    }
    
    path = _libexec.lookup("lib" + name + ".jnilib");

    if (path.canRead()) {
      return path.getNativePath();
    }

    return super.findLibrary(name);
  }
}
