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

package com.caucho.amp.skeleton;

import java.lang.reflect.Proxy;

import com.caucho.amp.actor.ActorContextImpl;
import com.caucho.amp.actor.AmpActor;
import com.caucho.amp.router.AmpBroker;

/**
 * Creates MPC skeletons and stubs.
 */
public class AmpReflectionSkeletonFactory implements AmpSkeletonFactory
{
  @Override
  public AmpActor createSkeleton(Object bean, 
                                 String address,
                                 AmpBroker broker)
  {
    return new AmpReflectionSkeleton(bean, address, broker);
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public <T> T createStub(Class<T> api,
                          AmpBroker router,
                          ActorContextImpl sender,
                          String to,
                          String from)
  {
    AmpReflectionHandler handler
      = new AmpReflectionHandler(api, router, sender, to, from);
    
    return (T) Proxy.newProxyInstance(api.getClassLoader(),
                                      new Class<?>[] { api },
                                      handler);
  }
}