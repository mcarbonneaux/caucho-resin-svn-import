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

package com.caucho.server.hmux;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import java.security.cert.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.log.Log;

import com.caucho.util.Base64;
import com.caucho.util.Crc64;
import com.caucho.util.CharBuffer;

import com.caucho.vfs.*;
import com.caucho.jsp.*;

import com.caucho.server.cluster.Server;

import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.dispatch.DispatchServer;

import com.caucho.server.connection.Connection;
import com.caucho.server.connection.AbstractHttpRequest;

import com.caucho.server.port.ServerRequest;

import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.WebAppController;
import com.caucho.server.webapp.ErrorPageManager;

import com.caucho.server.host.Host;

import com.caucho.server.http.InvocationKey;

import com.caucho.server.cluster.*;

/**
 * Handles the filter mapping (config) requests from a remote dispatcher.
 */
public class HmuxDispatchRequest {
  private static final Logger log = Log.open(HmuxDispatchRequest.class);

  // other, specialized protocols
  public static final int HMUX_HOST =      'h';
  public static final int HMUX_QUERY_ALL = 'q';
  public static final int HMUX_QUERY_URL = 'r';
  public static final int HMUX_QUERY_SERVER = 's';
  public static final int HMUX_WEB_APP =   'a';
  public static final int HMUX_MATCH =     'm';
  public static final int HMUX_IGNORE =    'i';
  public static final int HMUX_ETAG =      'e';
  public static final int HMUX_NO_CHANGE = 'n';
  public static final int HMUX_CLUSTER =   'c';
  public static final int HMUX_SRUN    =   's';
  public static final int HMUX_SRUN_BACKUP =   'b';

  private CharBuffer _cb = new CharBuffer();

  private HmuxRequest _request;
  private Server _server;

  private int _srunIndex;

  public HmuxDispatchRequest(HmuxRequest request)
  {
    _request = request;

    _server = (Server) request.getDispatchServer();
  }

  /**
   * Handles a new request.  Initializes the protocol handler and
   * the request streams.
   *
   * <p>Note: ClientDisconnectException must be rethrown to
   * the caller.
   */
  public boolean handleRequest(ReadStream is, WriteStream os)
    throws IOException
  {
    CharBuffer cb = _cb;
    boolean isLoggable = log.isLoggable(Level.FINE);
    int code;
    int len;
    String host = "";
    String etag = null;

    while (true) {
      code = is.read();

      switch (code) {
      case -1:
        if (isLoggable)
          log.fine(dbgId() + "end of file");
        return false;

      case HmuxRequest.HMUX_QUIT:
        if (isLoggable)
          log.fine(dbgId() + (char) code + ": end of request");
        return true;

      case HmuxRequest.HMUX_EXIT:
        if (isLoggable)
          log.fine(dbgId() + (char) code + ": end of socket");

        return false;

      case HMUX_ETAG:
        len = (is.read() << 8) + is.read();
	_cb.clear();
	is.readAll(_cb, len);
	etag = _cb.toString();

	if (isLoggable)
	  log.fine(dbgId() + "etag: " + etag);
        break;

      case HMUX_HOST:
        len = (is.read() << 8) + is.read();
	_cb.clear();
	is.readAll(_cb, len);
	host = _cb.toString();

	if (isLoggable)
	  log.fine(dbgId() + "host: " + host);
        break;

      case HMUX_QUERY_ALL:
        len = (is.read() << 8) + is.read();
	_cb.clear();
	is.readAll(_cb, len);

	if (isLoggable)
	  log.fine(dbgId() + "query: " + _cb);

        queryAll(os, host, _cb.toString(), etag);
        break;

	/*
      case HMUX_QUERY_SERVER:
        len = (is.read() << 8) + is.read();
	_cb.clear();
	is.readAll(_cb, len);

	if (isLoggable)
	  log.fine(dbgId() + "query-server: " + _cb);

        queryCluster(os, host, _cb.toString());
        break;
	*/
        
      default:
        len = (is.read() << 8) + is.read();

	if (isLoggable)
	  log.fine(dbgId() + (char) code + " " + len + " (dispatch)");
	is.skip(len);
	break;
      }
    }

    // _filter.setClientClosed(true);

    // return false;
  }
  
