/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.config.el;

import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.OwnerCreationalContext;
import com.caucho.config.xml.XmlConfigContext;

/**
 * Variable resolution for webbeans variables
 */
public class CandiContextResolver extends ELResolver {
  private static final ThreadLocal<ContextHolder> _envLocal
    = new ThreadLocal<ContextHolder>();
  
  private InjectManager _injectManager;
  
  public CandiContextResolver(InjectManager injectManager)
  {
    _injectManager = injectManager;
  }
  
  public CandiContextResolver()
  {
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context,
                                        Object base)
  {
    return Object.class;
  }

  @Override
  public Iterator<FeatureDescriptor>
    getFeatureDescriptors(ELContext context, Object base)
  {
    ArrayList<FeatureDescriptor> list = new ArrayList<FeatureDescriptor>();

    return list.iterator();
  }

  @Override
  public Class<?> getType(ELContext context,
                          Object base,
                          Object property)
  {
    if (! (property instanceof String) || base != null)
      return null;

    String name = (String) property;
    
    InjectManager manager = getInjectManager();
    
    Set<Bean<?>> beanSet = manager.getBeans(name);
    
    if (beanSet.size() == 0)
      return null;
    
    Bean<?> bean = beanSet.iterator().next();
    
    return bean.getBeanClass();
  }

  @Override
  public Object getValue(ELContext context,
                         Object base,
                         Object property)
    throws PropertyNotFoundException,
           ELException
  {
    if (! (property instanceof String) || base != null)
      return null;

    String name = (String) property;
    
    InjectManager manager = getInjectManager();
    
    if (manager == null)
      return manager;
    
    Set<Bean<?>> beanSet = manager.getBeans(name);
    
    if (beanSet.size() == 0)
      return null;
    
    Bean<?> bean = manager.resolve(beanSet);

    XmlConfigContext env = XmlConfigContext.getCurrent();

    ContextHolder holder = _envLocal.get();
    
    CreationalContextImpl<?> cxt = null;
    
    if (holder != null && holder.isActive()) {
      cxt = holder.getEnv();
      
      if (cxt == null) {
        cxt = new OwnerCreationalContext<Object>(null);
        holder.setEnv(cxt);
      }
    }
    
    if (cxt == null && env != null) {
      cxt = (CreationalContextImpl<?>) env.getCreationalContext();
    }
    
    if (cxt == null) {
      cxt = new OwnerCreationalContext(bean);
    }
    
    Object result = CreationalContextImpl.findAny(cxt, bean);
    
    if (result == null)
      result = manager.getReference(bean, cxt);

    if (result != null) {
      context.setPropertyResolved(true);

      return result;
    }
    else
      return null;
  }
  
  private InjectManager getInjectManager()
  {
    InjectManager manager = _injectManager;

    if (manager == null) {
      manager = InjectManager.getCurrent();
      
      if (manager == null)
        return null;
    }
    
    return manager;
  }

  @Override
  public boolean isReadOnly(ELContext context,
                            Object base,
                            Object property)
    throws PropertyNotFoundException,
           ELException
  {
    return true;
  }

  @Override
  public void setValue(ELContext context,
                       Object base,
                       Object property,
                       Object value)
    throws PropertyNotFoundException,
           PropertyNotWritableException,
           ELException
  {
  }
  
  public static final void startContext()
  {
    ContextHolder holder = _envLocal.get();
    
    if (holder == null) {
      holder = new ContextHolder();
      _envLocal.set(holder);
    }
    
    holder.setActive();
  }
  
  public static final void finishContext()
  {
    ContextHolder holder = _envLocal.get();
    
    holder.free();
  }
  
  static class ContextHolder {
    private boolean _isActive;
    private CreationalContextImpl<?> _env;

    void setActive()
    {
      _isActive = true;
    }
    
    boolean isActive()
    {
      return _isActive;
      
    }
    
    CreationalContextImpl<?> getEnv()
    {
      return _env;
    }

    void setEnv(CreationalContextImpl<?> env)
    {
      _env = env;
    }
    
    void free()
    {
      CreationalContextImpl<?> env = _env;
      _env = null;
      _isActive = false;
      
      env.release();
    }
  }
}
