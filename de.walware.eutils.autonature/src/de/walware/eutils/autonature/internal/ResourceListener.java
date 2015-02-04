/*=============================================================================#
 # Copyright (c) 2014-2015 Stephan Wahlbrink (WalWare.de) and others.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.osgi.util.NLS;


public class ResourceListener implements IResourceChangeListener, IResourceDeltaVisitor {
	
	
	private final ConfigManager configManager;
	
	private final Map<IProject, List<AutoConfig>> projectConfigs;
	
	private final AtomicInteger counter= new AtomicInteger();
	
	private final TaskProcessor taskProcessor= new TaskProcessor();
	
	
	public ResourceListener(final ConfigManager configManager) {
		this.configManager= configManager;
		this.projectConfigs= new HashMap<>();
	}
	
	
	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.POST_CHANGE
				&& this.configManager.hasActiveConfigs() ) {
			this.counter.incrementAndGet();
			try {
				event.getDelta().accept(this);
			}
			catch (final CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally {
				if (this.counter.decrementAndGet() == 0) {
					scheduleTasks();
				}
			}
		}
	}
	
	@Override
	public boolean visit(final IResourceDelta delta) throws CoreException {
		IResource resource;
		switch (delta.getKind()) {
		case IResourceDelta.ADDED:
		case IResourceDelta.CHANGED:
			resource= delta.getResource();
			if (resource.getType() == IResource.FILE) {
				checkFile((IFile) resource);
			}
			return true;
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
			final AutoConfig config= this.configManager.getConfig(contentType);
			if (config != null) {
				addTasks(file.getProject(), config);
			}
		}
		catch (final CoreException e) {
		}
	}
	
	private synchronized void addTasks(final IProject project, final AutoConfig config) {
		List<AutoConfig> configs= this.projectConfigs.get(project);
		if (configs == null) {
			configs= new ArrayList<>(8);
			this.projectConfigs.put(project, configs);
		}
		if (!configs.contains(config)) {
			configs.add(config);
		}
	}
	
	private synchronized void scheduleTasks() {
		if (this.projectConfigs.isEmpty()) {
			return;
		}
		final List<Task> tasks= new ArrayList<>();
		for (final Entry<IProject, List<AutoConfig>> entry : this.projectConfigs.entrySet()) {
			final IProject project= entry.getKey();
			try {
				for (final AutoConfig config : entry.getValue()) {
					if (config.isAvailable()) {
						for (final Task task : config.getTasks()) {
							if (tasks.isEmpty()) {
								if (task.isRequired(project)) {
									tasks.add(task);
								}
							}
							else {
								if (!tasks.contains(task)) {
									tasks.add(task);
								}
							}
						}
					}
				}
				
				if (!tasks.isEmpty()) {
					this.taskProcessor.add(project, tasks);
				}
			}
			catch (final CoreException e) {
				Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0,
						NLS.bind("An error occurred when aggregating auto configuration tasks for project ''{0}''", project),
						e ));
			}
			finally {
				tasks.clear();
			}
		}
		this.projectConfigs.clear();
		
		this.taskProcessor.schedule();
	}
	
}