  /**
   * Returns the url.
   */
  private void queryAll(WriteStream os, String hostName,
			String url, String etag)
    throws IOException
  {
    int channel = 2;
    boolean isLoggable = log.isLoggable(Level.FINE);
    
    os.write(HmuxRequest.HMUX_CHANNEL);
    os.write(channel >> 8);
    os.write(channel);

    Host host = _server.getHost(hostName, 80);
    if (host == null) {
      writeString(os, HmuxRequest.HMUX_HEADER, "check-interval");
      writeString(os, HmuxRequest.HMUX_STRING,
		  String.valueOf(_server.getDependencyCheckInterval() / 1000));
      writeString(os, HMUX_WEB_APP, "");

      if (isLoggable)
	log.fine(dbgId() + "host '" + host + "' not configured");
      return;
    }
    else if (! host.isActive()) {
      if (etag == null) {
	// take control until more information is known
	writeString(os, HMUX_WEB_APP, "");
	writeString(os, HMUX_MATCH, "/*");
      }
      else {
	sendQuery(null, host, hostName, url);

	writeString(os, HMUX_NO_CHANGE, "");
      }

      if (isLoggable)
	log.fine(dbgId() + "host '" + host + "' not active");
      return;
    }

    if (host.getConfigETag() == null)
      sendQuery(null, host, hostName, url);

    if (etag == null) {
    }
    else if (etag.equals(host.getConfigETag())) {
      if (isLoggable)
	log.fine(dbgId() + "host '" + host + "' no change");
      
      writeString(os, HMUX_NO_CHANGE, "");
      return;
    }
    else {
      if (isLoggable)
	log.fine(dbgId() + "host '" + host + "' changed");
    }
    
    sendQuery(os, host, hostName, url);
  }

