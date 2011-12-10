/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.remote.websocket;

import java.io.IOException;
import java.io.InputStream;

import com.caucho.websocket.WebSocketContext;

/**
 * WebSocketInputStream reads a single WebSocket packet.
 *
 * <code><pre>
 * +-+------+---------+-+---------+
 * |F|xxx(3)|opcode(4)|R|len(7)   |
 * +-+------+---------+-+---------+
 * 
 * OPCODES
 *   0 - cont
 *   1 - text
 *   2 - binary
 *   8 - close
 * </pre></code>
 */
public class UnmaskedFrameInputStream extends FrameInputStream
{
  private InputStream _is;
  
  private boolean _isFinal;
  private int _op;
  private long _length;

  public UnmaskedFrameInputStream()
  {
  }
  
  @Override
  public void init(WebSocketContext cxt, InputStream is)
  {
    super.init(cxt, is);
    
    if (is == null)
      throw new NullPointerException();
    
    _is = is;
  }

  @Override
  public int getOpcode()
  {
    return _op;
  }
  
  @Override
  public boolean isFinal()
  {
    return _isFinal;
  }

  @Override
  public long getLength()
  {
    return _length;
  }

  @Override
  public int read()
    throws IOException
  {
    long length = _length;
    
    if (length > 0) {
      int ch = _is.read();
      
      _length = length - 1;

      return ch;
    }
    else
      return -1;
  }
  
  @Override
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    if (_length == 0 && ! _isFinal) {
      readFrameHeader();
      
      if (_length == 0)
        return -1;
    }
    
    int sublen = length;
    
    if (_length < sublen)
      sublen = (int) _length;
    
    sublen = _is.read(buffer, offset, sublen);
    
    if (sublen < 0)
      return sublen;
    
    _length -= sublen;
    
    return sublen;
  }

  @Override
  protected boolean readFrameHeaderImpl()
    throws IOException
  {
    InputStream is = _is;
    
    int frame1 = is.read();
    int frame2 = is.read();

    if (frame2 < 0)
      return false;

    boolean isFinal = (frame1 & FLAG_FIN) == FLAG_FIN;
    _op = frame1 & 0xf;

    _isFinal = isFinal;

    long length = frame2 & 0x7f;

    if (length < 0x7e) {
    }
    else if (length == 0x7e) {
      length = ((((long) is.read()) << 8)
          + (((long) is.read())));
    }
    else {
      length = ((((long) is.read()) << 56)
          + (((long) is.read()) << 48)
          + (((long) is.read()) << 40)
          + (((long) is.read()) << 32)
          + (((long) is.read()) << 24)
          + (((long) is.read()) << 16)
          + (((long) is.read()) << 8)
          + (((long) is.read())));
    }

    _length = length;

    return true;
  }
}
