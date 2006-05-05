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

package javax.script;

import java.io.Reader;

/**
 * A standard imlementation of the ScriptEngine.
 */
abstract public class GenericScriptEngine implements ScriptEngine {
  protected ScriptContext context;

  /**
   * Creates a default script engine.
   */
  public GenericScriptEngine()
  {
    this.context = new GenericScriptContext();
    this.context.setNamespace(createNamespace(),
			      GenericScriptContext.ENGINE_SCOPE);
  }

  /**
   * Creates a default script engine.
   */
  public GenericScriptEngine(Namespace n)
  {
    this.context = new GenericScriptContext();
    this.context.setNamespace(n, GenericScriptContext.ENGINE_SCOPE);
  }
  
  /**
   * Evaluates the script with the given context, returning any value.
   *
   * @param script path to the script
   * @param context the script context
   */
  abstract public Object eval(String script, ScriptContext context)
    throws ScriptException;

  /**
   * Evaluates the script with the given context, returning any value.
   *
   * @param reader reader to the script
   * @param context the script context
   */
  abstract public Object eval(Reader reader, ScriptContext context)
    throws ScriptException;

  /**
   * Evaluates the script with the given context, returning any value.
   *
   * @param reader reader to the script
   * @param namespace the single namespace
   */
  public Object eval(Reader reader, Namespace namespace)
    throws ScriptException
  {
    GenericScriptContext cxt = new GenericScriptContext();

    cxt.setNamespace(namespace, ScriptContext.ENGINE_SCOPE);

    return eval(reader, cxt);
  }

  /**
   * Evaluates the script with the given context, returning any value.
   *
   * @param script the script
   * @param namespace the single namespace
   */
  public Object eval(String script, Namespace namespace)
    throws ScriptException
  {
    GenericScriptContext cxt = new GenericScriptContext();

    getContext().setNamespace(namespace, ScriptContext.ENGINE_SCOPE);

    return eval(script, cxt);
  }

  /**
   * Evaluates the script with no extra context, returning any value.
   *
   * @param script path to the script
   */
  public Object eval(String script)
    throws ScriptException
  {
    return eval(script, getContext());
  }

  /**
   * Evaluates the script with no extra context, returning any value.
   *
   * @param reader reader to the script
   */
  public Object eval(Reader reader)
    throws ScriptException
  {
    return eval(reader, getContext());
  }

  /**
   * Sets an engine scope value.
   */
  public void put(String key, Object value)
  {
    getNamespace(ScriptContext.ENGINE_SCOPE).put(key, value);
  }

  /**
   * Gets an engine scope value.
   */
  public Object get(String key)
  {
    return getNamespace(ScriptContext.ENGINE_SCOPE).get(key);
  }

  /**
   * Returns a scope of named values.
   */
  public Namespace getNamespace(int scope)
  {
    return getContext().getNamespace(scope);
  }

  /**
   * Sets a scope of named values.
   */
  public void setNamespace(Namespace namespace, int scope)
  {
    getContext().setNamespace(namespace, scope);
  }

  /**
   * Returns the default script context.
   */
  public ScriptContext getContext()
  {
    return this.context;
  }

  /**
   * Sets the default script context.
   */
  public void setContext(ScriptContext cxt)
  {
    this.context = cxt;
  }

  /**
   * Returns the factory which owns the script engine.
   */
  abstract public ScriptEngineFactory getFactory();
}

