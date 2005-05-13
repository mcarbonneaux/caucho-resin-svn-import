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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb;

import java.io.*;
import java.util.*;
import java.rmi.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.*;

import javax.ejb.*;
import javax.sql.*;
import javax.naming.*;

import javax.jms.ConnectionFactory;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.loader.EnvironmentBean;
import com.caucho.config.ConfigException;

import com.caucho.config.types.FileSetType;
import com.caucho.config.types.JndiBuilder;
import com.caucho.config.types.PathPatternType;
import com.caucho.config.types.Period;

import com.caucho.sql.*;
import com.caucho.ejb.*;
import com.caucho.naming.*;
import com.caucho.naming.Jndi;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentClassLoader;

import com.caucho.log.Log;

import com.caucho.naming.AbstractModel;
import com.caucho.naming.ContextImpl;

import com.caucho.ejb.cfg.EjbMethod;

import com.caucho.ejb.protocol.ProtocolContainer;

import com.caucho.ejb.admin.*;

import com.caucho.ejb.metadata.Bean;

import com.caucho.ejb.entity2.EntityIntrospector;

/**
 * Server containing all the EJBs for a given configuration.
 *
 * <p>Each protocol will extend the container to override Handle creation.
 */
public class EJBServer
  implements ObjectProxy, EnvironmentListener, EJBServerInterface,
	     EnvironmentBean {
  static final L10N L = new L10N(EJBServer.class);
  protected static final Logger log = Log.open(EJBServer.class);
  
  protected static EnvironmentLocal<String> _localURL =
    new EnvironmentLocal<String>("caucho.url");

  private String _jndiName = "java:comp/env/cmp";

  private EjbServerManager _ejbManager;
  private AbstractModel _rootModel;
  
  private ArrayList<Path> _descriptors;
  private ArrayList<Path> _ejbJars = new ArrayList<Path>();

  private EntityIntrospector _entityIntrospector;

  private MergePath _mergePath;
  
  private String _urlPrefix;
  
  private ArrayList<FileSetType> _configFileSetList =
    new ArrayList<FileSetType>();

  private DataSource _dataSource;
  private boolean _createDatabaseSchema;
  private boolean _validateDatabaseSchema = true;
  
  private boolean _entityLoadLazyOnTransaction = true;

  private String _resinIsolation;
  private String _jdbcIsolation;

  private ConnectionFactory _jmsConnectionFactory;
  
  private int _entityCacheSize = 32 * 1024;
  private long _entityCacheTimeout = 5000;

  private boolean _forbidJVMCall;
  private boolean _autoCompile = true;
  private boolean _isAllowPOJO = false;

  private String _startupMode;

  private long _transactionTimeout = 0;

  /**
   * Create a server with the given prefix name.
   */
  public EJBServer()
    throws ConfigException
  {
    _ejbManager = new EjbServerManager();
    _rootModel = _ejbManager.getProtocolManager().getNamingRoot();

    _urlPrefix = _localURL.get();

    _mergePath = new MergePath();
    _mergePath.addMergePath(Vfs.lookup());
    _mergePath.addClassPath();

    _entityIntrospector = new EntityIntrospector(_ejbManager);
  }

  /**
   * Returns the local EJB server.
   */
  public static EnvServerManager getLocalManager()
  {
    return EnvServerManager.getLocal();
  }

  /**
   * Gets the environment class loader.
   */
  public EnvironmentClassLoader getClassLoader()
  {
    return _ejbManager.getClassLoader();
  }

  /**
   * Sets the environment class loader.
   */
  public void setEnvironmentClassLoader(EnvironmentClassLoader env)
  {
  }
    
  /**
   * Sets the JNDI name.
   */
  public void setName(String name)
  {
    setJndiName(name);
  }

  /**
   * Sets the JNDI name.
   */
  public void setJndiName(String name)
  {
    _jndiName = name;
  }

  /**
   * Gets the JNDI name.
   */
  public String getJndiName()
  {
    return _jndiName;
  }

  /**
   * Sets the URL-prefix for all external beans.
   */
  public void setURLPrefix(String urlPrefix)
  {
    _urlPrefix = urlPrefix;
  }

  /**
   * Gets the URL-prefix for all external beans.
   */
  public String getURLPrefix()
  {
    return _urlPrefix;
  }

  /**
   * Sets the directory for the *.ejb files.
   */
  public void setConfigDirectory(Path dir)
    throws ConfigException
  {
    FileSetType fileSet = new FileSetType();

    fileSet.setDir(dir);
    
    fileSet.addInclude(new PathPatternType("**/*.ejb"));

    Path pwd = Vfs.lookup();

    String dirPath = dir.getPath();
    String pwdPath = pwd.getPath();
    
    if (dirPath.startsWith(pwdPath)) {
      String prefix = dirPath.substring(pwdPath.length());

      fileSet.setUserPathPrefix(prefix);
    }

    _ejbManager.addConfigFiles(fileSet);
  }

  /**
   * Adds an ejb descriptor.
   */
  public void addEJBDescriptor(String ejbDescriptor)
  {
    if (_descriptors == null)
      _descriptors = new ArrayList<Path>();

    Path path = _mergePath.lookup(ejbDescriptor);

    _descriptors.add(path);
  }

  /**
   * Adds an ejb jar.
   */
  public void addEJBJar(Path ejbJar)
    throws ConfigException
  {
    if (! ejbJar.canRead() || ! ejbJar.isFile())
      throw new ConfigException(L.l("<ejb-jar> {0} must refer to a valid jar file.",
				    ejbJar.getURL()));
    
    _ejbJars.add(ejbJar);
  }

  /**
   * Adds a bean.
   */
  public Bean createBean()
  {
    return new Bean(_ejbManager, _entityIntrospector);
  }

  /**
   * Adds a bean.
   */
  public void addBean(Bean bean)
  {
  }

  /**
   * Sets the data-source
   */
  public void setDataSource(DataSource dataSource)
    throws ConfigException
  {
    _dataSource = dataSource;

    if (_dataSource == null)
      throw new ConfigException(L.l("<ejb-server> data-source must be a valid DataSource."));

    _ejbManager.setDataSource(_dataSource);
  }

  /**
   * Sets the data-source
   */
  public void setReadDataSource(DataSource dataSource)
    throws ConfigException
  {
    _ejbManager.setReadDataSource(dataSource);
  }

  /**
   * Sets the xa data-source
   */
  public void setXADataSource(DataSource dataSource)
    throws ConfigException
  {
    _ejbManager.setXADataSource(dataSource);
  }

  /**
   * Sets true if database schema should be generated automatically.
   */
  public void setCreateDatabaseSchema(boolean create)
  {
    _createDatabaseSchema = create;
  }

  /**
   * True if database schema should be generated automatically.
   */
  public boolean getCreateDatabaseSchema()
  {
    return _createDatabaseSchema;
  }

  /**
   * Sets true if database schema should be validated automatically.
   */
  public void setValidateDatabaseSchema(boolean validate)
  {
    _validateDatabaseSchema = validate;
  }

  /**
   * True if database schema should be validated automatically.
   */
  public boolean getValidateDatabaseSchema()
  {
    return _validateDatabaseSchema;
  }

  /**
   * Sets true if database schema should be validated automatically.
   */
  public void setLoadLazyOnTransaction(boolean isLazy)
  {
    _ejbManager.setEntityLoadLazyOnTransaction(isLazy);
  }

  /**
   * Sets the jndi name of the jmsConnectionFactory
   */
  public void setJMSConnectionFactory(JndiBuilder factory)
    throws ConfigException, NamingException
  {
    Object obj = factory.getObject();

    if (! (obj instanceof ConnectionFactory))
      throw new ConfigException(L.l("`{0}' must be a JMS ConnectionFactory.", obj));
		      
    _jmsConnectionFactory = (ConnectionFactory) obj;
  }

  /**
   * Gets the jndi name of the jmsQueueConnectionFactory
   */
  public ConnectionFactory getConnectionFactory()
  {
    return _jmsConnectionFactory;
  }

  /**
   * Sets consumer max
   */
  public void setMessageConsumerMax(int consumerMax)
    throws ConfigException, NamingException
  {
    _ejbManager.setMessageConsumerMax(consumerMax);
  }

  /**
   * Gets the entity cache size.
   */
  public int getEntityCacheSize()
  {
    return _entityCacheSize;
  }

  /**
   * Sets the entity cache size.
   */
  public void setCacheSize(int size)
  {
    _entityCacheSize = size;
  }

  /**
   * Gets the entity cache timeout.
   */
  public long getEntityCacheTimeout()
  {
    return _entityCacheTimeout;
  }

  /**
   * Sets the entity cache timeout.
   */
  public void setCacheTimeout(Period timeout)
  {
    _entityCacheTimeout = timeout.getPeriod();
  }

  /**
   * Gets transaction timeout.
   */
  public long getTransactionTimeout()
  {
    return _transactionTimeout;
  }

  /**
   * Sets the transaction timeout.
   */
  public void setTransactionTimeout(Period timeout)
  {
    _transactionTimeout = timeout.getPeriod();
  }

  /**
   * Gets the Resin isolation.
   */
  public String getResinIsolation()
  {
    return _resinIsolation;
  }

  /**
   * Sets the Resin isolation.
   */
  public void setResinIsolation(String resinIsolation)
  {
    _resinIsolation = resinIsolation;
  }

  /**
   * Gets the JDBC isolation.
   */
  public String getJdbcIsolation()
  {
    return _jdbcIsolation;
  }

  /**
   * Sets the JDBC isolation.
   */
  public void setJdbcIsolation(String jdbcIsolation)
  {
    _jdbcIsolation = jdbcIsolation;
  }

  /**
   * If true, JVM calls are forbidden.
   */
  public void setForbidJvmCall(boolean forbid)
  {
    _forbidJVMCall = forbid;
  }

  /**
   * If true, automatically compile old EJBs.
   */
  public boolean isAutoCompile()
  {
    return _autoCompile;
  }

  /**
   * Set true to automatically compile old EJBs.
   */
  public void setAutoCompile(boolean autoCompile)
  {
    _autoCompile = autoCompile;
  }

  /**
   * If true, allow POJO beans
   */
  public boolean isAllowPOJO()
  {
    return _isAllowPOJO;
  }

  /**
   * Set true to allow POJO beans
   */
  public void setAllowPOJO(boolean allowPOJO)
  {
    _isAllowPOJO = allowPOJO;
  }

  /**
   * Sets the EJB server startup mode.
   */
  public void setStartupMode(String startupMode)
  {
    _startupMode = startupMode;
  }

  /**
   * Initialize the container.
   */
  public void init()
    throws Exception
  {
    String name = _jndiName;
    if (! name.startsWith("java:"))
      name = "java:comp/env/" + name;
    
    try {
      Jndi.rebindDeep(name, this);
    } catch (NamingException e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if ("manual".equals(_startupMode))
      return;

    manualInit();
  }
  
  /**
   * Initialize the container.
   */
  public void manualInit()
    throws Exception
  {
    try {
      log.fine("Initializing ejb-server : " + _jndiName);

      ProtocolContainer protocol = new ProtocolContainer();
      if (_urlPrefix != null)
        protocol.setURLPrefix(_urlPrefix);
      
      protocol.setServerManager(_ejbManager.getEnvServerManager());
    
      _ejbManager.getProtocolManager().setProtocolContainer(protocol);

      _ejbManager.setDataSource(_dataSource);
      _ejbManager.setCreateDatabaseSchema(_createDatabaseSchema);
      _ejbManager.setValidateDatabaseSchema(_validateDatabaseSchema);
      _ejbManager.setJMSConnectionFactory(_jmsConnectionFactory);
      _ejbManager.setCacheSize(_entityCacheSize);
      _ejbManager.setCacheTimeout(_entityCacheTimeout);
      _ejbManager.setTransactionTimeout(_transactionTimeout);
      _ejbManager.setAllowJVMCall(! _forbidJVMCall);
      _ejbManager.setAutoCompile(_autoCompile);
      _ejbManager.setAllowPOJO(isAllowPOJO());

      int resinIsolation = -1;

      if (_resinIsolation == null) {
      }
      else if (_resinIsolation.equals("row-locking"))
        resinIsolation = EjbMethod.RESIN_ROW_LOCKING;
      else if (_resinIsolation.equals("database"))
        resinIsolation = EjbMethod.RESIN_DATABASE;
      else {
        throw new ConfigException(L.l("resin-isolation may only be `row-locking' or `database' in EJBServer, not `{0}'", _resinIsolation));
      }
    
      _ejbManager.setResinIsolation(resinIsolation);

      int jdbcIsolation = -1;

      if (_jdbcIsolation == null) {
      }
      else if (_jdbcIsolation.equals("none"))
        jdbcIsolation = java.sql.Connection.TRANSACTION_NONE;
      else if (_jdbcIsolation.equals("read-committed"))
        jdbcIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED;
      else if (_jdbcIsolation.equals("read-uncommitted"))
        jdbcIsolation = java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
      else if (_jdbcIsolation.equals("repeatable-read"))
        jdbcIsolation = java.sql.Connection.TRANSACTION_REPEATABLE_READ;
      else if (_jdbcIsolation.equals("serializable"))
        jdbcIsolation = java.sql.Connection.TRANSACTION_SERIALIZABLE;
      else
        throw new ConfigException(L.l("unknown value for jdbc-isolation at `{0}'",
                                      _jdbcIsolation));
    
      _ejbManager.setJDBCIsolation(jdbcIsolation);

      _entityIntrospector.init();

      initAllEjbs();

      _ejbManager.init();

      /*
      String name = _jndiName;
      if (! name.startsWith("java:"))
        name = "java:comp/env/" + name;

      Jndi.bindDeep(name, this);
      */

      Environment.addEnvironmentListener(this);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      throw e;
    }
  }
  
  /**
   * Initialize all EJBs for any *.ejb or ejb-jar.xml in the WEB-INF or
   * in a META-INF in the classpath.
   */
  public void initEJBs()
    throws Exception
  {
    manualInit();

    /*
    addEJBJars();

    _ejbManager.initJars();
    */
  }
  
  /**
   * Initialize all EJBs for any *.ejb or ejb-jar.xml in the WEB-INF or
   * in a META-INF in the classpath.
   */
  private void initAllEjbs()
    throws Exception
  {
    /*
    MergePath mergePath = new MergePath();
    
    if (_ejbConfigDir != null)
      mergePath.addMergePath(_ejbConfigDir);
    mergePath.addClassPath();
    mergePath.addMergePath(CauchoSystem.getWorkPath());
    */

    //_ejbManager.setSearchPath(mergePath);
    //_ejbManager.setWorkPath(CauchoSystem.getWorkPath());

    addEJBJars();
    
    if (_descriptors != null) {
      for (int i = 0; i < _descriptors.size(); i++) {
        Path path = _descriptors.get(i);

        // XXX: app.addDepend(path);
        _ejbManager.addEJBPath(path);
      }
    }

    /*
    if (_ejbConfigDir != null) {
      Path dir = _ejbConfigDir;
      String userPath = dir.getUserPath();
      if (! userPath.endsWith("/") && ! userPath.equals(""))
	userPath = userPath + "/";

      String []list = dir.list();
      for (int j = 0; j < list.length; j++) {
        String name = list[j];

	System.out.println("N: " + name);

        if (name.endsWith(".ejb")) {
          Path path = dir.lookup(name);
	  
          path.setUserPath(userPath + name);

          if (path.exists()) {
            // XXX: app.addDepend(path);
            _ejbManager.addEJBPath(path);
          }
        }
      }
    }
    */
  }

  private void addEJBJars()
    throws Exception
  {
    for (int i = 0; i < _ejbJars.size(); i++) {
      Path path = _ejbJars.get(i);
	
      Environment.addDependency(path);
	  
      JarPath jar = JarPath.create(path);

      _ejbManager.addEJBJar(jar);
      /*
      Path descriptorPath = jar.lookup("META-INF/ejb-jar.xml");

      if (descriptorPath.exists())
	_ejbManager.addEJBPath(descriptorPath);

      descriptorPath = jar.lookup("META-INF/resin-ejb-jar.xml");

      if (descriptorPath.exists())
	_ejbManager.addEJBPath(descriptorPath);

      Path metaInf = jar.lookup("META-INF");
      if (! metaInf.isDirectory())
	continue;
	  
      String []metaList = metaInf.list();
      for (int j = 0; j < metaList.length; j++) {
	String metaName = metaList[j];
	if (metaName.endsWith(".ejb")) {
	  Path metaPath = metaInf.lookup(metaName);
	  _ejbManager.addEJBPath(metaPath);
	}
      }
      */
    }
  }

  public Object createObject(Hashtable env)
  {
    return new ContextImpl(_rootModel, env);
  }
  
  /**
   * Handles the case where a class loader is activated.
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
  }
  
  /**
   * Handles the case where a class loader is dropped.
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    close();
  }

  public void close()
  {
    _ejbManager.destroy();
  }
}

