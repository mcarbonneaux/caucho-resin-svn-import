/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.admin;

import java.util.concurrent.atomic.AtomicLong;
import com.caucho.util.Alarm;

public final class ActiveProbe extends Probe implements ActiveSample {
  private final Object _lock = new Object();

  // sample data
  private final AtomicLong _activeCount = new AtomicLong();
  private final AtomicLong _activeCountMax = new AtomicLong();
  private final AtomicLong _totalCount = new AtomicLong();

  private long _lastTotal;

  public ActiveProbe(String name)
  {
    super(name);
  }

  public final void start()
  {
    long activeCount = _activeCount.incrementAndGet();
    _totalCount.incrementAndGet();

    long max;

    while ((max = _activeCountMax.get()) < activeCount
           && ! _activeCountMax.compareAndSet(max, activeCount)) {
    }
  }

  public final void end()
  {
    _activeCount.decrementAndGet();
  }

  public Probe createMax(String name)
  {
    return new MaxProbe(name);
  }

  public Probe createTotal(String name)
  {
    return new TotalProbe(name);
  }

  /**
   * Sample the active count
   */
  public final double sampleActive()
  {
    return _activeCount.get();
  }

  /**
   * Sample the active count
   */
  public final double sampleMax()
  {
    return _activeCountMax.getAndSet(_activeCount.get());
  }

  /**
   * Sample the total count
   */
  public final double sample()
  {
    long totalCount = _totalCount.get();
    long lastTotal = _lastTotal;
    _lastTotal = totalCount;

    return totalCount - lastTotal;
  }

  class MaxProbe extends Probe {
    MaxProbe(String name)
    {
      super(name);
    }

    public double sample()
    {
      return sampleMax();
    }
  }

  class TotalProbe extends Probe {
    TotalProbe(String name)
    {
      super(name);
    }

    public double sample()
    {
      return sample();
    }
  }
}
