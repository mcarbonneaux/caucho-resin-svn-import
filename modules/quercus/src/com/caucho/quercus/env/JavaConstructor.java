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

package com.caucho.quercus.env;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.util.L10N;

import java.lang.reflect.*;

/**
 * Represents the introspected static function information.
 */
public class JavaConstructor extends JavaInvoker {
  private static final L10N L = new L10N(JavaConstructor.class);
  
  private final Constructor _constructor;

  /**
   * Creates the statically introspected function.
   *
   * @param method the introspected method.
   */
  public JavaConstructor(ModuleContext moduleContext,
			 Constructor cons)
  {    
    super(moduleContext,
	  cons.getDeclaringClass().getName(),
	  cons.getParameterTypes(),
	  cons.getParameterAnnotations(),
	  cons.getAnnotations(),
	  cons.getDeclaringClass());

    _constructor = cons;
  }

  public Object invoke(Object obj, Object []args)
  {
    try {
      obj = _constructor.newInstance(args);
      
      return obj;
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException)
	throw (RuntimeException) e.getCause();
      else
	throw new QuercusException(e.getCause());
    } catch (Exception e) {
      throw new QuercusException(e);
    }
  }
}
