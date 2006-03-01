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

package com.caucho.vfs;

import java.io.*;
import java.net.*;
import java.util.*;

import java.nio.channels.SelectableChannel;

import java.security.cert.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * Abstract socket to handle both normal sockets and bin/resin sockets.
 */
abstract public class QSocket {
  /**
   * Returns the server inet address that accepted the request.
   */
  abstract public InetAddress getLocalAddress();
  
  /**
   * Returns the server port that accepted the request.
   */
  abstract public int getLocalPort();

  /**
   * Returns the remote client's inet address.
   */
  abstract public InetAddress getRemoteAddress();

  /**
   * Returns the remote client's inet address.
   */
  public String getRemoteHost()
  {
    return getRemoteAddress().getHostAddress();
  }

  /**
   * Returns the remote client's inet address.
   */
  public int getRemoteAddress(byte []buffer, int offset, int length)
  {
    String name = getRemoteHost();
    int len = name.length();

    for (int i = 0; i < len; i++)
      buffer[i + offset] = (byte) name.charAt(i);

    return len;
  }
  
  /**
   * Returns the remote client's port.
   */
  abstract public int getRemotePort();

  /**
   * Returns true if the connection is secure.
   */
  public boolean isSecure()
  {
    return false;
  }

  /**
   * Returns any selectable channel.
   */
  public SelectableChannel getSelectableChannel()
  {
    return null;
  }

  /**
   * Read non-blocking
   */
  public boolean readNonBlock(int ms)
  {
    return false;
  }

  /**
   * Returns the secure cipher algorithm.
   */
  public String getCipherSuite()
  {
    return null;
  }

  /**
   * Returns the bits in the socket.
   */
  public int getCipherBits()
  {
    return 0;
  }

  /**
   * Returns the client certificate.
   */
  public X509Certificate getClientCertificate()
    throws java.security.cert.CertificateException
  {
    return null;
  }

  /**
   * Returns the client certificate chain.
   */
  public X509Certificate []getClientCertificates()
    throws java.security.cert.CertificateException
  {
    X509Certificate cert = getClientCertificate();

    if (cert != null)
      return new X509Certificate[] { cert };
    else
      return null;
  }
  
  /**
   * Returns a stream impl for the socket encapsulating the
   * input and output stream.
   */
  abstract public StreamImpl getStream()
    throws IOException;

  /**
   * returns true if it's closed.
   */
  abstract public boolean isClosed();
  
  abstract public void close()
    throws IOException;
}

