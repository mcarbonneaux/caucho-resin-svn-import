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

import com.caucho.hmpp.HmppConnection;
import com.caucho.config.*;
import com.caucho.hmpp.*;
import com.caucho.hmpp.disco.*;
import com.caucho.hmpp.spi.*;
import com.caucho.util.*;

import java.io.Serializable;
import java.util.*;
import java.util.logging.*;
import javax.annotation.*;
import javax.webbeans.*;

/**
 * Configuration for a service
 */
public class GenericResource extends AbstractHmppResource
{
  private static final L10N L = new L10N(GenericResource.class);
  private static final Logger log
    = Logger.getLogger(GenericResource.class.getName());
  
  private @In HmppBroker _broker;
  
  private HmppConnection _conn;
  private HmppStream _toBroker;
  
  public void setName(String name)
  {
    setJid(name);
  }
  
  protected HmppConnection getConnection()
  {
    return _conn;
  }

  public HmppStream getToBroker()
  {
    return _toBroker;
  }

  public HmppStream getStream()
  {
    return _toBroker;
  }

  @PostConstruct
  protected void init()
  {
    if (getJid() == null)
      throw new ConfigException(L.l("{0} requires a jid",
				    getClass().getSimpleName()));

    _conn = _broker.registerResource(getJid(), this);

    if (log.isLoggable(Level.FINE))
      log.fine(this + " init");

    _toBroker = _conn.getStream();
  }

  //
  // queries
  //

  @Override
  public boolean sendQueryGet(long id, String to, String from,
			      Serializable value)
  {
    if (value instanceof DiscoInfoQuery) {
      _toBroker.sendQueryResult(id, from, to,
				new DiscoInfoQuery(getDiscoIdentity(),
						   getDiscoFeatures()));

      return true;
    }

    Serializable result = doQueryGet(to, from, value);

    if (result != null) {
      _toBroker.sendQueryResult(id, from, to, result);
      return true;
    }

    return false;
  }

  protected Serializable doQueryGet(String to, String from, Serializable value)
  {
    return null;
  }

  protected Serializable doQuerySet(String to, String from, Serializable value)
  {
    return null;
  }

  /**
   * Returns the disco identity of the resource
   */
  protected DiscoIdentity []getDiscoIdentity()
  {
    return new DiscoIdentity[] {
      new DiscoIdentity(getDiscoCategory(), getDiscoType()),
    };
  }

  /**
   * Returns the disco features of the resource
   */
  protected DiscoFeature []getDiscoFeatures()
  {
    ArrayList<String> featureNames = new ArrayList<String>();

    getDiscoFeatureNames(featureNames);

    DiscoFeature []features = new DiscoFeature[featureNames.size()];

    for (int i = 0; i < featureNames.size(); i++) {
      features[i] = new DiscoFeature(featureNames.get(i));
    }

    return features;
  }

  protected String getDiscoCategory()
  {
    return "x-application";
  }

  protected String getDiscoType()
  {
    return getClass().getSimpleName();
  }

  protected void getDiscoFeatureNames(ArrayList<String> featureNames)
  {
    featureNames.add(DiscoInfoQuery.FEATURE);
  }

  @PreDestroy
  protected void destroy()
  {
    _conn.close();

    if (log.isLoggable(Level.FINE))
      log.fine(this + " destroy");
  }
}
