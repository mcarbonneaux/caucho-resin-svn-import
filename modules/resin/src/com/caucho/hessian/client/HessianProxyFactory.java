/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package com.caucho.hessian.client;

import com.caucho.hessian.io.*;
import com.caucho.services.client.ServiceProxyFactory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Factory for creating Hessian client stubs.  The returned stub will
 * call the remote object for all methods.
 *
 * <pre>
 * String url = "http://localhost:8080/ejb/hello";
 * HelloHome hello = (HelloHome) factory.create(HelloHome.class, url);
 * </pre>
 *
 * After creation, the stub can be like a regular Java class.  Because
 * it makes remote calls, it can throw more exceptions than a Java class.
 * In particular, it may throw protocol exceptions.
 *
 * The factory can also be configured as a JNDI resource.  The factory
 * expects to parameters: "type" and "url", corresponding to the two
 * arguments to <code>create</code>
 *
 * In Resin 3.0, the above example would be configured as:
 * <pre>
 * &lt;reference>
 *   &lt;jndi-name>hessian/hello&lt;/jndi-name>
 *   &lt;factory>com.caucho.hessian.client.HessianProxyFactory&lt;/factory>
 *   &lt;init-param url="http://localhost:8080/ejb/hello"/>
 *   &lt;init-param type="test.HelloHome"/>
 * &lt;/reference>
 * </pre>
 *
 * To get the above resource, use JNDI as follows:
 * <pre>
 * Context ic = new InitialContext();
 * HelloHome hello = (HelloHome) ic.lookup("java:comp/env/hessian/hello");
 *
 * System.out.println("Hello: " + hello.helloWorld());
 * </pre>
 *
 * <h3>Authentication</h3>
 *
 * <p>The proxy can use HTTP basic authentication if the user and the
 * password are set.
 */
public class HessianProxyFactory implements ServiceProxyFactory, ObjectFactory {
  protected static Logger log
    = Logger.getLogger(HessianProxyFactory.class.getName());

  private SerializerFactory _serializerFactory;
  private HessianRemoteResolver _resolver;
  
  private String _user;
  private String _password;
  private String _basicAuth;

  private boolean _isOverloadEnabled = false;

  private boolean _isHessian2Reply = false;
  private boolean _isHessian2Request = false;

  private boolean _isChunkedPost = false;
  private boolean _isDebug = false;

  private long _readTimeout = -1;

  private String _connectionFactoryName = "jms/ConnectionFactory";

  /**
   * Creates the new proxy factory.
   */
  public HessianProxyFactory()
  {
    _resolver = new HessianProxyResolver(this);
  }

  /**
   * Sets the user.
   */
  public void setUser(String user)
  {
    _user = user;
    _basicAuth = null;
  }

  /**
   * Sets the password.
   */
  public void setPassword(String password)
  {
    _password = password;
    _basicAuth = null;
  }

  /**
   * Sets the name of the connection factory to use when connecting
   * to JMS Hessian services.
   */
  public void setConnectionFactoryName(String connectionFactoryName)
  {
    _connectionFactoryName = connectionFactoryName;
  }

  /**
   * Sets the debug
   */
  public void setDebug(boolean isDebug)
  {
    _isDebug = isDebug;
  }

  /**
   * Gets the debug
   */
  public boolean isDebug()
  {
    return _isDebug;
  }

  /**
   * Returns true if overloaded methods are allowed (using mangling)
   */
  public boolean isOverloadEnabled()
  {
    return _isOverloadEnabled;
  }

  /**
   * set true if overloaded methods are allowed (using mangling)
   */
  public void setOverloadEnabled(boolean isOverloadEnabled)
  {
    _isOverloadEnabled = isOverloadEnabled;
  }

  /**
   * Set true if should use chunked encoding on the request.
   */
  public void setChunkedPost(boolean isChunked)
  {
    _isChunkedPost = isChunked;
  }

  /**
   * Set true if should use chunked encoding on the request.
   */
  public boolean isChunkedPost()
  {
    return _isChunkedPost;
  }

