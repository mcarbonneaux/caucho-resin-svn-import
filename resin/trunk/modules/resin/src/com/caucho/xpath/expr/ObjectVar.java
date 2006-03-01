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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.xpath.expr;

import java.util.*;
import java.io.*;
import org.w3c.dom.*;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.*;
import com.caucho.xpath.*;
import com.caucho.xpath.pattern.*;

/**
 * A variable containing a Java object.
 */
public class ObjectVar extends Var {
  private Object object;

  /**
   * Creates a new object variable with the object.
   */
  public ObjectVar(Object object)
  {
    this.object = object;
  }
  
  /**
   * Returns the value as a double.
   */
  double getDouble()
    throws XPathException
  {
    double v = Expr.toDouble(getObject());

    return v;
  }
  
  /**
   * Returns the value as an object.
   */
  Object getObject()
  {
    if (object instanceof NodeIterator)
      return ((NodeIterator) object).clone();
    else
      return object;
  }

  public String toString()
  {
    return "[ObjectVar " + object + "]";
  }
}

