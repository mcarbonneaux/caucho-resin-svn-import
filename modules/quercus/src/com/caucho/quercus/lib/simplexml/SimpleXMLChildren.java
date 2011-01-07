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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.simplexml;

import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

/**
 * SimpleXMLElement object oriented API facade.
 * Also acts as the DOM document.
 */
public class SimpleXMLChildren extends SimpleXMLElement
{
  protected SimpleXMLChildren(Env env, QuercusClass cls,
                              SimpleXMLElement parent, String name)
  {
    super(env, cls, parent, name);
  }
  
  /**
   * Implementation for getting the indices of this class.
   * i.e. <code>$a->foo[0]</code>
   */
  @Override
  public Value __get(Env env, Value indexV)
  {
    if (indexV.isString()) {
      String name = indexV.toString();
      
      return wrapJava(env, _cls, getAttribute(name));
    }
    else if (indexV.isLongConvertible()) {
      int i = indexV.toInt();

      if (_children != null && i < _children.size())
        return wrapJava(env, _cls, _children.get(i));
      else
        return NullValue.NULL;
    }
    else
      return NullValue.NULL;
  }
  
  /**
   * Converts node tree to a valid xml string.
   * 
   * @return xml string
   */
  @ReturnNullAsFalse
  public StringValue asXML(Env env)
  {
    if (_children != null)
      return _children.get(0).toXML(env);
    else
      return null;
  }
}
