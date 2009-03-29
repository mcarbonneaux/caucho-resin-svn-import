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

package com.caucho.loader;

import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.util.ArrayList;

/**
 * A jar artifact in the repository
 */
public class ArtifactRepository
{
  private static final L10N L = new L10N(ArtifactRepository.class);

  private static final EnvironmentLocal<ArtifactRepository> _local
    = new EnvironmentLocal<ArtifactRepository>();

  private ArtifactRepository _parent;
  private EnvironmentClassLoader _loader;

  private ArrayList<ArtifactResolver> _resolverList
    = new ArrayList<ArtifactResolver>();

  private ArtifactRepository(EnvironmentClassLoader loader)
  {
    _loader = loader;

    if (loader != null) {
      EnvironmentClassLoader parentLoader
	= Environment.getEnvironmentClassLoader(loader.getParent());

      if (parentLoader != null && parentLoader != loader)
	_parent = create(parentLoader);
    }
  }

  public static ArtifactRepository create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }

  public static ArtifactRepository create(ClassLoader loader)
  {
    synchronized (_local) {
      ArtifactRepository repository = _local.getLevel(loader);

      if (repository == null) {
	EnvironmentClassLoader envLoader
	  = Environment.getEnvironmentClassLoader(loader.getParent());

	repository = new ArtifactRepository(envLoader);

	_local.set(repository);
      }

      return repository;
    }
  }

  public static ArtifactRepository getCurrent()
  {
    return _local.get();
  }

  public void addResolver(ArtifactResolver resolver)
  {
    _resolverList.add(resolver);
  }

  public ArrayList<Artifact> resolve(ArtifactDependency dependency)
  {
    ArrayList<Artifact> artifactList = new ArrayList<Artifact>();

    resolve(artifactList, dependency);

    return artifactList;
  }

  protected void resolve(ArrayList<Artifact> artifactList,
			 ArtifactDependency dependency)
  {
    if (_parent != null)
      _parent.resolve(artifactList, dependency);

    for (ArtifactResolver resolver : _resolverList)
      resolver.resolve(artifactList, dependency);
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[" + _loader + "]");
  }
}
