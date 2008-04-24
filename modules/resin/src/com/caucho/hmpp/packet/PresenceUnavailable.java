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

package com.caucho.hmpp.packet;

import com.caucho.hmpp.HmppStream;
import java.io.Serializable;

/**
 * Announces presence unavailability
 */
public class PresenceUnavailable extends Presence {
  /**
   * zero-arg constructor for Hessian
   */
  private PresenceUnavailable()
  {
  }

  /**
   * An undirected presence unavailable announcement to the server.
   *
   * @param data a collection of presence data
   */
  public PresenceUnavailable(Serializable []data)
  {
    super(data);
  }

  /**
   * A directed presence unavailable announcement to another client
   *
   * @param to the target client
   * @param data a collection of presence data
   */
  public PresenceUnavailable(String to, Serializable []data)
  {
    super(to, data);
  }

  /**
   * A directed presence unavailable announcement to another client
   *
   * @param to the target client
   * @param from the source
   * @param data a collection of presence data
   */
  public PresenceUnavailable(String to, String from, Serializable []data)
  {
    super(to, from, data);
  }

  /**
   * SPI method to dispatch the packet to the proper handler
   */
  @Override
  public void dispatch(HmppStream handler, HmppStream toSource)
  {
    handler.sendPresenceUnavailable(getTo(), getFrom(), getData());
  }
}
