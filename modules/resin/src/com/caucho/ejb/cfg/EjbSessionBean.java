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

package com.caucho.ejb.cfg;

import com.caucho.bytecode.JAnnotation;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.config.BuilderProgram;
import com.caucho.config.BuilderProgramContainer;
import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.j2ee.EjbInjectProgram;
import com.caucho.config.j2ee.InjectIntrospector;
import com.caucho.config.j2ee.JndiBindProgram;
import com.caucho.config.types.EnvEntry;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.gen.SessionAssembler;
import com.caucho.ejb.gen.StatelessAssembler;
import com.caucho.ejb.session.SessionServer;
import com.caucho.ejb.session.StatelessServer;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.management.j2ee.J2EEManagedObject;
import com.caucho.management.j2ee.StatefulSessionBean;
import com.caucho.management.j2ee.StatelessSessionBean;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import java.util.ArrayList;

/**
 * Configuration for an ejb entity bean.
 */
public class EjbSessionBean extends EjbBean {
  private static final L10N L = new L10N(EjbSessionBean.class);

  private boolean _isStateless;

  // Default is container managed transaction.
  private boolean _isContainerTransaction = true;

  /**
   * Creates a new session bean configuration.
   */
  public EjbSessionBean(EjbConfig ejbConfig, String ejbModuleName)
  {
    super(ejbConfig, ejbModuleName);
  }

  /**
   * Returns the kind of bean.
   */
  public String getEJBKind()
  {
    return "session";
  }

  /**
   * Sets the ejb implementation class.
   */
  @Override
  public void setEJBClass(String typeName)
    throws ConfigException
  {
    super.setEJBClass(typeName);

    JClass ejbClass = getEJBClassWrapper();

    if (ejbClass.isAbstract())
      throw error(L.l("'{0}' must not be abstract.  Session bean implementations must be fully implemented.", ejbClass.getName()));

    /*
      if (! ejbClass.isAssignableTo(SessionBean.class)
          && ! ejbClass.isAnnotationPresent(Stateless.class)
          && ! ejbClass.isAnnotationPresent(Stateful.class))
        throw error(L.l("'{0}' must implement SessionBean or @Stateless or @Stateful.  Session beans must implement javax.ejb.SessionBean.", ejbClass.getName()));
    */

    introspectSession();
  }

  /**
   * Returns true if it's a stateless session bean.
   */
  public boolean isStateless()
  {
    return _isStateless;
  }

  /**
   * Set true if it's a stateless session bean.
   */
  public void setSessionType(String type)
    throws ConfigException
  {
    if (type.equals("Stateful"))
      _isStateless = false;
    else if (type.equals("Stateless"))
      _isStateless = true;
    else
      throw new ConfigException(L.l("'{0}' is an unknown session-type.  session-type must be 'Stateless' or 'Stateful'.", type));
  }

  /**
   * Returns true if the container handles transactions.
   */
  public boolean isContainerTransaction()
  {
    return _isContainerTransaction;
  }

  /**
   * Set true if the container handles transactions.
   */
  public void setTransactionType(String type)
    throws ConfigException
  {
    if (type.equals("Container"))
      _isContainerTransaction = true;
    else if (type.equals("Bean"))
      _isContainerTransaction = false;
    else
      throw new ConfigException(L.l("'{0}' is an unknown transaction-type.  transaction-type must be 'Container' or 'Bean'.", type));
  }

