/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.expr;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.program.Statement;
import com.caucho.quercus.program.AnalyzeInfo;

import com.caucho.quercus.parser.QuercusParser;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

import com.caucho.util.L10N;

/**
 * Represents a PHP expression.
 */
abstract public class Expr {
  private static final L10N L = new L10N(Expr.class);
  private static final Logger log = Logger.getLogger(Expr.class.getName());

  public static final int COMPILE_ARG_MAX = 5;

  private final Location _location;

  public Expr(Location location)
  {
    _location = location;
  }

  public Expr()
  {
    _location = Location.UNKNOWN;
  }

  /**
   * Returns the location.
   */
  final public Location getLocation()
  {
    return _location;
  }

  /**
   * Returns the filename.
   */
  public String getFileName()
  {
    if (_location != Location.UNKNOWN)
      return _location.getFileName();
    else
      return null;
  }

  /**
   * Returns the filename.
   */
  public int getLine()
  {
    return _location.getLineNumber();
  }

  /**
   * Returns the function name.
   */
  public String getFunctionLocation()
  {
    return "";
  }
  
  /**
   * Returns the location if known.
   */
  public String getLocationLine()
  {
    if (_location != Location.UNKNOWN)
      return _location.getFileName() + ":" + getLine() + ": ";
    else
      return "";
  }

  /**
   * Returns true for a reference.
   */
  public boolean isRef()
  {
    return false;
  }

  /**
   * Returns true for a constant expression.
   */
  public boolean isConstant()
  {
    return isLiteral();
  }

  /**
   * Returns true for a literal expression.
   */
  public boolean isLiteral()
  {
    return false;
  }

  /**
   * Returns true if a static true value.
   */
  public boolean isTrue()
  {
    return false;
  }

  /**
   * Returns true for an expression that can be read (only $a[] uses this)
   */
  public boolean canRead()
  {
    return true;
  }

  public Expr createAssign(QuercusParser parser, Expr value)
    throws IOException
  {
    String msg = (L.l("{0} is an invalid left-hand side of an assignment.",
		      this));

    if (parser != null)
      throw parser.error(msg);
    else
      throw new IOException(msg);
  }

  /**
   * Mark as an assignment for a list()
   */
  public void assign(QuercusParser parser)
    throws IOException
  {
    String msg = L.l("{0} is an invalid left-hand side of an assignment.",
		     this);

    if (parser != null)
      throw parser.error(msg);
    else
      throw new IOException(msg);
  }

  public Expr createAssignRef(QuercusParser parser, Expr value)
    throws IOException
  {
    // XXX: need real exception
    String msg = L.l("{0} is an invalid left-hand side of an assignment.",
		     this);

    if (parser != null)
      throw parser.error(msg);
    else
      throw new IOException(msg);
  }

  /**
   * Creates a reference.
   * @param location
   */
  public Expr createRef(Location location)
    throws IOException
  {
    return this;
  }

  public Expr createDeref()
    throws IOException
  {
    return this;
  }

  /**
   * Creates a assignment
   * @param location
   */
  public Expr createCopy(Location location)
  {
    return this;
  }

  /**
   * Copy for things like $a .= "test";
   * @param location
   */
  public Expr copy(Location location)
  {
    return this;
  }

  /**
   * Creates a field ref
   */
  public Expr createFieldGet(Location location, String name)
  {
    return new FieldGetExpr(location, this, name);
  }

  /**
   * Creates a field ref
   */
  public Expr createFieldGet(Location location, Expr name)
  {
    return new FieldVarGetExpr(location, this, name);
  }

  /**
   * Creates a assignment
   */
  public Statement createUnset(Location location)
    throws IOException
  {
    throw new IOException(L.l("{0} is an illegal value to unset",
			      this));
  }

  /**
   * Creates an isset expression
   */
  public Expr createIsset()
    throws IOException
  {
    throw new IOException(L.l("{0} is an illegal value to isset",
			      this));
  }

  /**
   * Returns true if the expression evaluates to a boolean.
   */
  public boolean isBoolean()
  {
    return false;
  }

  /**
   * Returns true if the expression evaluates to a long.
   */
  public boolean isLong()
  {
    return false;
  }

  /**
   * Returns true if the expression evaluates to a double.
   */
  public boolean isDouble()
  {
    return false;
  }

  /**
   * Returns true if the expression evaluates to a number.
   */
  public boolean isNumber()
  {
    return isLong() || isDouble();
  }

  /**
   * Returns true if the expression evaluates to a string.
   */
  public boolean isString()
  {
    return false;
  }