  /**
   * Writes the host data, returning the crc
   */
  private void sendQuery(WriteStream os, Host host,
			 String hostName, String url)
    throws IOException
  {
    boolean isLoggable = log.isLoggable(Level.FINE);
    
    long crc64 = 0;
    
    queryServer(os);
    
    writeString(os, HMUX_HOST, host.getHostName());

    if (hostName.equals(host.getHostName())) {
      crc64 = queryCluster(os, host, crc64);
      
      WebAppController controller = host.findByURI(url);
      if (controller != null) {
	try {
	  controller.request();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }

      ArrayList<WebAppController> appList = host.getWebAppList();

      for (int i = 0; i < appList.size(); i++) {
	WebAppController appEntry = appList.get(i);

	if (appEntry.getParent() != null &&
	    appEntry.getParent().isDynamicDeploy()) {
	  continue;
	}
	
	writeString(os, HMUX_WEB_APP, appEntry.getContextPath());
	if (isLoggable)
	  log.fine(dbgId() + "web-app '" + appEntry.getContextPath() + "'");

	crc64 = Crc64.generate(crc64, appEntry.getContextPath());

	WebApp app = appEntry.getWebApp();

	if (appEntry.isDynamicDeploy()) {
	  writeString(os, HMUX_MATCH, "/*");
	
	  crc64 = Crc64.generate(crc64, "/*");
	  
	  if (isLoggable)
	    log.fine(dbgId() + "dynamic '" + appEntry.getContextPath() + "'");
	}
	else if (app == null || ! app.isActive()) {
	  if (isLoggable)
	    log.fine(dbgId() + "not active '" + appEntry.getContextPath() + "'");
	  
	  writeString(os, HMUX_NO_CHANGE, "");
	}
	else {
	  if (isLoggable)
	    log.fine(dbgId() + "active '" + appEntry.getContextPath() + "'");
	  ArrayList<String> patternList = app.getServletMappingPatterns();

	  for (int j = 0; patternList != null && j < patternList.size(); j++) {
	    String pattern = patternList.get(j);

	    writeString(os, HMUX_MATCH, pattern);
	  
	    crc64 = Crc64.generate(crc64, pattern);
	  }
	  
	  patternList = app.getServletIgnoreMappingPatterns();

	  for (int j = 0; patternList != null && j < patternList.size(); j++) {
	    String pattern = patternList.get(j);

	    writeString(os, HMUX_IGNORE, pattern);
	  
	    crc64 = Crc64.generate(crc64, "i");
	    crc64 = Crc64.generate(crc64, pattern);
	  }
	}
      }

      CharBuffer cb = new CharBuffer();
      Base64.encode(cb, crc64);
      String newETag = cb.close();
      host.setConfigETag(newETag);
    }
    
    writeString(os, HMUX_ETAG, host.getConfigETag());
  }
  
  /**
   * Queries the cluster.
   */
  private long queryCluster(WriteStream os, Host host, long crc64)
    throws IOException
  {
    /*
    int channel = 2;
    
    os.write(HmuxRequest.HMUX_CHANNEL);
    os.write(channel >> 8);
    os.write(channel);
    */

    Cluster cluster = host.getCluster();

    if (cluster == null)
      return 0;

    writeString(os, HMUX_CLUSTER, cluster.getId());

    crc64 = Crc64.generate(crc64, cluster.getId());

    writeString(os, HmuxRequest.HMUX_HEADER, "live-time");
    writeString(os, HmuxRequest.HMUX_STRING, "" + (cluster.getClientMaxIdleTime() / 1000));

    writeString(os, HmuxRequest.HMUX_HEADER, "dead-time");
    writeString(os, HmuxRequest.HMUX_STRING, "" + (cluster.getClientFailRecoverTime() / 1000));

    ClusterServer []servers = cluster.getServerList();

    for (int i = 0; i < servers.length; i++) {
      ClusterServer server = servers[i];

      if (server != null && server.getClusterPort() != null) {
	ClusterPort port = server.getClusterPort();
	
	String srunHost = port.getAddress() + ":" + port.getPort();

	/*
	if (server.isBackup())
	  writeString(os, HMUX_SRUN_BACKUP, srunHost);
	else
	*/
	writeString(os, HMUX_SRUN, srunHost);
      
	crc64 = Crc64.generate(crc64, srunHost);
      }
    }

    return crc64;
  }
  
  /**
   * Queries the cluster.
   */
  private void queryServer(WriteStream os)
    throws IOException
  {
    writeString(os, HmuxRequest.HMUX_HEADER, "check-interval");
    writeString(os, HmuxRequest.HMUX_STRING,
		String.valueOf(_server.getDependencyCheckInterval() / 1000));
    
    writeString(os, HmuxRequest.HMUX_HEADER, "cookie");
    writeString(os, HmuxRequest.HMUX_STRING,
		_server.getSessionCookie());
    writeString(os, HmuxRequest.HMUX_HEADER, "ssl-cookie");
    writeString(os, HmuxRequest.HMUX_STRING,
		_server.getSSLSessionCookie());
    writeString(os, HmuxRequest.HMUX_HEADER, "session-url-prefix");
    writeString(os, HmuxRequest.HMUX_STRING,
		_server.getSessionURLPrefix());
    writeString(os, HmuxRequest.HMUX_HEADER, "alt-session-url-prefix");
    writeString(os, HmuxRequest.HMUX_STRING,
		_server.getAlternateSessionURLPrefix());

    if (_server.getConnectionErrorPage() != null) {
      writeString(os, HmuxRequest.HMUX_HEADER, "connection-error-page");
      writeString(os, HmuxRequest.HMUX_STRING,
		  _server.getConnectionErrorPage());
    }
  }

  void writeString(WriteStream os, int code, String value)
    throws IOException
  {
    if (os == null)
      return;
    
    if (value == null)
      value = "";
    
    int len = value.length();

    os.write(code);
    os.write(len >> 8);
    os.write(len);
    os.print(value);
    
    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + (char)code + " " + value);
  }

  void writeString(WriteStream os, int code, CharBuffer value)
    throws IOException
  {
    if (os == null)
      return;
    
    int len = value.length();

    os.write(code);
    os.write(len >> 8);
    os.write(len);
    os.print(value);
    
    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + (char)code + " " + value);
  }

  private String dbgId()
  {
    return _request.dbgId();
  }
}