  /**
   * The socket timeout on requests in milliseconds.
   */
  public long getReadTimeout()
  {
    return _readTimeout;
  }

  /**
   * The socket timeout on requests in milliseconds.
   */
  public void setReadTimeout(long timeout)
  {
    _readTimeout = timeout;
  }

  /**
   * True if the proxy can read Hessian 2 responses.
   */
  public void setHessian2Reply(boolean isHessian2)
  {
    _isHessian2Reply = isHessian2;
  }

  /**
   * True if the proxy should send Hessian 2 requests.
   */
  public void setHessian2Request(boolean isHessian2)
  {
    _isHessian2Request = isHessian2;

    if (isHessian2)
      _isHessian2Reply = true;
  }

  /**
   * Returns the remote resolver.
   */
  public HessianRemoteResolver getRemoteResolver()
  {
    return _resolver;
  }

  /**
   * Sets the serializer factory.
   */
  public void setSerializerFactory(SerializerFactory factory)
  {
    _serializerFactory = factory;
  }

  /**
   * Gets the serializer factory.
   */
  public SerializerFactory getSerializerFactory()
  {
    if (_serializerFactory == null)
      _serializerFactory = new SerializerFactory();
    
    return _serializerFactory;
  }

  /**
   * Creates the URL connection.
   */
  protected URLConnection openConnection(URL url)
    throws IOException
  {
    URLConnection conn = url.openConnection();

    conn.setDoOutput(true);

    if (_readTimeout > 0) {
      try {
	conn.setReadTimeout((int) _readTimeout);
      } catch (Throwable e) {
      }
    }

    conn.setRequestProperty("Content-Type", "x-application/hessian");

    if (_basicAuth != null)
      conn.setRequestProperty("Authorization", _basicAuth);
    else if (_user != null && _password != null) {
      _basicAuth = "Basic " + base64(_user + ":" + _password);
      conn.setRequestProperty("Authorization", _basicAuth);
    }

    return conn;
  }

  /**
   * Creates a new proxy with the specified URL.  The API class uses
   * the java.api.class value from _hessian_
   *
   * @param url the URL where the client object is located.
   *
   * @return a proxy to the object with the specified interface.
   */
  public Object create(String url)
    throws MalformedURLException, ClassNotFoundException
  {
    HessianMetaInfoAPI metaInfo;

    metaInfo = (HessianMetaInfoAPI) create(HessianMetaInfoAPI.class, url);

    String apiClassName =
      (String) metaInfo._hessian_getAttribute("java.api.class");

    if (apiClassName == null)
      throw new HessianRuntimeException(url + " has an unknown api.");

    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    Class apiClass = Class.forName(apiClassName, false, loader);

    return create(apiClass, url);
  }

