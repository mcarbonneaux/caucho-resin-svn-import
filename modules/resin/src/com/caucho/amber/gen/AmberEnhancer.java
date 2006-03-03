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

package com.caucho.amber.gen;

import java.lang.reflect.Method;

import java.io.IOException;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.bytecode.JavaClass;

import com.caucho.util.L10N;

import com.caucho.vfs.Path;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Vfs;

import com.caucho.log.Log;

import com.caucho.loader.Environment;

import com.caucho.loader.enhancer.ClassEnhancer;

import com.caucho.config.ConfigException;

import com.caucho.java.JavaCompiler;

import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.java.gen.DependencyComponent;
import com.caucho.java.gen.GenClass;

import com.caucho.amber.AmberRuntimeException;

import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.SubEntityType;

import com.caucho.amber.field.AmberField;

import com.caucho.amber.manager.AmberContainer;

import com.caucho.bytecode.ConstantPool;
import com.caucho.bytecode.FieldRefConstant;
import com.caucho.bytecode.MethodRefConstant;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JavaMethod;
import com.caucho.bytecode.CodeVisitor;
import com.caucho.bytecode.Analyzer;

/**
 * Enhancing the java objects for Amber mapping.
 */
public class AmberEnhancer implements AmberGenerator, ClassEnhancer {
  private static final L10N L = new L10N(AmberEnhancer.class);
  private static final Logger log = Log.open(AmberEnhancer.class);

  private Path _configDirectory;
  private boolean _useHibernateFiles;

  private AmberContainer _amberContainer;

  private ArrayList<String> _pendingClassNames = new ArrayList<String>();

  public AmberEnhancer(AmberContainer amberContainer)
  {
    _amberContainer = amberContainer;
  }
  
  /**
   * Sets the config directory.
   */
  public void setConfigDirectory(Path dir)
  {
    _configDirectory = dir;
  }

  /**
   * Initialize the enhancer.
   */
  public void init()
    throws Exception
  {
  }

