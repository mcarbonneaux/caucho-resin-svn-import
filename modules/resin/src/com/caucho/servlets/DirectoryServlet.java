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

package com.caucho.servlets;

import java.io.*;
import java.util.*;

import javax.servlet.http.*;
import javax.servlet.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.i18n.CharacterEncoding;

import com.caucho.server.webapp.Application;

import com.caucho.server.connection.CauchoRequest;
import com.caucho.server.connection.CauchoResponse;

public class DirectoryServlet extends HttpServlet {
  Application _app;
  Path _context;
  private boolean _enable = true;

  public DirectoryServlet(Path context)
  {
    _context = context;
  }

  public DirectoryServlet()
  {
    this(Vfs.lookup());
  }

  public void setEnable(boolean enable)
  {
    _enable = enable;
  }

  public void init()
  {
    _app = (Application) getServletContext();
    _context = _app.getAppDir();
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)

    throws IOException
  {
    if (! _enable) {
      res.sendError(404);
      return;
    }
    
    CauchoRequest cauchoReq = null;

    if (req instanceof CauchoRequest)
      cauchoReq = (CauchoRequest) req;

    String uri = req.getRequestURI();
    boolean redirect = false;
 
    if (uri.length() > 0 && uri.charAt(uri.length() - 1) != '/') {
      res.sendRedirect(uri + "/");
      return;
    }
 
    String encoding = CharacterEncoding.getLocalEncoding();
    if (encoding == null)
      res.setContentType("text/html");
    else
      res.setContentType("text/html; charset=" + encoding);

    boolean isInclude = false;

    if (cauchoReq != null) {
      uri = cauchoReq.getPageURI();
      isInclude = ! uri.equals(cauchoReq.getRequestURI());
    }
    else {
      uri = (String) req.getAttribute("javax.servlet.include.request_uri");
      if (uri != null)
        isInclude = true;
      else
        uri = req.getRequestURI();
    }

    CharBuffer cb = CharBuffer.allocate();
    String servletPath;

    if (cauchoReq != null)
      servletPath = cauchoReq.getPageServletPath();
    else if (isInclude)
      servletPath = (String) req.getAttribute("javax.servlet.include.servlet_path");
    else
      servletPath = req.getServletPath();
        
    if (servletPath != null)
      cb.append(servletPath);
      
    String pathInfo;
    if (cauchoReq != null)
      pathInfo = cauchoReq.getPagePathInfo();
    else if (isInclude)
      pathInfo = (String) req.getAttribute("javax.servlet.include.path_info");
    else
      pathInfo = req.getPathInfo();
        
    if (pathInfo != null)
      cb.append(pathInfo);
    
    String relPath = cb.close();

    String filename = getServletContext().getRealPath(relPath);
    Path path = _context.lookupNative(filename);
    
    String rawpath = java.net.URLDecoder.decode(uri);

    PrintWriter pw = res.getWriter();

    if (rawpath.length() == 0 || rawpath.charAt(0) != '/')
      rawpath = "/" + rawpath;

    boolean endsSlash = rawpath.charAt(rawpath.length() - 1) == '/';
    String tail = "";
    if (! endsSlash) {
      int p = rawpath.lastIndexOf('/');
      tail = rawpath.substring(p + 1) + "/";
      rawpath = rawpath + "/";
    }

    pw.println("<html>");
    pw.println("<head>");
    pw.println("<title>Directory of " + rawpath + "</title>");
    pw.println("</head>");
    pw.println("<body>");

    pw.println("<h1>Directory of " + rawpath + "</h1>");

    pw.println("<ul>");

    Iterator i = path.iterator();
    while (i.hasNext()) {
      String name = (String) i.next();

      if (name.equalsIgnoreCase("web-inf") ||
          name.equalsIgnoreCase("meta-inf"))
        continue;

      String enc = URLUtil.encodeURL(tail + name);

      pw.println("<li><a href='" + enc + "'>" + name + "</a>");
    }
    pw.println("</ul>");
    pw.println("</body>");
    pw.println("</html>");
    pw.close();
  }
}