  /**
   * Configure initialization.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    super.init();

    try {
      if (getRemoteHome() != null)
        validateHome(getRemoteHome(), getRemoteList().get(0));

      if (getLocalHome() != null) {
        validateHome(getLocalHome(), getLocalList().get(0));
      }

      for (JClass remoteApi : getRemoteList())
        validateRemote(remoteApi);

      for (JClass localApi : getLocalList())
        validateRemote(localApi);

      if (getEJBClass() == null) {
        throw error(L.l("'{0}' does not have a defined ejb-class.  Session beans must have an ejb-class.",
                        getEJBName()));
      }

      if (! getEJBClassWrapper().isAssignableTo(SessionSynchronization.class)) {
      }
      else if (isStateless()) {
        throw error(L.l("'{0}' must not implement SessionSynchronization.  Stateless session beans must not implement SessionSynchronization.",
                        getEJBClass().getName()));
      }
      else if (! _isContainerTransaction) {
        throw error(L.l("'{0}' must not implement SessionSynchronization.  Session beans with Bean-managed transactions may not use SessionSynchronization.",
                        getEJBClass().getName()));
      }
    } catch (LineConfigException e) {
      throw e;
    } catch (ConfigException e) {
      throw new LineConfigException(getLocation() + e.getMessage(), e);
    }

    if (isStateless())
      J2EEManagedObject.register(new StatelessSessionBean(this));
    else
      J2EEManagedObject.register(new StatefulSessionBean(this));
  }

  /**
   * Obtain and apply initialization from annotations.
   */
  public void initIntrospect()
    throws ConfigException
  {
    super.initIntrospect();

    JClass type = getEJBClassWrapper();

    // XXX: ejb/0f78
    if (type == null)
      return;

    if (! type.isAnnotationPresent(Stateful.class)
        && ! type.isAnnotationPresent(Stateless.class))
      return;

    /* XXX: ejb/0f6d: bean with local and remote interfaces
    if (_localHome != null || _localList.size() != 0
        || _remoteHome != null || _remoteList.size() != 0)
      return;
    */

    JClass []ifs = type.getInterfaces();

    ArrayList<JClass> interfaceList = new ArrayList<JClass>();

    for (int i = 0; i < ifs.length; i++) {
      JAnnotation local = ifs[i].getAnnotation(javax.ejb.Local.class);

      if (local != null) {
        setLocalWrapper(ifs[i]);
        continue;
      }

      JAnnotation remote = ifs[i].getAnnotation(javax.ejb.Remote.class);

      if (remote != null || ifs[i].isAssignableTo(java.rmi.Remote.class)) {
        setRemoteWrapper(ifs[i]);
        continue;
      }

      if (ifs[i].getName().equals("java.io.Serializable"))
        continue;

      if (ifs[i].getName().equals("java.io.Externalizable"))
        continue;

      if (ifs[i].getName().startsWith("javax.ejb"))
        continue;

      if (ifs[i].getName().equals("java.rmi.Remote"))
        continue;

      if (! interfaceList.contains(ifs[i]))
        interfaceList.add(ifs[i]);
    }

    // if (getLocalList().size() != 0 || getRemoteList().size() != 0) {
    if (_localHome != null || _localList.size() != 0
        || _remoteHome != null || _remoteList.size() != 0) {
    }
    else if (interfaceList.size() == 0)
      throw new ConfigException(L.l("'{0}' has no interfaces.  Can't currently generate.",
                                    type.getName()));
    else if (interfaceList.size() != 1)
      throw new ConfigException(L.l("'{0}' has multiple interfaces, but none are marked as @Local or @Remote.\n{1}",
                                    type.getName(),
                                    interfaceList.toString()));
    else {
      setLocalWrapper(interfaceList.get(0));
    }

    // XXX: Check ejb30/bb/session/stateless/migration/twothree/annotated
    // There is a conflict between 2.1 and 3.0 interfaces.

    // ejb/0f6f
    // The session bean might have @RemoteHome for EJB 2.1 and
    // the @Remote interface for EJB 3.0 (same with @LocalHome and @Local).
    // TCK: ejb30/bb/session/stateful/sessioncontext/annotated

    JClass ejbClass = getEJBClassWrapper();

    JAnnotation localHomeAnn = ejbClass.getAnnotation(LocalHome.class);

    if (localHomeAnn != null) {
      setLocalHome((Class) localHomeAnn.get("value"));
    }

    JAnnotation remoteHomeAnn = ejbClass.getAnnotation(RemoteHome.class);

    // ejb/0f6f
    if (remoteHomeAnn != null) {
      setHome((Class) remoteHomeAnn.get("value"));
    }
  }

  /**
   * Creates the assembler for the bean.
   */
  protected BeanAssembler createAssembler(String fullClassName)
  {
    if (isStateless())
      return new StatelessAssembler(this, fullClassName);
    else
      return new SessionAssembler(this, fullClassName);
  }

