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

package com.caucho.hemp.broker;

import com.caucho.bam.*;
import com.caucho.hemp.*;
import com.caucho.loader.*;
import com.caucho.server.resin.*;
import com.caucho.util.*;
import java.util.*;
import java.util.logging.*;
import java.lang.ref.*;
import java.io.Serializable;


/**
 * Broker
 */
public class HempBrokerManager
{
  private static final Logger log
    = Logger.getLogger(HempBrokerManager.class.getName());
  private static final L10N L = new L10N(HempBrokerManager.class);

  private static EnvironmentLocal<HempBrokerManager> _localBroker
    = new EnvironmentLocal<HempBrokerManager>();
  
  // brokers
  private final HashMap<String,WeakReference<BamBroker>> _brokerMap
    = new HashMap<String,WeakReference<BamBroker>>();

  public HempBrokerManager()
  {
    _localBroker.set(this);
  }

  public static HempBrokerManager getCurrent()
  {
    return _localBroker.get();
  }

  public void addBroker(String name, BamBroker broker)
  {
    synchronized (_brokerMap) {
      _brokerMap.put(name, new WeakReference<BamBroker>(broker));
    }

    if (log.isLoggable(Level.FINER))
      log.finer(this + " add " + broker + " as '" + name + "'");
  }

  public BamBroker removeBroker(String name)
  {
    WeakReference<BamBroker> brokerRef = null;
    
    synchronized (_brokerMap) {
      brokerRef = _brokerMap.remove(name);
    }

    if (brokerRef != null) {
      if (log.isLoggable(Level.FINER))
	log.finer(this + " remove " + name);
      
      return brokerRef.get();
    }
    else
      return null;
  }

  public BamBroker findBroker(String name)
  {
    WeakReference<BamBroker> brokerRef = null;
    
    synchronized (_brokerMap) {
      brokerRef = _brokerMap.get(name);
    }

    if (brokerRef != null)
      return brokerRef.get();
    else
      return null;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
