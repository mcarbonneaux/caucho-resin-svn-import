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

package com.caucho.xmpp.pubsub;

import com.caucho.xmpp.pubsub.PubSubQuery;
import java.util.*;

/**
 * Subscribe query
 */
public class PubSubSubscribe extends PubSubQuery {
  private final String address;
  private final String node;

  public PubSubSubscribe()
  {
    this.address = null;
    this.node = null;
  }

  public PubSubSubscribe(String address)
  {
    this.address = address;
    this.node = null;
  }

  public PubSubSubscribe(String address, String node)
  {
    this.address = address;
    this.node = node;
  }

  public String getAddress()
  {
    return this.address;
  }

  public String getNode()
  {
    return this.node;
  }

  @Override
  public String toString()
  {
    if (this.node != null)
      return (getClass().getSimpleName() + "[" + this.address
              + ",node=" + this.node + "]");
    else
      return getClass().getSimpleName() + "[" + this.address + "]";
  }
}