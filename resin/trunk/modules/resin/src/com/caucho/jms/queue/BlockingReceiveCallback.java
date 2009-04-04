/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.jms.queue;

import java.io.Serializable;

import java.util.concurrent.locks.LockSupport;

/**
 * Listener for queue message available
 */
public class BlockingReceiveCallback implements MessageCallback
{
  private Thread _thread;

  private String _msgId;
  private Serializable _payload;
  
    
  public BlockingReceiveCallback()
  {
    _thread = Thread.currentThread();
  }
  
  /**
   * Notifies a listener that a message is available from the queue.
   *
   * The message handler MUST use a different thread to retrieve the
   * message from the queue, i.e. the <code>notifyMessageAvailable</code>
   * implementation must spawn or wake a thread to handle the actual
   * message.
   * 
   * @return true if the consumer can handle the message
   */
  public boolean messageReceived(String msgId, Serializable payload)
  {
    _msgId = msgId;
    _payload = payload;

    Thread thread = _thread;

    if (thread != null)
      LockSupport.unpark(thread);

    return true;
  }

  public Serializable receive(AbstractMemoryQueue queue,
			      boolean isAutoAck, long timeout)
  {
    if (! queue.listen(this)) {
      return null;
    }

    if (timeout > 0)
      LockSupport.parkNanos(timeout * 1000000L);

    if (_msgId == null)
      queue.removeMessageCallback(this);

    _thread = null;

    return _payload;
  }
}

