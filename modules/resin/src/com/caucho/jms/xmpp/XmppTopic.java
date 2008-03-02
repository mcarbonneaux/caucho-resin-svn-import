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

package com.caucho.jms.xmpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.*;

import javax.annotation.*;
import javax.jms.*;
import javax.webbeans.*;

import com.caucho.jms.memory.*;
import com.caucho.jms.message.*;
import com.caucho.jms.queue.*;
import com.caucho.jms.connection.*;
import com.caucho.util.*;
import com.caucho.webbeans.manager.*;

/**
 * Implements an xmpp topic.
 */
public class XmppTopic extends AbstractTopic
{
  private static final L10N L = new L10N(XmppTopic.class);
  
  private static final Logger log
    = Logger.getLogger(XmppTopic.class.getName());

  private ArrayList<AbstractQueue> _subscriptionList
    = new ArrayList<AbstractQueue>();

  private XmppPubSubLeaf _xmppNode;

  private int _id;

  //
  // JMX configuration
  //

  /**
   * Returns the configuration URL.
   */
  public String getUrl()
  {
    return "xmpp:name=" + getName();
  }

  @PostConstruct
  public void init()
  {
    super.init();

    WebBeansContainer container = WebBeansContainer.create();

    ComponentFactory comp = container.resolveByType(XmppProtocol.class);

    if (comp == null)
      throw new java.lang.IllegalStateException(L.l("Need xmpp protocol"));
    
    XmppProtocol xmpp = (XmppProtocol) comp.get();

    if (xmpp == null)
      throw new java.lang.IllegalStateException(L.l("Need xmpp protocol"));


    _xmppNode = xmpp.createNode(getName());
  }
  

  @Override
  public AbstractQueue createSubscriber(JmsSession session,
                                        String name,
                                        boolean noLocal)
  {
    MemoryQueue queue;

    if (name != null) {
      queue = new MemorySubscriberQueue(session, noLocal);
      queue.setName(getName() + ":sub-" + name);

      _subscriptionList.add(queue);
    }
    else {
      queue = new MemorySubscriberQueue(session, noLocal);
      queue.setName(getName() + ":sub-" + _id++);

      _subscriptionList.add(queue);
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " create-subscriber(" + queue + ")");

    _xmppNode.addQueue(queue);

    return queue;
  }

  @Override
  public void closeSubscriber(AbstractQueue queue)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " close-subscriber(" + queue + ")");
    
    _xmppNode.removeQueue(queue);
    
    _subscriptionList.remove(queue);
  }

  @Override
  public void send(JmsSession session, MessageImpl msg, long timeout)
    throws JMSException
  {
    for (int i = 0; i < _subscriptionList.size(); i++) {
      _subscriptionList.get(i).send(session, msg, timeout);
    }
  }
}

