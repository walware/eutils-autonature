/*=============================================================================#
 # Copyright (c) 2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.eutils.autonature.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;


public interface IProjectConfigurator {
	
	
	byte NOT_CONFIGURABLE= 0;
	byte ALREADY_CONFIGURED= 1;
	byte CONFIGURABLE= 2;
	
	
	byte check(IProject project, IProgressMonitor monitor);
	
	void configure(IProject project, IProgressMonitor monitor);
	
}
