/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.xtpdoc;

import java.io.PrintWriter;
import java.io.IOException;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

public class Text implements ContentItem {
  private String _text;

  public Text(String text)
  {
    _text = text;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeCharacters(_text);
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.print(LaTeXUtil.escapeForLaTeX(_text));
  }

  public void writeLaTeXEnclosed(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }

  public void writeLaTeXTop(PrintWriter out)
    throws IOException
  {
    writeLaTeX(out);
  }
}
