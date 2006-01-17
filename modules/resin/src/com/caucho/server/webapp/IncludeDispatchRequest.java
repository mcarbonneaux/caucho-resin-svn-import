/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.server.webapp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.log.Log;

import com.caucho.server.connection.RequestAdapter;
import com.caucho.server.connection.CauchoRequest;
import com.caucho.server.connection.CauchoResponse;
import com.caucho.server.connection.ServletInputStreamImpl;
import com.caucho.server.connection.Form;

import com.caucho.server.session.SessionManager;
import com.caucho.server.session.SessionImpl;
/**
 * sub-request for a include() page
 */
class IncludeDispatchRequest extends DispatchRequest {
  protected static final Logger log = Log.open(IncludeDispatchRequest.class);

  private static final FreeList<IncludeDispatchRequest> _freeList =
    new FreeList<IncludeDispatchRequest>(32);
  
  private Hashtable<String,String> _headers;
  
  protected IncludeDispatchRequest()
  {
  }

  /**
   * Creates a dispatch request.
   */
  public static IncludeDispatchRequest createDispatch()
  {
    IncludeDispatchRequest req = _freeList.allocate();
    if (req == null)
      req = new IncludeDispatchRequest();

    return req;
  }

  void init(Application application,
            Application oldApplication,
            HttpServletRequest request,
            HttpServletResponse response,
	    String method, String uri,
	    String servletPath, String pathInfo,
	    String queryString, String addedQuery)
    throws ServletException
  {
    super.init(application, oldApplication, request, response,
	       method, uri, servletPath, pathInfo, queryString, addedQuery);
    
    _headers = null;
  }

  /**
   * Returns the include's view of the header names.
   */
  public Enumeration getHeaderNames()
  {
    return new HeaderEnumeration(super.getHeaderNames(), _headers);
  }
    
  public String getHeader(String key)
  {
    String value = null;
    if (_headers != null)
      value = (String) _headers.get(key);

    if (value != null)
      return value;

    // The included file must ignore caching directives from the
    // original request
    if (key.equalsIgnoreCase("If-Modified-Since") ||
        key.equalsIgnoreCase("If-None-Match"))
      return null;
    else {
      return super.getHeader(key);
    }
  }

  public void setHeader(String key, String value)
  {
    if (_headers == null)
      _headers = new Hashtable<String,String>();
    _headers.put(key, value);
  }

  /**
   * Frees the request.
   */
  public static void free(IncludeDispatchRequest req)
  {
    req.free();

    _freeList.free(req);
  }

  public static class HeaderEnumeration implements Enumeration {
    private Enumeration _parent;
    
    private Hashtable<String,String> _headers;
    private Iterator<String> _headerIter;

    private String _nextHeader;

    HeaderEnumeration(Enumeration parent, Hashtable<String,String> headers)
    {
      _parent = parent;
      _headers = headers;

      if (headers != null)
	_headerIter = headers.keySet().iterator();
    }
    
    /**
     * Returns true if there are more elements.
     */
    public boolean hasMoreElements()
    {
      if (_nextHeader != null)
	return true;
      
      if (_parent == null && _headerIter == null)
	return false;

      if (_parent != null) {
	while (_parent.hasMoreElements() && _nextHeader == null) {
	  _nextHeader = (String) _parent.nextElement();

	  if (_nextHeader == null) {
	  }
	  else if (_nextHeader.equalsIgnoreCase("If-Modified-Since") ||
		   _nextHeader.equalsIgnoreCase("If-None-Match") ||
		   _headers != null && _headers.get(_nextHeader) != null) {
	    _nextHeader = null;
	  }
	}

	if (_nextHeader == null)
	  _parent = null;
	else
	  return true;
      }

      if (_headerIter != null) {
	_nextHeader = _headerIter.next();
      }

      return _nextHeader != null;
    }
    
    public Object nextElement()
    {
      if (! hasMoreElements())
	return null;

      Object value = _nextHeader;
      _nextHeader = null;

      return value;
    }
  }
}
