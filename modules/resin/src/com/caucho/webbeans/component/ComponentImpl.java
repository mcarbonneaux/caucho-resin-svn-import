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

package com.caucho.webbeans.component;

import com.caucho.config.program.FieldComponentProgram;
import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.types.*;
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.bytecode.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.manager.WebBeansContainer;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

import javax.annotation.*;
import javax.webbeans.*;
import javax.webbeans.manager.Bean;

/**
 * Configuration for the xml web bean component.
 */
public class ComponentImpl<T> extends Bean<T>
  implements ObjectProxy
{
  private static final L10N L = new L10N(ComponentImpl.class);

  private static final Object []NULL_ARGS = new Object[0];
  private static final ConfigProgram []NULL_INJECT = new ConfigProgram[0];

  protected WbWebBeans _webbeans;
  
  private Class<? extends Annotation> _deploymentType;

  private Type _targetType;

  private boolean _isFromClass;

  private String _name;
  
  private boolean _hasBinding;

  private LinkedHashSet<Class<?>> _types
    = new LinkedHashSet<Class<?>>();
  
  private ArrayList<WbBinding> _bindingList
    = new ArrayList<WbBinding>();

  private WebBeansHandle _handle;

  private Class<? extends Annotation> _scopeType;
  protected ScopeContext _scope;
  private String _scopeId;

  protected ConfigProgram []_injectProgram = NULL_INJECT;
  protected ConfigProgram []_initProgram = NULL_INJECT;
  protected ConfigProgram []_destroyProgram = NULL_INJECT;
  
  private ContainerProgram _init;

  public ComponentImpl(WbWebBeans webbeans)
  {
    super(WebBeansContainer.create());
    
    _webbeans = webbeans;
  }

  public WbWebBeans getWebBeans()
  {
    return _webbeans;
  }

  /**
   * Returns the component's EL binding name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the component's EL binding name.
   */
  public String getName()
  {
    return _name;
  }
  
  public void setTargetType(Type type)
  {
    _targetType = type;
  }
  
  public Type getTargetType()
  {
    return _targetType;
  }
  
  public String getTargetSimpleName()
  {
    if (_targetType instanceof Class)
      return ((Class) _targetType).getSimpleName();
    else
      return String.valueOf(_targetType);
  }
  
  public Class getTargetClass()
  {
    if (_targetType instanceof Class)
      return ((Class) _targetType);
    else if (_targetType instanceof ParameterizedType)
      return (Class) ((ParameterizedType) _targetType).getRawType();
    else
      return (Class) _targetType;
  }

  public String getClassName()
  {
    if (_targetType instanceof Class)
      return ((Class) _targetType).getName();
    else
      return String.valueOf(_targetType);
  }

  public void addBinding(Annotation binding)
  {
    _bindingList.add(new WbBinding(binding));
  }
  
  /**
   * Adds a component binding.
   */
  public void setBindingList(ArrayList<WbBinding> bindingList)
  {
    _bindingList = bindingList;
  }
  
  public ArrayList<WbBinding> getBindingList()
  {
    return _bindingList;
  }

  /**
   * Sets the scope annotation.
   */
  public void setScope(ScopeContext scope)
  {
    _scope = scope;
    if (_scope != null)
      _scopeType = _scope.getScopeType();
  }

  /**
   * Sets the scope annotation.
   */
  public void setScopeType(Class<? extends Annotation> scopeType)
  {
    _scopeType = scopeType;
  }

  /**
   * Gets the scope annotation.
   */
  public ScopeContext getScope()
  {
    return _scope;
  }

  public boolean isSingleton()
  {
    return (_scope instanceof SingletonScope
	    || _scope instanceof ApplicationScope);
  }

  /**
   * Sets the init program.
   */
  public void setInit(ContainerProgram init)
  {
    _init = init;
  }

  /**
   * Add to the init program.
   */
  public void addProgram(ConfigProgram program)
  {
    if (_init == null)
      _init = new ContainerProgram();
    
    _init.addProgram(program);
  }

  /**
   * True if the component was defined by class introspection.
   */
  public void setFromClass(boolean isFromClass)
  {
    _isFromClass = isFromClass;
  }

  /**
   * True if the component was defined by class introspection.
   */
  public boolean isFromClass()
  {
    return _isFromClass;
  }

  /**
   * Returns the serialization handle
   */
  public WebBeansHandle getHandle()
  {
    return _handle;
  }

  /**
   * Initialization.
   */
  public void init()
  {
    introspectTypes(getTargetClass());
    
    introspect();

    if (_deploymentType == null)
      _deploymentType = Production.class;

    if (_bindingList.size() == 0)
      _bindingList.add(new WbBinding(new AnnotationLiteral<Current>() {}));

    if (_scopeType == null)
      _scopeType = Dependent.class;

    generateScopeId();

    _handle = new WebBeansHandle(_targetType, _bindingList);
  }

  /**
   * Introspects all the types implemented by the class
   */
  private void introspectTypes(Class cl)
  {
    if (cl == null || cl.equals(Object.class))
      return;

    if (_types.contains(cl))
      return;

    _types.add(cl);

    introspectTypes(cl.getSuperclass());

    for (Class iface : cl.getInterfaces()) {
      introspectTypes(iface);
    }
  }

  protected void introspect()
  {
  }

  private void generateScopeId()
  {
    long crc64 = 17;

    crc64 = Crc64.generate(crc64, String.valueOf(_targetType));

    if (_name != null)
      crc64 = Crc64.generate(crc64, _name);

    for (WbBinding binding : _bindingList) {
      crc64 = binding.generateCrc64(crc64);
    }

    StringBuilder sb = new StringBuilder();
    Base64.encode(sb, crc64);

    _scopeId = sb.toString();
  }

  /**
   * Called for implicit introspection.
   */
  protected void introspectScope(Class type)
  {
    Class scopeClass = null;

    if (getScope() == null) {
      for (Annotation ann : type.getDeclaredAnnotations()) {
	if (ann.annotationType().isAnnotationPresent(ScopeType.class)) {
	  if (scopeClass != null)
	    throw new ConfigException(L.l("{0}: @ScopeType annotation @{1} conflicts with @{2}.  WebBeans components may only have a single @ScopeType.",
					  type.getName(),
					  scopeClass.getName(),
					  ann.annotationType().getName()));

	  scopeClass = ann.annotationType();
	  setScope(_webbeans.getScopeContext(scopeClass));
	}
      }
    }
  }

  /**
   * Introspects the methods for any @Produces
   */
  protected void introspectBindings()
  {
  }

  public boolean isMatch(ArrayList<Annotation> bindList)
  {
    for (int i = 0; i < bindList.size(); i++) {
      if (! isMatch(bindList.get(i)))
	return false;
    }
    
    return true;
  }

  public boolean isMatch(Annotation []bindings)
  {
    for (Annotation binding : bindings) {
      if (! isMatch(binding))
	return false;
    }
    
    return true;
  }

  /**
   * Returns true if at least one of this component's bindings match
   * the injection binding.
   */
  public boolean isMatch(Annotation bindAnn)
  {
    for (int i = 0; i < _bindingList.size(); i++) {
      if (_bindingList.get(i).isMatch(bindAnn))
	return true;
    }
    
    return false;
  }

  public boolean isMatchByBinding(ArrayList<Binding> bindList)
  {
    for (int i = 0; i < bindList.size(); i++) {
      if (! isMatchByBinding(bindList.get(i)))
	return false;
    }
    
    return true;
  }

  /**
   * Returns true if at least one of this component's bindings match
   * the injection binding.
   */
  public boolean isMatchByBinding(Binding binding)
  {
    for (int i = 0; i < _bindingList.size(); i++) {
      if (_bindingList.get(i).isMatch(binding))
	return true;
    }
    
    return false;
  }

  /**
   * Returns the component object if it already exists
   */
  public Object getIfExists()
  {
    if (_scope != null)
      return _scope.get(this, false);
    else
      return get();
  }

  /**
   * Returns the component object, creating if necessary
   */
  public Object get()
  {
    if (_scope != null) {
      Object value = _scope.get(this, false);

      if (value != null) {
	return value;
      }
    }
    
    return create();
  }

  public T get(ConfigContext env)
  {
    if (_scope != null) {
      T value = _scope.get(this, false);

      if (value != null)
	return value;
    }
    else {
      T value = (T) env.get(this);

      if (value != null)
	return value;
    }

    T value;

    if (_scope != null) {
      value = createNew(null);
      _scope.put(this, value);
      
      env = new ConfigContext(this, value, _scope);
    }
    else {
      value = createNew(env);
    }

    init(value, env);

    return value;
  }

  /**
   * Creates a new instance of the component.
   */
  public T create()
  {
    try {
      ConfigContext env = new ConfigContext(_scope);
      
      T value = createNew(env);

      env.put(this, value);

      if (_scope != null)
	_scope.put(this, value);

      init(value, env);

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a new instance of the component.
   */
  public T createNoInit()
  {
    try {
      T value = createNew(null);

      if (_injectProgram.length > 0) {
        ConfigContext env = new ConfigContext(this, value, null);

	for (ConfigProgram program : _injectProgram) {
	  program.inject(value, env);
	}
      }

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a new instance of the component
   * 
   * @param env the configuration environment
   * @return the new object
   */
  protected T createNew(ConfigContext env)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Initialize the created value
   */
  protected T init(T value, ConfigContext env)
  {
    if (_init != null)
      _init.inject(value, env);

    for (ConfigProgram inject : _injectProgram) {
      inject.inject(value, env);
    }

    for (ConfigProgram inject : _initProgram) {
      inject.inject(value, env);
    }

    if (_destroyProgram.length > 0) {
      env.addDestructor(this, value);
    }

    return value;
  }

  /**
   * Returns true if there's a destroy program.
   */
  public boolean isDestroyPresent()
  {
    return _destroyProgram.length > 0;
  }

  /**
   * Destroys the value
   */
  public void destroy(Object value, ConfigContext env)
  {
    for (ConfigProgram inject : _destroyProgram)
      inject.inject(value, env);
  }

  /**
   * Destroys the value
   */
  public void destroy(T value)
  {
    destroy(value, null);
  }

  /**
   * Binds parameters
   */
  public void bind()
  {
  }

  public void createProgram(ArrayList<ConfigProgram> initList, Field field)
    throws ConfigException
  {
    initList.add(new FieldComponentProgram(this, field));
  }

  public String getScopeId()
  {
    return _scopeId;
  }

  //
  // metadata for the bean
  //

  /**
   * Returns the bean's binding types
   */
  public Set<Annotation> getBindingTypes()
  {
    Set<Annotation> set = new LinkedHashSet<Annotation>();

    for (WbBinding binding : _bindingList) {
      set.add(binding.getAnnotation());
    }

    return set;
  }

  /**
   * Sets the component type.
   */
  public void setDeploymentType(Class<? extends Annotation> type)
  {
    if (type == null)
      throw new NullPointerException();

    if (_deploymentType != null && ! _deploymentType.equals(type))
      throw new ConfigException(L.l("deployment-type must be unique"));
    
    _deploymentType = type;
  }

  /**
   * Returns the bean's deployment type
   */
  public Class<? extends Annotation> getDeploymentType()
  {
    return _deploymentType;
  }

  /**
   * Returns the bean's name or null if the bean does not have a
   * primary name.
   */
      /*
  public String getName()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
      */

  /**
   * Returns true if the bean can be null
   */
  public boolean isNullable()
  {
    return false;
  }

  /**
   * Returns true if the bean is serializable
   */
  public boolean isSerializable()
  {
    return false;
  }

  /**
   * Returns the bean's scope
   */
  public Class<Annotation> getScopeType()
  {
    return (Class<Annotation>) _scopeType;
  }

  /**
   * Returns the types that the bean implements
   */
  public Set<Class<?>> getTypes()
  {
    return _types;
  }

  //
  // lifecycle
  //

  /**
   * Create a new instance of the bean.
   */
  /*
  public T create()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  /**
   * Destroys a bean instance
   */
  /*
  public void destroy(T instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  //
  // ObjectProxy
  //

  /**
   * Returns the new object for JNDI
   */
  public Object createObject(Hashtable env)
  {
    return get();
  }

  protected ConfigException error(Method method, String msg)
  {
    return new ConfigException(LineConfigException.loc(method) + msg);
  }

  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    else if (! (obj instanceof ComponentImpl))
      return false;

    ComponentImpl comp = (ComponentImpl) obj;

    if (! _targetType.equals(comp._targetType)) {
      return false;
    }

    int size = _bindingList.size();

    if (size != comp._bindingList.size()) {
      return false;
    }

    for (int i = size - 1; i >= 0; i--) {
      if (! comp._bindingList.contains(_bindingList.get(i))) {
	return false;
      }
    }

    return true;
  }

  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    if (_targetType instanceof Class)
      sb.append(((Class) _targetType).getSimpleName());
    else
      sb.append(String.valueOf(_targetType));
    sb.append("[");
    
    if (_name != null) {
      sb.append("name=");
      sb.append(_name);
    }

    for (WbBinding binding : _bindingList) {
      sb.append(",");
      sb.append(binding.toDebugString());
    }

    if (_deploymentType != null) {
      sb.append(", @");
      sb.append(_deploymentType.getSimpleName());
    }
    
    if (_scopeType != null) {
      sb.append(", @");
      sb.append(_scopeType.getSimpleName());
    }

    sb.append("]");

    return sb.toString();
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    if (_targetType instanceof Class)
      sb.append(((Class) _targetType).getSimpleName());
    else
      sb.append(String.valueOf(_targetType));
    sb.append(", ");

    if (_deploymentType != null) {
      sb.append("@");
      sb.append(_deploymentType.getSimpleName());
    }
    else
      sb.append("@null");
    
    if (_name != null) {
      sb.append(", ");
      sb.append("name=");
      sb.append(_name);
    }
    
    if (_scope != null) {
      sb.append(", @");
      sb.append(_scope.getClass().getSimpleName());
    }

    sb.append("]");

    return sb.toString();
  }

  protected static String getSimpleName(Type type)
  {
    if (type instanceof Class)
      return ((Class) type).getSimpleName();
    else
      return String.valueOf(type);
  }
}
