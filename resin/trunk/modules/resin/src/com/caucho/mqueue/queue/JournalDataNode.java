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

package com.caucho.mqueue.queue;

import java.io.IOException;

import com.caucho.db.block.BlockStore;

/**
 * A chunk of journal data, part of a queue message.
 */
final class JournalDataNode
{
  private final BlockStore _store;
  
  private final long _blockAddress;
  private final int _offset;
  private final int _length;
  
  private JournalDataNode _next;
  
  JournalDataNode(BlockStore store, long blockAddress, int offset, int length)
  {
    _store = store;
    
    _blockAddress = blockAddress;
    _offset = offset;
    _length = length;
  }
  
  final int getLength()
  {
    return _length;
  }
  
  final void read(int nodeOffset, byte []buffer, int offset, int length)
    throws IOException
  {
    _store.readBlock(_blockAddress, nodeOffset + _offset,
                     buffer, offset, length);
  }
  
  final JournalDataNode getNext()
  {
    return _next;
  }
  
  final void setNext(JournalDataNode next)
  {
    _next = next;
  }
}