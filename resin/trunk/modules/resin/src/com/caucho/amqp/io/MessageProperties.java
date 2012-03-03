/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.amqp.io;

import java.io.IOException;

/**
 * The message properties header.
 */
public final class MessageProperties extends AmqpAbstractComposite {
  private long _messageId; // messageid
  private String _contentType; // symbol
  
  public String getContentType()
  {
    return _contentType;
  }
  
  public void setContentType(String contentType)
  {
    _contentType = contentType;
  }
  
  @Override
  public long getDescriptorCode()
  {
    return ST_MESSAGE_PROPERTIES;
  }
  
  @Override
  public MessageProperties createInstance()
  {
    return new MessageProperties();
  }
  
  @Override
  public void readBody(AmqpReader in, int count)
    throws IOException
  {
    _messageId = in.readInt();
    _contentType = in.readSymbol();
  }
  
  @Override
  public int writeBody(AmqpWriter out)
    throws IOException
  {
    out.writeInt((int) _messageId);
    out.writeSymbol(_contentType);
    
    return 2;
  }
}