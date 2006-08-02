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

import java.util.ArrayList;

public class NamedText implements ContentItem {
  private String _name;
  private ContentItem _text;

  public NamedText(String name, ContentItem text)
  {
    _name = name;
    _text = text;
  }

  public void writeHtml(PrintWriter writer)
    throws IOException
  {
    writer.print("<b>" + _name + ":</b> ");

    _text.writeHtml(writer);
  }

  public void writeLaTeX(PrintWriter writer)
    throws IOException
  {
    writer.print("\\textbf{" + LaTeXUtil.escapeForLaTeX(_name) + ":} ");

    _text.writeLaTeX(writer);
  }
}
