/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.el;

import javax.el.*;
import javax.faces.context.*;
import javax.faces.el.*;

public class MethodBindingAdapter extends MethodBinding
{
  private final MethodExpression _expr;

  public MethodBindingAdapter(MethodExpression expr)
  {
    _expr = expr;
  }

  public String getExpressionString()
  {
    return _expr.getExpressionString();
  }
  
  @Deprecated
  public Object invoke(FacesContext context, Object []param)
    throws EvaluationException, javax.faces.el.MethodNotFoundException
  {
    try {
      return _expr.invoke(context.getELContext(), param);
    } catch (javax.el.MethodNotFoundException e) {
      throw new javax.faces.el.MethodNotFoundException(e);
    } catch (Exception e) {
      throw new EvaluationException(e);
    }
  }

  @Deprecated
  public Class getType(FacesContext context)
    throws EvaluationException, javax.faces.el.PropertyNotFoundException
  {
    return Object.class;
  }

  public String toString()
  {
    return "MethodBindingAdapter[" + _expr.getExpressionString() + "]";
  }
}
