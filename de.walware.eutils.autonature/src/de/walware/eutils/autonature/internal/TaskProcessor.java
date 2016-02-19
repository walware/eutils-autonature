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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;


public class TaskProcessor {
	
	
	private class ConfigJob extends WorkspaceJob {
		
		
		public ConfigJob() {
			super("Auto Project Configuration");
			setUser(false);
			setRule(ResourcesPlugin.getWorkspace().getRoot());
		}
		
		
		@Override
		public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
			return TaskProcessor.this.process(monitor);
		}
		
	}
	
	
	private Map<IProject, List<Task>> projectTasks;
	
	private ConfigJob job;
	
	
	public TaskProcessor() {
		this.projectTasks= new HashMap<>();
	}
	
	
	public synchronized void add(final IProject project, final List<Task> tasks) {
		final List<Task> allTasks= this.projectTasks.get(project);
		if (allTasks == null) {
			this.projectTasks.put(project, new ArrayList<>(tasks));
			return;
		}
		for (final Task task : tasks) {
			if (!allTasks.contains(task)) {
				allTasks.add(task);
			}
		}
	}
	
	public synchronized void schedule() {
		if (this.projectTasks.isEmpty()) {
			return;
		}
		if (this.job == null) {
			this.job= new ConfigJob();
		}
		this.job.schedule();
	}
	
	
	private IStatus process(final IProgressMonitor monitor) {
		final Map<IProject, List<Task>> tasks;
		synchronized (this) {
			tasks= this.projectTasks;
			if (tasks.isEmpty()) {
				return Status.OK_STATUS;
			}
			this.projectTasks= new HashMap<>();
		}
		
		final SubMonitor progress= SubMonitor.convert(monitor, tasks.size());
		List<Status> status= null;
		for (final Map.Entry<IProject, List<Task>> entry : tasks.entrySet()) {
			try {
				process(entry.getKey(), entry.getValue(), progress.newChild(1, SubMonitor.SUPPRESS_NONE));
			}
			catch (final CoreException e) {
				if (status == null) {
					status= new ArrayList<>(tasks.size());
				}
				status.add(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						NLS.bind("An error occurred when configuring project ''{0}''.", entry.getKey().getName()),
						e ));
			}
		}
		
		if (status != null) {
			return new MultiStatus(Activator.PLUGIN_ID, 0, status.toArray(new IStatus[status.size()]),
					(status.size() == 1) ?
							"The Auto Project Configuration failed for 1 project." :
							NLS.bind("The Auto Project Configuration failed for {0} projects.", status.size()),
					null );
		}
		return Status.OK_STATUS;
	}
	
	private static boolean contains(final String[] array, final String s) {
		for (int i= 0; i < array.length; i++) {
			if (array[i] == s) {
				return true;
			}
		}
		return false;
	}
	
	private void process(final IProject project, final List<Task> tasks,
			final SubMonitor progress) throws CoreException {
		progress.beginTask(NLS.bind("Configuring project ''{0}''", project.getName()),
				3 );
		final IProjectDescription description= project.getDescription();
		boolean descriptionChanged= false;
		
		final String[] existingNatureIds= description.getNatureIds();
		final List<String> natureIds= new ArrayList<>(existingNatureIds.length + tasks.size());
		for (int i= 0; i < existingNatureIds.length; i++) {
			natureIds.add(existingNatureIds[i]= existingNatureIds[i].intern());
		}
		for (final Task task : tasks) {
			if (task instanceof NatureTask) {
				final String natureId= ((NatureTask) task).getNatureId();
				if (!contains(existingNatureIds, natureId)) {
					natureIds.add(natureId);
				}
			}
		}
		progress.worked(1);
		
		if (natureIds.size() > existingNatureIds.length) {
			final ConfigManager configManager= Activator.getDefault().getConfigManager();
			configManager.arrangeNatures(natureIds, existingNatureIds.length);
			description.setNatureIds(natureIds.toArray(new String[natureIds.size()]));
			descriptionChanged= true;
		}
		
		if (descriptionChanged) {
			progress.setWorkRemaining(1);
			project.setDescription(description, progress.newChild(1));
		}
	}
	
}
