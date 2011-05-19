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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.types;

import com.caucho.el.EL;
import com.caucho.el.ELParser;
import com.caucho.el.Expr;
import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.el.ELException;

/**
 * Class-loading TypeBuilder
 */
public class StringTypeBuilder {
  static L10N L = new L10N(StringTypeBuilder.class);

  public static String evalString(String string)
    throws ELException
  {
    return evalString(string, EL.getEnvironment());
  }

  public static String evalString(String string, ELContext env)
    throws ELException
  {
    Expr expr = new ELParser(env, string).parse();

    return expr.evalString(env);
  }

  public void append(char ch)
  {
  }
}