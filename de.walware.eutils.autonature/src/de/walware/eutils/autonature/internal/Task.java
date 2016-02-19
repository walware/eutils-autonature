/*=============================================================================#
 # Copyright (c) 2014-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.eutils.autonature.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;


public abstract class Task {
	
	
	/**
	 * Returns the label of the task (appears in UI).
	 * 
	 * @return the label
	 */
	public abstract String getLabel();
	
	/**
	 * Returns if all requirements (like dependencies, project natures, ...) are available.
	 * 
	 * @return if available
	 */
	public abstract boolean isAvailable();
	
	/**
	 * Validates if task is required for the specified project.
	 * 
	 * @param project
	 * @return
	 * @throws CoreException 
	 */
	public abstract boolean isRequired(IProject project) throws CoreException;
	
	
}
