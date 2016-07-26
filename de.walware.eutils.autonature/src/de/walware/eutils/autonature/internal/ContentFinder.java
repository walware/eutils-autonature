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

package de.walware.eutils.autonature.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;


public class ContentFinder implements IResourceVisitor {
	
	
	private final IContainer root;
	private final Set<IPath> ignoredPaths;
	
	private final ConfigManager configManager;
	private final byte mode;
	
	private final List<AutoConfig> configs= new ArrayList<>(); 
	
	private final IProgressMonitor monitor;
	
	
	public ContentFinder(final IContainer root, final Set<IPath> ignoredPaths,
			final ConfigManager configManager, final byte mode,
			final IProgressMonitor monitor) {
		this.root= root;
		this.ignoredPaths= ignoredPaths;
		this.configManager= configManager;
		this.mode= mode;
		this.monitor= monitor;
	}
	
	@Override
	public boolean visit(final IResource resource) throws CoreException {
		if (this.monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		
		if (resource != this.root && this.ignoredPaths != null) {
			for (final IPath ignoredDirectory : this.ignoredPaths) {
				if (ignoredDirectory.equals(resource.getLocation())) {
					return false;
				}
			}
		}
		
		switch (resource.getType()) {
		case IResource.PROJECT:
		case IResource.FOLDER:
			return true;
		case IResource.FILE:
			checkFile((IFile) resource);
			return false;
		default:
			return false;
		}
		
	}
	
	private void checkFile(final IFile file) {
		try {
			final IContentDescription description= file.getContentDescription();
			if (description == null) {
				return;
			}
			final IContentType contentType= description.getContentType();
			if (contentType == null) {
				return;
			}
			final AutoConfig config= this.configManager.getConfig(contentType, this.mode);
			if (config != null) {
				addTasks(config);
			}
		}
		catch (final CoreException e) {
		}
	}
	
	protected void addTasks(final AutoConfig config) {
		if (!this.configs.contains(config)) {
			this.configs.add(config);
		}
	}
	
	public boolean hasTasks() {
		return (!this.configs.isEmpty());
	}
	
	public List<AutoConfig> getConfigs() {
		return this.configs;
	}
	
}