  /**
   * Checks to see if the preloaded class is modified.
   */
  protected boolean isModified(Class preloadedClass)
  {
    try {
      Method init = preloadedClass.getMethod("_caucho_init",
					     new Class[] { Path.class });


      if (_configDirectory != null)
	init.invoke(null, new Object[] { _configDirectory });
      else
	init.invoke(null, new Object[] { Vfs.lookup() });
      
      Method isModified = preloadedClass.getMethod("_caucho_is_modified",
						   new Class[0]);

      Object value = isModified.invoke(null, new Object[0]);

      if (Boolean.FALSE.equals(value)) {
	loadEntityType(preloadedClass, preloadedClass.getClassLoader());
	return false;
      }
      else
	return true;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);

      return true;
    }
  }

  /**
   * Returns true if the class should be enhanced.
   */
  public boolean shouldEnhance(String className)
  {
    int p = className.lastIndexOf('-');

    if (p > 0)
      className = className.substring(0, p);
    
    p = className.lastIndexOf('$');

    if (p > 0)
      className = className.substring(0, p);
    
    EntityType type = _amberContainer.getEntity(className);

    if (type != null && type.isEnhanced())
      return true;

    return false;
      /*
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      thread.setContextClassLoader(getRawLoader());

      Class baseClass = Class.forName(className, false, getRawLoader());

      type = loadEntityType(baseClass, getRawLoader());
    } catch (ClassNotFoundException e) {
      return false;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    if (type == null)
      return false;

    return className.equals(type.getName()) || type.isFieldAccess();
      */
  }

  /**
   * Returns true if the class should be enhanced.
   */
  private EntityType loadEntityType(Class cl, ClassLoader loader)
  {
    EntityType parentType = null;
    
    for (; cl != null; cl = cl.getSuperclass()) {
      java.net.URL url;

      String className = cl.getName();
      
      EntityType type = _amberContainer.getEntity(className);

      if (parentType == null)
	parentType = type;

      if (type != null && ! type.startConfigure())
	return type;

      type = loadEntityTypeImpl(cl, loader);

      if (type != null && ! type.startConfigure())
	return type;
    }

    return parentType;
  }

  protected EntityType loadEntityTypeImpl(Class cl, ClassLoader rawLoader)
  {
    if (! _useHibernateFiles)
      return null;
    
    String className = cl.getName();
    
    String xmlName = className.replace('.', '/') + ".hbm.xml";
    
    java.net.URL url = rawLoader.getResource(xmlName);

    if (url == null)
      return null;

    Path path = Vfs.lookup(url.toString());

    try {
      parseHibernateMapping(path);

      EntityType type = _amberContainer.getEntity(className);
      type.setInstanceClassName(className + "__ResinExt");
      type.setEnhanced(true);

      return type;
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Enhances the class.
   */
  public void preEnhance(JavaClass baseClass)
    throws Exception
  {
    EntityType type = _amberContainer.getEntity(baseClass.getName());

    if (type instanceof SubEntityType) {
      SubEntityType subType = (SubEntityType) type;

      String parentClass = subType.getParentType().getInstanceClassName();
      baseClass.setSuperClass(parentClass.replace('.', '/'));
    }
  }
  
  /**
   * Enhances the class.
   */
  public void enhance(GenClass genClass,
		      JClass baseClass,
		      String extClassName)
    throws Exception
  {
    String className = baseClass.getName();
    
    EntityType type = _amberContainer.getEntity(className);

    // Type can be null for subclasses and inner classes that need fixups
    if (type != null) {
      log.info("Amber enhancing class " + className);

      // XXX: _amberContainerenceUnitenceUnit.configure();

      type.init();

      genClass.addInterfaceName("com.caucho.amber.entity.Entity");

      EntityComponent entity = new EntityComponent();

      entity.setEntityType(type);
      entity.setBaseClassName(baseClass.getName());
      entity.setExtClassName(extClassName);

      genClass.addComponent(entity);

      DependencyComponent dependency = genClass.addDependencyComponent();
      dependency.addDependencyList(type.getDependencies());
    
      //_amberContainerenceUnitenceUnit.generate();
      // generate(type);

      // compile();
    
      // XXX: _amberContainerenceUnitenceUnit.initEntityHomes();
    }
  }
  
  /**
   * Generates the type.
   */
  public void generate(EntityType type)
    throws Exception
  {
    JavaClassGenerator javaGen = new JavaClassGenerator();

    String extClassName = type.getBeanClass().getName() + "__ResinExt";
    type.setInstanceClassName(extClassName);
    type.setEnhanced(true);
    
    _pendingClassNames.add(type.getInstanceClassName());
    
    generateJava(javaGen, type);
  }
  
  /**
   * Generates the type.
   */
  public void generateJava(JavaClassGenerator javaGen, EntityType type)
    throws Exception
  {
    if (type.isGenerated())
      return;

    type.setGenerated(true);
    
    GenClass javaClass = new GenClass(type.getInstanceClassName());

    javaClass.setSuperClassName(type.getBeanClass().getName());

    javaClass.addInterfaceName("com.caucho.amber.entity.Entity");

    type.setEnhanced(true);

    EntityComponent entity = new EntityComponent();

    entity.setEntityType(type);
    entity.setBaseClassName(type.getBeanClass().getName());

    //String extClassName = gen.getBaseClassName() + "__ResinExt";
    // type.setInstanceClassName(extClassName);
    
    entity.setExtClassName(type.getInstanceClassName());

    javaClass.addComponent(entity);

    javaGen.generate(javaClass);
    
    // _pendingClassNames.add(extClassName);
  }

  /**
   * Compiles the pending classes.
   */
  public void compile()
    throws Exception
  {
    if (_pendingClassNames.size() == 0)
      return;

    ArrayList<String> classNames = new ArrayList<String>(_pendingClassNames);
    _pendingClassNames.clear();
    
    String []javaFiles = new String[classNames.size()];

    for (int i = 0; i < classNames.size(); i++) {
      String name = classNames.get(i);

      name = name.replace('.', '/') + ".java";

      javaFiles[i] = name;
    }
    
    EntityGenerator gen = new EntityGenerator();
    gen.setSearchPath(_configDirectory);
    // XXX:
    // gen.setClassDir(getPath());

    JavaCompiler compiler = gen.getCompiler();

    compiler.compileBatch(javaFiles);

    for (int i = 0; i < classNames.size(); i++) {
      String extClassName = classNames.get(i);
      int tail = extClassName.length() - "__ResinExt".length();
      
      String baseClassName = extClassName.substring(0, tail);

      // fixup(baseClassName, extClassName);
    }
  }
  
  /**
   * Enhances the class.
   */
  public void postEnhance(JavaClass baseClass)
    throws Exception
  {
    String className = baseClass.getThisClass();
    
    ArrayList<FieldMap> fieldMaps = new ArrayList<FieldMap>();

    JClass thisClass = _amberContainer.getJClassLoader().forName(className.replace('/', '.'));

    for (; thisClass != null; thisClass = thisClass.getSuperClass()) {
      EntityType type = _amberContainer.getEntity(thisClass.getName());

      if (type == null || ! type.isFieldAccess())
	continue;

      for (AmberField field : type.getId().getKeys()) {
	fieldMaps.add(new FieldMap(baseClass, field.getName()));
      }
    
      for (AmberField field : type.getFields()) {
	fieldMaps.add(new FieldMap(baseClass, field.getName()));
      }
    }

    if (fieldMaps.size() == 0)
      return;

    FieldFixupAnalyzer analyzer = new FieldFixupAnalyzer(fieldMaps);

    for (JavaMethod javaMethod : baseClass.getMethodList()) {
      CodeVisitor visitor = new CodeVisitor(baseClass, javaMethod.getCode());

      visitor.analyze(analyzer, true);
    }
  }
      
  /**
   * Parses the configuration file.
   */
  public void configure(EntityType type)
    throws ConfigException, IOException
  {
    String className = type.getName();

    String xmlName = className.replace('.', '/') + ".hbm.xml";

    // XXX: stub
    // java.net.URL url = getRawLoader().getResource(xmlName);
    java.net.URL url = null;

    if (url == null)
      return;

    Path path = Vfs.lookup(url.toString());

    if (type.getInstanceClassName() == null) {
      type.setInstanceClassName(className + "__ResinExt");
      type.setEnhanced(true);
    }
    
    try {
      parseHibernateMapping(path);
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Parses the configuration file.
   */
  public void parseHibernateMapping(Path path)
    throws ConfigException, IOException
  {
    Environment.addDependency(new Depend(path));
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      // XXX:
      // thread.setContextClassLoader(getRawLoader());

      // HibernateParser.parse(_amberContainerenceUnitenceUnit, path);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  static class FieldMap {
    private int _fieldRef = -1;
    private int _getterRef;
    private int _setterRef;

    FieldMap(com.caucho.bytecode.JavaClass baseClass,
	     String fieldName)
    {
      ConstantPool pool = baseClass.getConstantPool();

      FieldRefConstant fieldRef = pool.getFieldRef(fieldName);

      if (fieldRef == null)
	return;

      _fieldRef = fieldRef.getIndex();

      MethodRefConstant methodRef;
      
      String getterName = "__caucho_get_" + fieldName;

      methodRef = pool.addMethodRef(baseClass.getThisClass(),
				    getterName,
				    "()" + fieldRef.getType());
      
      _getterRef = methodRef.getIndex();
      
      String setterName = "__caucho_set_" + fieldName;

      methodRef = pool.addMethodRef(baseClass.getThisClass(),
				    setterName,
				    "(" + fieldRef.getType() + ")V");
      
      _setterRef = methodRef.getIndex();
    }

    int getFieldRef()
    {
      return _fieldRef;
    }

    int getGetterRef()
    {
      return _getterRef;
    }

    int getSetterRef()
    {
      return _setterRef;
    }
  }

  static class FieldFixupAnalyzer extends Analyzer {
    private ArrayList<FieldMap> _fieldMap;

    FieldFixupAnalyzer(ArrayList<FieldMap> fieldMap)
    {
      _fieldMap = fieldMap;
    }

    int getGetter(int fieldRef)
    {
      for (int i = _fieldMap.size() - 1; i >= 0; i--) {
	FieldMap fieldMap = _fieldMap.get(i);

	if (fieldMap.getFieldRef() == fieldRef)
	  return fieldMap.getGetterRef();
      }

      return -1;
    }

    public void analyze(CodeVisitor visitor)
    {
      switch (visitor.getOpcode()) {
      case CodeVisitor.GETFIELD:
	int getter = getGetter(visitor.getShortArg());

	if (getter > 0) {
	  visitor.setByteArg(0, CodeVisitor.INVOKEVIRTUAL);
	  visitor.setShortArg(1, getter);
	}
	break;
      case CodeVisitor.PUTFIELD:
	int setter = getSetter(visitor.getShortArg());

	if (setter > 0) {
	  visitor.setByteArg(0, CodeVisitor.INVOKEVIRTUAL);
	  visitor.setShortArg(1, setter);
	}
	break;
      }
    }

    int getSetter(int fieldRef)
    {
      for (int i = _fieldMap.size() - 1; i >= 0; i--) {
	FieldMap fieldMap = _fieldMap.get(i);

	if (fieldMap.getFieldRef() == fieldRef)
	  return fieldMap.getSetterRef();
      }

      return -1;
    }
  }
}
