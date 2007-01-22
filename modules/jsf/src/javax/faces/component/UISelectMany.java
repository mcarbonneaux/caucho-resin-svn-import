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

package javax.faces.component;

import java.util.*;

import javax.el.*;
import javax.faces.application.*;
import javax.faces.context.*;

public class UISelectMany extends UIInput
{
  public static final String COMPONENT_FAMILY = "javax.faces.SelectMany";
  public static final String COMPONENT_TYPE = "javax.faces.SelectMany";
  public static final String INVALID_MESSAGE_ID
    = "javax.faces.component.UISelectMany.INVALID";

  public UISelectMany()
  {
    setRendererType("javax.faces.Listbox");
  }

  /**
   * Returns the component family, used to select the renderer.
   */
  public String getFamily()
  {
    return COMPONENT_FAMILY;
  }

  //
  // properties
  //

  public Object []getSelectedValues()
  {
    return (Object []) super.getValue();
  }

  public void setSelectedValues(Object []value)
  {
    super.setValue(value);
  }

  /**
   * Returns the value expression with the given name.
   */
  @Override
  public ValueExpression getValueExpression(String name)
  {
    if ("selectedValues".equals(name))
      return super.getValueExpression("value");
    else
      return super.getValueExpression(name);
  }

  /**
   * Sets the value expression with the given name.
   */
  @Override
  public void setValueExpression(String name, ValueExpression expr)
  {
    if ("selectedValues".equals(name)) {
      super.setValueExpression("value", expr);
    }
    else
      super.setValueExpression(name, expr);
  }

  //
  // validate
  //

  public void validateValue(FacesContext context, Object value)
  {
    super.validateValue(context, value);
    
    if (! isValid())
      return;

    boolean hasValue = false;
    
    if (value instanceof Object[]) {
      Object []values = (Object []) value;

      for (int i = 0; i < values.length; i++) {
	hasValue = false;
	
	for (UIComponent child : getChildren()) {
	  if (child instanceof UISelectItem) {
	    UISelectItem item = (UISelectItem) child;

	    if (values[i].equals(item.getItemValue())) {
	      hasValue = true;
	      break;
	    }
	  }
	}

	if (! hasValue)
	  break;
      }
    }

    if (! hasValue) {
      setValid(false);
      
      FacesMessage msg = new FacesMessage(INVALID_MESSAGE_ID);
      
      context.addMessage(getClientId(context), msg);
    }
  }

  @Override
  protected boolean compareValues(Object oldValue, Object newValue)
  {
    if (oldValue == newValue)
      return false;
    else if (oldValue == null || newValue == null)
      return true;

    Object []oldValues = (Object []) oldValue;
    Object []newValues = (Object []) newValue;

    if (oldValues.length != newValues.length)
      return true;

    for (int i = 0; i < oldValues.length; i++) {
      if (! oldValues[i].equals(newValues[i]))
	return true;
    }

    return false;
  }
}
