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

public class Results extends FormattedTextWithAnchors {
  private String _title;
  private String _language = null;

  public Results(Document document)
  {
    super(document);
  }

  public void setTitle(String title)
  {
    _title = title;
  }

  public void setLanguage(String language)
  {
    _language = language;
  }

  public void writeHtml(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeStartElement("div");
    out.writeAttribute("class", "example");

    super.writeHtml(out);

    out.writeEndElement(); // div
  }

  public void writeLaTeX(PrintWriter out)
    throws IOException
  {
    out.println("\\begin{center}");
    out.println("\\begin{Verbatim}[frame=single,fontfamily=courier,");
    out.println("                  fillcolor=\\color{results-gray},");
    out.println("                  fontsize=\\footnotesize,");
    out.println("                  samepage=true]");

    super.writeLaTeX(out);

    out.println();
    out.println("\\end{Verbatim}");

    if (_title != null)
      out.println(LaTeXUtil.escapeForLaTeX(_title));

    out.println("\\end{center}");
  }
}