  /**
   * Evaluates the expression as a constant.
   *
   * @return the expression value.
   */
  public Value evalConstant()
    throws IOException
  {
    throw new IOException(L.l("{0} is not a constant expression", this));
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  abstract public Value eval(Env env);

  /**
   * Evaluates the expression, creating an array for unassigned values.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
  {
    return eval(env);
  }

  /**
   * Evaluates the expression, creating an object for unassigned values.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
  {
    return eval(env);
  }

  /**
   * Evaluates the expression as a reference.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalRef(Env env)
  {
    return eval(env);
  }

  /**
   * Evaluates the expression as a copy
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
  {
    return eval(env);
  }

  /**
   * Evaluates the expression as a function argument where it is unknown
   * if the value is a reference.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArg(Env env)
  {
    return eval(env);
  }

  /**
   * Needed to handle list.
   */
  public void evalAssign(Env env, Value value)
  {
    throw new RuntimeException(L.l("{0} is an invalid left-hand side of an assignment.",
				   this));
  }

  /**
   * Evaluates the expression as a string
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public String evalString(Env env)
  {
    return eval(env).toString();
  }

  /**
   * Evaluates the expression as a string
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public char evalChar(Env env)
  {
    return eval(env).toChar();
  }

  /**
   * Evaluates the expression as a boolean.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public boolean evalBoolean(Env env)
  {
    return eval(env).toBoolean();
  }

  /**
   * Evaluates the expression as a long
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public long evalLong(Env env)
  {
    return eval(env).toLong();
  }

  /**
   * Evaluates the expression as a double
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public double evalDouble(Env env)
  {
    return eval(env).toDouble();
  }

  /**
   * Prints to the output as an echo.
   */
  public void print(Env env)
    throws IOException
  {
    eval(env).print(env);
  }

  //
  // Java code generation
  //

  /**
   * Analyze the expression
   */
  public void analyze(AnalyzeInfo info)
  {
    System.out.println("ANALYZE: " + getClass().getName());
  }

  /**
   * Analyze the expression
   */
  public void analyzeAssign(AnalyzeInfo info)
  {
    analyze(info);
  }

  /**
   * Analyze as modified.
   */
  public void analyzeSetModified(AnalyzeInfo info)
  {
  }

  /**
   * Analyze as reference
   */
  public void analyzeSetReference(AnalyzeInfo info)
  {
  }

  /**
   * Returns the variables state.
   *
   * @param var the variables to test
   * @param owner the owning expression
   */
  public VarState getVarState(VarExpr var, VarExpr owner)
  {
    return VarState.UNKNOWN;
  }

  /**
   * Returns the variables state.
   *
   * @param leftState the variables to test
   * @param rightState the owning expression
   */
  protected VarState combineBinaryVarState(VarState leftState,
					   VarState rightState)
  {
    if (leftState == VarState.UNSET || leftState == VarState.UNDEFINED)
      return leftState;

    if (rightState == VarState.UNSET || rightState == VarState.UNDEFINED)
      return rightState;

    if (leftState == VarState.VALID || rightState == VarState.VALID)
      return VarState.VALID;
    else
      return VarState.UNKNOWN;
  }

  /**
   * Returns true if the variable is ever assigned.
   *
   * @param var the variable to test
   */
  public boolean isVarAssigned(VarExpr var)
  {
    return false;
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    String var = out.addExpr(this);

    out.print(var + ".eval(env)");
  }

  /**
   * Generates code for a function arg.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArg(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code for a function arg.
   *
   * @param out the writer to the Java source code.
   */
  public void generateValue(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code for a reference.
   *
   * @param out the writer to the Java source code.
   */
  public void generateRef(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateCopy(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code to recreate the expression, creating an array
   * for an unset value.
   *
   * @param out the writer to the Java source code.
   */
  public void generateArray(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code to recreate the expression, creating an object
   * for an unset value.
   *
   * @param out the writer to the Java source code.
   */
  public void generateObject(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateTop(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code for a return value
   *
   * @param out the writer to the Java source code.
   */
  public void generateReturn(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateStatement(PhpWriter out)
    throws IOException
  {
    generateTop(out);
    out.println(";");
  }

  /**
   * Generates code to evaluate a boolean directly
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toBoolean()");
  }

  /**
   * Generates code to evaluate a string directly
   *
   * @param out the writer to the Java source code.
   */
  public void generateString(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toString()");
  }

  /**
   * Generates code to append to a string builder.
   *
   * @param out the writer to the Java source code.
   */
  public void generateAppend(PhpWriter out)
    throws IOException
  {
    generate(out);
  }

  /**
   * Generates code to evaluate a string directly
   *
   * @param out the writer to the Java source code.
   */
  public void generateChar(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toChar()");
  }

  /**
   * Generates code to evaluate the expression directly
   *
   * @param out the writer to the Java source code.
   */
  public void generateLong(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toLong()");
  }

  /**
   * Generates code to evaluate the expression directly
   *
   * @param out the writer to the Java source code.
   */
  public void generateDouble(PhpWriter out)
    throws IOException
  {
    generate(out);
    out.print(".toDouble()");
  }

  /**
   * Generates code to get the out.
   */
  public void generateGetOut(PhpWriter out)
    throws IOException
  {
    // quercus/1l07
    // out.print("_quercus_out");

    // out.print("env.getOut()");
    out.print("env");
  }

  /**
   * Generates code to print the expression directly
   *
   * @param out the writer to the Java source code.
   */
  public void generatePrint(PhpWriter out)
    throws IOException
  {
    if (isLong()) {
      out.print("env.print(");
      generateLong(out);
      out.print(")");
    }
    else if (isDouble()) {
      out.print("env.print(");
      generateDouble(out);
      out.print(")");
    }
    else if (isString()) {
      out.print("env.print(");
      generateString(out);
      out.print(")");
    }
    else {
      generate(out);
      out.print(".print(env)");
    }
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateExpr(PhpWriter out)
    throws IOException
  {
    // XXX: remove when done
    System.out.println("Generate: " + getClass().getName());

    out.print("com.caucho.quercus.expr.NullLiteralExpr.NULL");
  }

  public String toString()
  {
    return "Expr[]";
  }
}