  /**
   * Adds the assemblers.
   */
  protected void addImports(BeanAssembler assembler)
  {
    super.addImports(assembler);

    if (isStateless()) {
      assembler.addImport("com.caucho.ejb.session.StatelessServer");
      assembler.addImport("com.caucho.ejb.session.AbstractStatelessContext");
      assembler.addImport("com.caucho.ejb.session.StatelessHome");
      assembler.addImport("com.caucho.ejb.session.StatelessObject");
    }
    else {
      assembler.addImport("com.caucho.ejb.session.SessionServer");
      assembler.addImport("com.caucho.ejb.session.AbstractSessionContext");
      assembler.addImport("com.caucho.ejb.session.SessionHome");
      assembler.addImport("com.caucho.ejb.session.SessionObject");
    }
  }

  /**
   * Creates the views.
   */
  protected EjbHomeView createHomeView(JClass homeClass, String prefix)
    throws ConfigException
  {
    if (isStateless())
      return new EjbStatelessHomeView(this, homeClass, prefix);
    else
      return new EjbSessionHomeView(this, homeClass, prefix);
  }

  /**
   * Deploys the bean.
   */
  public AbstractServer deployServer(EjbServerManager ejbManager,
                                     JavaClassGenerator javaGen)
    throws ClassNotFoundException, ConfigException
  {
    AbstractServer server;

    if (isStateless())
      server = new StatelessServer(ejbManager);
    else
      server = new SessionServer(ejbManager);

    server.setModuleName(getEJBModuleName());
    server.setEJBName(getEJBName());
    server.setMappedName(getMappedName());

    JClass remoteHome = getRemoteHome();
    if (remoteHome != null)
      server.setRemoteHomeClass(remoteHome.getJavaClass());

    ArrayList<JClass> remoteList = getRemoteList();
    if (remoteList.size() > 0)
      server.setRemoteObjectList(remoteList);

    JClass localHome = getLocalHome();
    if (localHome != null)
      server.setLocalHomeClass(localHome.getJavaClass());

    ArrayList<JClass> localList = getLocalList();
    if (localList.size() > 0)
      server.setLocalApiList(localList);

    Class contextImplClass = javaGen.loadClass(getSkeletonName());

    server.setContextImplClass(contextImplClass);

    Class beanClass = javaGen.loadClass(getEJBClass().getName());

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(server.getClassLoader());

      ArrayList<BuilderProgram> initList;
      initList = InjectIntrospector.introspect(beanClass, ejbManager.getLocalJndiPrefix());

      BuilderProgramContainer initContainer = getInitProgram();

      if (initList != null && initList.size() > 0) {
        if (initContainer == null)
          initContainer = new BuilderProgramContainer();

        for (BuilderProgram init : initList) {
          String foreignName = null;

          if (init instanceof JndiBindProgram) {
            // ejb/0f6c
            JndiBindProgram jndiBind = (JndiBindProgram) init;

            foreignName = jndiBind.getForeignName();
          }
          else if (init instanceof EjbInjectProgram) {
            // ejb/0g1c
            EjbInjectProgram ejbInject = (EjbInjectProgram) init;

            foreignName = ejbInject.getBeanName();
          }

          if (foreignName != null) {
            int index = foreignName.lastIndexOf("/");

            foreignName = foreignName.substring(++index);

            addBeanDependency(foreignName);
          }

          initContainer.addProgram(init);
        }
      }

      server.setInitProgram(initContainer);

      try {
        if (getServerProgram() != null)
          getServerProgram().configure(server);

        // ejb/0fd0, ejb/0g03
        for (EnvEntry envEntry : getEnvEntries())
          envEntry.init();
      } catch (ConfigException e) {
        throw e;
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new ConfigException(e);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return server;
  }

  private void introspectSession()
    throws ConfigException
  {
    JClass ejbClass = getEJBClassWrapper();

    if (ejbClass.isAnnotationPresent(Stateless.class))
      introspectStateless(ejbClass);
    else if (ejbClass.isAnnotationPresent(Stateful.class))
      introspectStateful(ejbClass);
  }

  private void introspectStateless(JClass type)
    throws ConfigException
  {
    String className = type.getName();

    JAnnotation stateless = type.getAnnotation(Stateless.class);

    setAllowPOJO(true);

    setSessionType("Stateless");

    setTransactionType(type);

    String name;
    if (stateless != null)
      name = stateless.getString("name");
    else
      name = className;

    introspectBean(type, name);
  }

  private void introspectStateful(JClass type)
    throws ConfigException
  {
    String className = type.getName();

    JAnnotation stateful = type.getAnnotation(Stateful.class);

    setAllowPOJO(true);

    setSessionType("Stateful");

    setTransactionType(type);

    String name;
    if (stateful != null)
      name = stateful.getString("name");
    else
      name = className;

    introspectBean(type, name);
  }

  private void setTransactionType(JClass type)
  {
    JAnnotation transaction = type.getAnnotation(TransactionManagement.class);
    if (transaction == null)
      setTransactionType("Container");
    else if (TransactionManagementType.BEAN.equals(transaction.get("value")))
      setTransactionType("Bean");
    else {
      setTransactionType("Container");
    }
  }

  private void validateMethods()
    throws ConfigException
  {
  }

  /**
   * Validates the home interface.
   */
  private void validateHome(JClass homeClass, JClass objectClass)
    throws ConfigException
  {
    JClass beanClass = getEJBClassWrapper();
    String beanName = beanClass.getName();

    if (homeClass == null)
      return;
    String homeName = homeClass.getName();
    String objectName = objectClass.getName();

    boolean hasFindByPrimaryKey = false;

    if (! homeClass.isPublic())
      throw error(L.l("'{0}' must be public", homeName));

    if (beanClass.isFinal())
      throw error(L.l("'{0}' must not be final", beanName));

    if (beanClass.isAbstract())
      throw error(L.l("'{0}' must not be abstract", beanName));

    if (! homeClass.isInterface())
      throw error(L.l("'{0}' must be an interface", homeName));

    boolean hasCreate = false;

    JMethod []methods = getMethods(homeClass);
    for (int i = 0; i < methods.length; i++) {
      JMethod method = methods[i];
      String name = method.getName();
      JClass []param = method.getParameterTypes();
      JClass retType = method.getReturnType();

      if (method.getDeclaringClass().isAssignableFrom(EJBHome.class) ||
          method.getDeclaringClass().isAssignableFrom(EJBLocalHome.class))
        continue;

      if (homeClass.isAssignableTo(EJBHome.class))
        validateException(method, java.rmi.RemoteException.class);

      if (name.startsWith("create")) {
        hasCreate = true;

        if (isStateless() && (! name.equals("create") ||
                              method.getParameterTypes().length != 0)) {
          throw error(L.l("{0}: '{1}' forbidden in stateless session home.  The create() method for a stateless session bean must have zero arguments.",
                          method.getFullName(),
                          homeName));
        }

        if (! isAllowPOJO())
          validateException(method, CreateException.class);

        /* XXX: ejb/0f6f

           The session bean might have @RemoteHome for EJB 2.1 and the @Remote interface for EJB 3.0
           TCK: ejb30/bb/session/stateful/sessioncontext/annotated

        if (! retType.equals(objectClass))
          throw error(L.l("{0}: '{1}' must return {2}.  Create methods must return the local or remote interface.",
                          homeName,
                          method.getFullName(),
                          objectClass.getName()));
        */

        String createName = "ejbC" + name.substring(1);
        JMethod implMethod =
          validateNonFinalMethod(createName, param,
                                 method, homeClass, isAllowPOJO());

        if (implMethod != null) {
          if (! implMethod.getReturnType().getName().equals("void"))
            throw error(L.l("'{0}' must return {1} in {2}",
                            getFullMethodName(createName, param),
                            "void",
                            beanName));

          validateExceptions(method, implMethod.getExceptionTypes());
        }
      }
      else if (name.startsWith("ejb") || name.startsWith("remove")) {
        throw error(L.l("'{0}' forbidden in {1}",
                        method.getFullName(),
                        homeClass.getName()));
      }
    }

    if (! hasCreate)
      throw error(L.l("'{0}' needs at least one create method.  Session beans need a create method.",
                      homeClass.getName()));
  }
}
