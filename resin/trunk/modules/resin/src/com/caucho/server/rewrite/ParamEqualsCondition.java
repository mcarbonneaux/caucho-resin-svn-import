/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.server.rewrite;

import javax.servlet.http.HttpServletRequest;
import javax.annotation.PostConstruct;

/**
* A rewrite condition that passes if the value of a named parameter is exactly
* equal to a specified value.
*/
public class ParamEqualsCondition
  extends AbstractEqualsCondition
{
  private String _name;

  public String getTagName()
  {
    return "param-equals";
  }

  /**
   * Sets the name of the param, required.
   */
  public void setName(String name)
  {
    _name = name;
  }

  @PostConstruct
  public void init()
  {
    super.init();

    required(_name, "name");
  }

  protected String getValue(HttpServletRequest request)
  {
    return request.getParameter(_name);
  }
}