  /**
   * Creates a new proxy with the specified URL.  The returned object
   * is a proxy with the interface specified by api.
   *
   * <pre>
   * String url = "http://localhost:8080/ejb/hello");
   * HelloHome hello = (HelloHome) factory.create(HelloHome.class, url);
   * </pre>
   *
   * @param api the interface the proxy class needs to implement
   * @param url the URL where the client object is located.
   *
   * @return a proxy to the object with the specified interface.
   */
  public Object create(Class api, String urlName)
    throws MalformedURLException
  {
    return create(api, urlName,
		  Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates a new proxy with the specified URL.  The returned object
   * is a proxy with the interface specified by api.
   *
   * <pre>
   * String url = "http://localhost:8080/ejb/hello");
   * HelloHome hello = (HelloHome) factory.create(HelloHome.class, url);
   * </pre>
   *
   * @param api the interface the proxy class needs to implement
   * @param url the URL where the client object is located.
   *
   * @return a proxy to the object with the specified interface.
   */
  public Object create(Class api, String urlName, ClassLoader loader)
    throws MalformedURLException
  {
    InvocationHandler handler = null;

    if (urlName.startsWith("jms:")) {
      String jndiName = urlName.substring("jms:".length());

      try {
        handler = new HessianJMSProxy(this, jndiName, _connectionFactoryName);
      } catch (Exception e) {
        log.info("Unable to create JMS proxy: " + e);
        return null;
      }
    } 
    else {
      URL url = new URL(urlName); 
      handler = new HessianProxy(this, url);
    }

    return Proxy.newProxyInstance(loader,
                                  new Class[] { api,
                                                HessianRemoteObject.class },
                                  handler);
  }

  public AbstractHessianInput getHessianInput(InputStream is)
  {
    AbstractHessianInput in;

    if (_isDebug)
      is = new HessianDebugInputStream(is, new PrintWriter(System.out));

    if (_isHessian2Reply)
      in = new Hessian2Input(is);
    else
      in = new HessianInput(is);
    
    in.setRemoteResolver(getRemoteResolver());

    in.setSerializerFactory(getSerializerFactory());

    return in;
  }

  public AbstractHessianOutput getHessianOutput(OutputStream os)
  {
    AbstractHessianOutput out;

    if (_isHessian2Request)
      out = new Hessian2Output(os);
    else {
      HessianOutput out1 = new HessianOutput(os);
      out = out1;

      if (_isHessian2Reply)
        out1.setVersion(2);
    }
      
    out.setSerializerFactory(getSerializerFactory());

    return out;
  }

  /**
   * JNDI object factory so the proxy can be used as a resource.
   */
  public Object getObjectInstance(Object obj, Name name,
                                  Context nameCtx, Hashtable<?,?> environment)
    throws Exception
  {
    Reference ref = (Reference) obj;

    String api = null;
    String url = null;
    String user = null;
    String password = null;

    for (int i = 0; i < ref.size(); i++) {
      RefAddr addr = ref.get(i);

      String type = addr.getType();
      String value = (String) addr.getContent();

      if (type.equals("type"))
	api = value;
      else if (type.equals("url"))
	url = value;
      else if (type.equals("user"))
	setUser(value);
      else if (type.equals("password"))
	setPassword(value);
    }

    if (url == null)
      throw new NamingException("`url' must be configured for HessianProxyFactory.");
    // XXX: could use meta protocol to grab this
    if (api == null)
      throw new NamingException("`type' must be configured for HessianProxyFactory.");

    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    Class apiClass = Class.forName(api, false, loader);

    return create(apiClass, url);
  }

  /**
   * Creates the Base64 value.
   */
  private String base64(String value)
  {
    StringBuffer cb = new StringBuffer();

    int i = 0;
    for (i = 0; i + 2 < value.length(); i += 3) {
      long chunk = (int) value.charAt(i);
      chunk = (chunk << 8) + (int) value.charAt(i + 1);
      chunk = (chunk << 8) + (int) value.charAt(i + 2);
        
      cb.append(encode(chunk >> 18));
      cb.append(encode(chunk >> 12));
      cb.append(encode(chunk >> 6));
      cb.append(encode(chunk));
    }
    
    if (i + 1 < value.length()) {
      long chunk = (int) value.charAt(i);
      chunk = (chunk << 8) + (int) value.charAt(i + 1);
      chunk <<= 8;

      cb.append(encode(chunk >> 18));
      cb.append(encode(chunk >> 12));
      cb.append(encode(chunk >> 6));
      cb.append('=');
    }
    else if (i < value.length()) {
      long chunk = (int) value.charAt(i);
      chunk <<= 16;

      cb.append(encode(chunk >> 18));
      cb.append(encode(chunk >> 12));
      cb.append('=');
      cb.append('=');
    }

    return cb.toString();
  }

  public static char encode(long d)
  {
    d &= 0x3f;
    if (d < 26)
      return (char) (d + 'A');
    else if (d < 52)
      return (char) (d + 'a' - 26);
    else if (d < 62)
      return (char) (d + '0' - 52);
    else if (d == 62)
      return '+';
    else
      return '/';
  }
}

