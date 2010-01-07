/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.server.http;

import com.caucho.util.CharBuffer;
import com.caucho.vfs.*;
import com.caucho.server.dispatch.ServletInvocation;
import com.caucho.server.session.SessionImpl;
import com.caucho.server.session.SessionManager;
import com.caucho.server.webapp.WebApp;
import com.caucho.servlet.DuplexContext;
import com.caucho.servlet.DuplexListener;
import com.caucho.security.RoleMapManager;
import com.caucho.security.Login;
import com.caucho.util.Alarm;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.security.*;
import javax.servlet.*;
import javax.servlet.http.*;

abstract public class AbstractCauchoRequest implements CauchoRequest {
  private static final Logger log
    = Logger.getLogger(AbstractCauchoRequest.class.getName());
  
  protected int _sessionGroup = -1;

  private boolean _sessionIsLoaded;
  private SessionImpl _session;

  public RequestDispatcher getRequestDispatcher(String path)
  {
    if (path == null || path.length() == 0)
      return null;
    else if (path.charAt(0) == '/')
      return getWebApp().getRequestDispatcher(path);
    else {
      CharBuffer cb = new CharBuffer();

      WebApp webApp = getWebApp();

      String servletPath = getPageServletPath();
      if (servletPath != null)
        cb.append(servletPath);
      String pathInfo = getPagePathInfo();
      if (pathInfo != null)
        cb.append(pathInfo);

      int p = cb.lastIndexOf('/');
      if (p >= 0)
        cb.setLength(p);
      cb.append('/');
      cb.append(path);

      if (webApp != null)
        return webApp.getRequestDispatcher(cb.toString());

      return null;
    }
  }

  public String getRealPath(String uri)
  {
    WebApp webApp = getWebApp();

    return webApp.getRealPath(uri);
  }

  /**
   * Returns the URL for the request
   */
  public StringBuffer getRequestURL()
  {
    StringBuffer sb = new StringBuffer();

    sb.append(getScheme());
    sb.append("://");

    sb.append(getServerName());
    int port = getServerPort();

    if (port > 0 &&
        port != 80 &&
        port != 443) {
      sb.append(":");
      sb.append(port);
    }

    sb.append(getRequestURI());

    return sb;
  }

  /**
   * Returns the real path of pathInfo.
   */
  public String getPathTranslated()
  {
    // server/106w
    String pathInfo = getPathInfo();

    if (pathInfo == null)
      return null;
    else
      return getRealPath(pathInfo);
  }

  public boolean isTop()
  {
    return false;
  }

  //
  // session management
  //

  public abstract boolean isSessionIdFromCookie();
  
  public abstract String getSessionId();
  
  public abstract void setSessionId(String sessionId);
  
  /**
   * Returns the memory session.
   */
  public HttpSession getMemorySession()
  {
    if (_session != null && _session.isValid())
      return _session;
    else
      return null;
  }

  /**
   * Returns the current session, creating one if necessary.
   * Sessions are a convenience for keeping user state
   * across requests.
   */
  public HttpSession getSession()
  {
    return getSession(true);
  }

  /**
   * Returns the current session.
   *
   * @param create true if a new session should be created
   *
   * @return the current session
   */
  public HttpSession getSession(boolean create)
  {
    if (_session != null) {
      if (_session.isValid())
        return _session;
    }
    else if (! create && _sessionIsLoaded)
      return null;

    _sessionIsLoaded = true;

    _session = createSession(create);

    return _session;
  }

  /**
   * Returns the current session.
   *
   * @return the current session
   */
  public HttpSession getLoadedSession()
  {
    if (_session != null && _session.isValid())
      return _session;
    else
      return null;
  }

  /**
   * Returns true if the HTTP request's session id refers to a valid
   * session.
   */
  public boolean isRequestedSessionIdValid()
  {
    String id = getRequestedSessionId();

    if (id == null)
      return false;

    SessionImpl session = _session;

    if (session == null)
      session = (SessionImpl) getSession(false);

    return session != null && session.isValid() && session.getId().equals(id);
  }

  /**
   * Returns the current session.
   *
   * XXX: duplicated in RequestAdapter
   *
   * @param create true if a new session should be created
   *
   * @return the current session
   */
  private SessionImpl createSession(boolean create)
  {
    SessionManager manager = getSessionManager();

    if (manager == null)
      return null;

    String id = getSessionId();

    long now = Alarm.getCurrentTime();

    SessionImpl session
      = manager.createSession(create, this, id, now,
                              isSessionIdFromCookie());

    if (session != null
        && (id == null || ! session.getId().equals(id))
        && manager.enableSessionCookies()) {
      setSessionId(session.getId());
    }

    return session;
  }

  /**
   * Returns the session manager.
   */
  protected final SessionManager getSessionManager()
  {
    WebApp webApp = getWebApp();

    if (webApp != null)
      return webApp.getSessionManager();
    else
      return null;
  }

  /**
   * Returns the session cookie.
   */
  protected final String getSessionCookie(SessionManager manager)
  {
    if (isSecure())
      return manager.getSSLCookieName();
    else
      return manager.getCookieName();
  }

  public int getSessionGroup()
  {
    return _sessionGroup;
  }

  void saveSession()
  {
    SessionImpl session = _session;
    if (session != null)
      session.save();
  }

  //
  // security
  //

  protected String getRunAs()
  {
    return null;
  }

  protected ServletInvocation getInvocation()
  {
    return null;
  }

  /**
   * Returns the next request in a chain.
   */
  protected HttpServletRequest getRequest()
  {
    return null;
  }

  /**
   * Returns true if the user represented by the current request
   * plays the named role.
   *
   * @param role the named role to test.
   * @return true if the user plays the role.
   */
  public boolean isUserInRole(String role)
  {
    ServletInvocation invocation = getInvocation();

    if (invocation == null) {
      if (getRequest() != null)
        return getRequest().isUserInRole(role);
      else
        return false;
    }
    
    HashMap<String,String> roleMap = invocation.getSecurityRoleMap();

    if (roleMap != null) {
      String linkRole = roleMap.get(role);

      if (linkRole != null)
        role = linkRole;
    }

    String runAs = getRunAs();
    
    if (runAs != null)
      return runAs.equals(role);

    WebApp webApp = getWebApp();

    Principal user = getUserPrincipal();

    if (user == null) {
      if (log.isLoggable(Level.FINE))
        log.fine(this + " no user for isUserInRole");

      return false;
    }

    RoleMapManager roleManager
      = webApp != null ? webApp.getRoleMapManager() : null;

    if (roleManager != null) {
      Boolean result = roleManager.isUserInRole(role, user);

      if (result != null) {
        if (log.isLoggable(Level.FINE))
          log.fine(this + " userInRole(" + role + ")->" + result);

        return result;
      }
    }

    Login login = webApp == null ? null : webApp.getLogin();

    boolean inRole = login != null && login.isUserInRole(user, role);

    if (log.isLoggable(Level.FINE)) {
      if (login == null)
        log.fine(this + " no Login for isUserInRole");
      else if (user == null)
        log.fine(this + " no user for isUserInRole");
      else if (inRole)
        log.fine(this + " " + user + " is in role: " + role);
      else
        log.fine(this + " failed " + user + " in role: " + role);
    }

    return inRole;
  }
  
  
  //
  // lifecycle
  //
  
  protected void finishRequest()
    throws IOException
  {
    SessionImpl session = _session;

    if (session != null)
      session.finishRequest();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
