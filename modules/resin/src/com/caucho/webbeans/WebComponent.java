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

package com.caucho.webbeans;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.util.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.inject.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.ArrayList;

/**
 * Configuration for the xml web bean component.
 */
public class WebComponent {
  private static L10N L = new L10N(WebComponent.class);
  
  private Class _type;

  private ArrayList<WbComponent> _componentList
    = new ArrayList<WbComponent>();

  public WebComponent(Class type)
  {
    _type = type;
  }

  public void addComponent(WbComponent comp)
  {
    if (! _componentList.contains(comp))
      _componentList.add(comp);
  }
  
  public void createProgram(ArrayList<BuilderProgram> initList,
			    AccessibleObject field,
			    String name,
			    AccessibleInject inject,
			    ArrayList<Annotation> bindList)
    throws ConfigException
  {
    WbComponent matchComp = null;

    System.out.println("COMPP: " + _componentList);
    for (int i = 0; i < _componentList.size(); i++) {
      WbComponent comp = _componentList.get(i);

      if (! comp.isMatch(bindList))
	continue;

      if (matchComp == null)
	matchComp = comp;
      else if (matchComp.getType().getPriority() < comp.getType().getPriority())
	matchComp = comp;
      else if (comp.getType().getPriority() < matchComp.getType().getPriority()) {
      }
      else if (matchComp != null) {
	throw WebBeans.injectError(field, L.l("WebBeans conflict between '{0}' and '{1}'.  WebBean injection must match uniquely.",
					      matchComp.getClassName(),
					      comp.getClassName()));
      }
    }

    if (matchComp == null)
      throw WebBeans.injectError(field, L.l("WebBeans unable to find matching component."));

    matchComp.createProgram(initList, field, name, inject);
  }
}
