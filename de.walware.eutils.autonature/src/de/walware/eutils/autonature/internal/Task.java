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
import org.eclipse.core.runtime.SubMonitor;


public abstract class Task {
	
	
	public static final int CONTENT_MATCH= 1;
	
	
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
	
	public boolean isSupported(final byte mode) {
		return true;
	}
	
	/**
	 * Validates if task is required for the specified project.
	 * 
	 * @param project
	 * @param flags TODO
	 * @param m progress monitor
	 * @return
	 * @throws CoreException 
	 */
	public abstract byte check(IProject project,
			int flags, SubMonitor m) throws CoreException;
	
}
