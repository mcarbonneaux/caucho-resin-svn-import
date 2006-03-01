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

package com.caucho.server.security;

import java.io.*;
import java.util.*;
import java.security.*;

import javax.servlet.http.*;
import javax.servlet.*;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.server.http.*;

abstract public class AbstractConstraint {
  /**
   * Returns true if the constraint requires authentication.
   */
  public boolean needsAuthentication()
  {
    return false;
  }
  
  /**
   * Returns true if any cache needs to be private.
   */
  public boolean isPrivateCache()
  {
    return true;
  }
  
  /**
   * Returns true if the user is authorized for the resource.
   *
   * <p>isAuthorized must provide the response if the user is not
   * authorized.  Typically this will just call sendError.
   *
   * <p>isAuthorized will be called after all the other filters, but
   * before the servlet.service().
   *
   * @param request the servlet request
   * @param response the servlet response
   *
   * @return true if the request is authorized.
   */
  abstract public boolean isAuthorized(HttpServletRequest request,
                                       HttpServletResponse response,
                                       ServletContext application)
    throws ServletException, IOException;

  /**
   * converts the sub constraints to an array.
   */
  protected AbstractConstraint []toArray()
  {
    return new AbstractConstraint[] { this };
  }
}
