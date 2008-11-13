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

package com.caucho.config.attribute;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

import com.caucho.config.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.type.*;
import com.caucho.config.types.AnnotationConfig;
import com.caucho.config.types.CustomBeanConfig;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

public class CustomBeanAttribute extends Attribute {
  private static final L10N L = new L10N(CustomBeanAttribute.class);
  
  private final ConfigType _configType;
  private final Method _setMethod;

  public CustomBeanAttribute()
  {
    this(null, TypeFactory.getType(CustomBeanConfig.class));
  }
  
  public CustomBeanAttribute(Method setMethod, ConfigType configType)
  {
    _configType = configType;
    _setMethod = setMethod;
  }

  public ConfigType getConfigType()
  {
    return _configType;
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, QName qName)
    throws ConfigException
  {
    String uri = qName.getNamespaceURI();
    String localName = qName.getLocalName();

    if (! uri.startsWith("urn:java:"))
      throw new IllegalStateException(L.l("'{0}' is an unexpected namespace, expected 'urn:java:...'", uri));

    String className = uri.substring("uri:java:".length()) + '.' + localName;
    Class cl = null;

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    try {
      cl = Class.forName(className, false, loader);
    } catch (ClassNotFoundException e) {
      throw new ConfigException(L.l("'{0}' is an unknown class for element '{1}'",
				    className, qName), e);
    }

    if (Annotation.class.isAssignableFrom(cl)) {
      return new AnnotationConfig(cl);
    }
    else {
      CustomBeanConfig config = new CustomBeanConfig(qName, cl);

      // config.setScope("singleton");

      return config;
    }
  }
  
  /**
   * Sets the value of the attribute as text
   */
  public void setText(Object bean, QName name, String value)
    throws ConfigException
  {
    CustomBeanConfig config = (CustomBeanConfig) create(bean, name);

    if (! value.trim().equals("")) {
      config.addArg(new TextArgProgram(value));
    }

    config.init();

    setValue(bean, name, config);
  }
  
  
  /**
   * Sets the value of the attribute
   */
  public void setValue(Object bean, QName name, Object value)
    throws ConfigException
  {
    try {
      if (value instanceof AnnotationConfig)
	value = ((AnnotationConfig) value).replace();
      
      if (_setMethod != null && value != null) {
	if (! _setMethod.getParameterTypes()[0].isAssignableFrom(value.getClass()))
	  throw new ConfigException(L.l("'{0}.{1}' is not assignable from {2}",
					_setMethod.getDeclaringClass().getSimpleName(),
					_setMethod.getName(),
					value));
	
					
	_setMethod.invoke(bean, value);
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  static class TextArgProgram extends ConfigProgram {
    private String _arg;

    TextArgProgram(String arg)
    {
      _arg = arg;
    }
    
    public void inject(Object bean, ConfigContext env)
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    public Object configure(ConfigType type, ConfigContext env)
      throws ConfigException
    {
      return type.valueOf(_arg);
    }
  }
}
