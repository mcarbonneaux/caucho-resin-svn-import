/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.type;

import java.io.IOException;

import java.sql.Types;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

import com.caucho.amber.AmberManager;

/**
 * Represents a primitive Java byte type
 */
public class PrimitiveByteType extends PrimitiveType {
  private static final L10N L = new L10N(PrimitiveByteType.class);

  private static final PrimitiveByteType BYTE_TYPE =
    new PrimitiveByteType();

  private PrimitiveByteType()
  {
  }

  /**
   * Returns the byte type.
   */
  public static PrimitiveByteType create()
  {
    return BYTE_TYPE;
  }

  /**
   * Returns the type name.
   */
  public String getName()
  {
    return "byte";
  }

  /**
   * Returns the foreign key type.
   */
  public Type getForeignType()
  {
    return ByteType.create();
  }

  /**
   * Generates the type for the table.
   */
  public String generateCreateTableSQL(AmberManager manager, int length, int precision, int scale)
  {
    return manager.getCreateTableSQL(Types.TINYINT, length, precision, scale);
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoad(JavaWriter out, String rs,
			  String indexVar, int index)
    throws IOException
  {
    out.print(rs + ".getByte(" + indexVar + " + " + index + ")");

    return index + 1;
  }

  /**
   * Generates a string to set the property.
   */
  public void generateSet(JavaWriter out, String pstmt,
			  String index, String value)
    throws IOException
  {
    out.println(pstmt + ".setByte(" + index + "++, " + value + ");");
  }

  /**
   * Generates a string to set the property.
   */
  public void generateSetNull(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    out.println(pstmt + ".setNull(" + index + "++, java.sql.Types.TINYINT);");
  }

  /**
   * Converts to an object.
   */
  public String toObject(String value)
  {
    return "new Byte(" + value + ")";
  }
  
  /**
   * Converts the value.
   */
  public String generateCastFromObject(String value)
  {
    return "((Number) " + value + ").byteValue()";
  }
}
