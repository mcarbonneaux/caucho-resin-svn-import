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

package com.caucho.remote;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.*;

import com.caucho.remote.annotation.ProxyType;
import com.caucho.remote.burlap.BurlapProtocolProxyFactory;

/**
 * The @BurlapClient registers a client with burlap
 *
 * <code><pre>
 * &lt;web-app xmlns="http://caucho.com/ns/resin"
 *        xmlns:resin="urn:java:com.caucho.resin">
 *
 *    &lt;mypkg:MyService xmlns:mypkg="urn:java:com.foo.mypkg">
 *      &lt;mypkg:BurlapClient url="http://localhost:8080/test"/>
 *    &lt;/mypkg:MyService>
 *
 * &lt;/web-app>
 * </pre></code>
 */

@Documented
@Target({TYPE})
@Retention(RUNTIME)
@ProxyType(defaultFactory=BurlapProtocolProxyFactory.class)
public @interface BurlapClient {
  public String url();
}
