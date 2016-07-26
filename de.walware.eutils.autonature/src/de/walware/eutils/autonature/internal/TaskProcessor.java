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
import java.util.Collection;
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

import de.walware.eutils.autonature.core.IProjectConfigurator;


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
	
	
	public static void aggregateTasks(final IProject project, final List<AutoConfig> configs,
			final int flags,
			List<Task> configurableTasks, List<Task> alreadyConfigTasks, List<Task> notConfigTasks,
			final SubMonitor m) throws CoreException {
		final int l= configs.size();
		
		if (configurableTasks == null) {
			configurableTasks= new ArrayList<>(l);
		}
		if (alreadyConfigTasks == null) {
			alreadyConfigTasks= new ArrayList<>(l);
		}
		if (notConfigTasks == null) {
			notConfigTasks= new ArrayList<>();
		}
		
		for (int i= 0; i < l; i++) {
			m.setWorkRemaining(l - i);
			
			final AutoConfig config= configs.get(i);
			if (config.isAvailable()) {
				for (final Task task : config.getTasks()) {
					if (!(configurableTasks.contains(task) 
							|| alreadyConfigTasks.contains(task)
							|| notConfigTasks.contains(task) )) {
						switch (task.check(project, flags, m.newChild(1))) {
						case IProjectConfigurator.CONFIGURABLE:
							configurableTasks.add(task);
							break;
						case IProjectConfigurator.ALREADY_CONFIGURED:
							alreadyConfigTasks.add(task);
							break;
						default:
							notConfigTasks.add(task);
							break;
						}
					}
				}
			}
		}
	}
	
	public static IStatus process(final IProject project, final Collection<Task> tasks,
			final SubMonitor m) {
		try {
			int configuratorCount= 0;
			for (final Task task : tasks) {
				if (task instanceof ConfiguratorTask) {
					configuratorCount++;
				}
			}
			
			m.beginTask(NLS.bind("Configuring project ''{0}''", project.getName()), 3 + configuratorCount);
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
			m.worked(1);
			
			if (natureIds.size() > existingNatureIds.length) {
				final ConfigManager configManager= Activator.getDefault().getConfigManager();
				configManager.arrangeNatures(natureIds, existingNatureIds.length);
				description.setNatureIds(natureIds.toArray(new String[natureIds.size()]));
				descriptionChanged= true;
			}
			
			if (descriptionChanged) {
				m.setWorkRemaining(1);
				project.setDescription(description, m.newChild(1));
			}
			
			for (final Task task : tasks) {
				if (task instanceof ConfiguratorTask) {
					((ConfiguratorTask) task).configure(project, m.newChild(1));
				}
			}
			
			return Status.OK_STATUS;
		}
		catch (final CoreException e) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					NLS.bind("An error occurred when configuring project ''{0}''.", project.getName()),
					e );
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
		List<IStatus> status= null;
		for (final Map.Entry<IProject, List<Task>> entry : tasks.entrySet()) {
			final IStatus projectStatus= process(entry.getKey(), entry.getValue(),
					progress.newChild(1, SubMonitor.SUPPRESS_NONE) );
			if (!projectStatus.isOK()) {
				if (status == null) {
					status= new ArrayList<>(tasks.size());
				}
				status.add(projectStatus);
			}
		}
		
		if (status != null) {
			return new MultiStatus(Activator.PLUGIN_ID, 0, status.toArray(new IStatus[status.size()]),
					(status.size() == 1) ?
							"The auto project configuration failed for 1 project." :
							NLS.bind("The auto project configuration failed for {0} projects.", status.size()),
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
	
}
