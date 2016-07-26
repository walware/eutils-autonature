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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;


public class ConfigManager implements IPreferenceChangeListener {
	
	
	private static final String EXTENSION_POINT_ID= "de.walware.eutils.autonature.autoConfigurations"; //$NON-NLS-1$
	
	static final String ON_FILE_CONTENT_CONTRIB= "onFileContent"; //$NON-NLS-1$
	
	private static final String LABEL_ATTR_NAME= "label"; //$NON-NLS-1$
	static final String CLASS_ATTR_NAME= "class"; //$NON-NLS-1$
	
	private static final String CONTENT_TYPE_ID_ATTR_NAME= "contentTypeId"; //$NON-NLS-1$
	private static final String ENABLE_ATTR_NAME= "enable"; //$NON-NLS-1$
	
	private static final String ENSURE_PROJECT_NATURE_ELEMENT_NAME= "ensureProjectNature"; //$NON-NLS-1$
	private static final String RUN_PROJECT_CONFIGURATOR_ELEMENT_NAME= "runProjectConfigurator"; //$NON-NLS-1$
	private static final String NATURE_ID_ATTR_NAME= "natureId"; //$NON-NLS-1$
	
	static final String PREF_QUALIFIER= Activator.PLUGIN_ID + "/configurations"; //$NON-NLS-1$
	
	public static final byte AUTO_MODE= 1;
	public static final byte MANUAL_MODE= 2;
	
	
	private static final AutoConfig DISABLED= new AutoConfig.Dummy("disabled");
	
	
	private class UpdateJob extends Job {
		
		public UpdateJob() {
			super("Update Auto Project Configuration");
			setUser(false);
			setSystem(true);
			setPriority(Job.SHORT);
		}
		
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			ConfigManager.this.lock.writeLock().lock();
			try {
				updateActiveTasks();
			}
			finally {
				ConfigManager.this.lock.writeLock().unlock();
			}
			return Status.OK_STATUS;
		}
		
	}
	
	
	private final Map<String, AutoConfig> contentConfigs= new HashMap<>();
	
	private final Map<String, AutoConfig> activeContentTasks= new HashMap<>();
	
	private final HashMap<String, NatureTask> natureTasks= new HashMap<>();
	
	private final ReentrantReadWriteLock lock= new ReentrantReadWriteLock();
	
	private Job updateJob;
	
	
	public ConfigManager() {
		loadContributions();
		updateActiveTasks();
		
		InstanceScope.INSTANCE.getNode(PREF_QUALIFIER).addPreferenceChangeListener(this);
	}
	
	
	private void loadContributions() {
		final IConfigurationElement[] contributions= Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_ID);
		final IEclipsePreferences defaultsNode= DefaultScope.INSTANCE.getNode(PREF_QUALIFIER);
		for (final IConfigurationElement contribution : contributions) {
			final String name= contribution.getName();
			if (name.equals(ON_FILE_CONTENT_CONTRIB)) {
				String contentTypeId= contribution.getAttribute(CONTENT_TYPE_ID_ATTR_NAME);
				if (contentTypeId == null || contentTypeId.isEmpty()) {
					continue;
				}
				contentTypeId= contentTypeId.intern();
				final List<Task> tasks= loadTasks(contribution.getChildren());
				if (tasks.isEmpty()) {
					continue;
				}
				final ContentTypeConfig config= new ContentTypeConfig(contentTypeId, tasks);
				defaultsNode.putBoolean(config.getEnabledPrefKey(), Boolean.parseBoolean(
						contribution.getAttribute(ENABLE_ATTR_NAME) ));
				this.contentConfigs.put(contentTypeId, config);
			}
		}
		
		for (final NatureTask task : this.natureTasks.values()) {
			task.finish();
		}
	}
	
	private List<Task> loadTasks(final IConfigurationElement[] tasksElements) {
		final List<Task> tasks= new ArrayList<>(tasksElements.length);
		for (int i= 0; i < tasksElements.length; i++) {
			final IConfigurationElement taskElement= tasksElements[i];
			switch (taskElement.getName()) {
			case ENSURE_PROJECT_NATURE_ELEMENT_NAME: {
					final String natureId= taskElement.getAttribute(NATURE_ID_ATTR_NAME);
					if (natureId == null || natureId.isEmpty()) {
						continue;
					}
					tasks.add(getNatureTask(natureId));
					continue;
				}
			case RUN_PROJECT_CONFIGURATOR_ELEMENT_NAME: {
					final String label= taskElement.getAttribute(LABEL_ATTR_NAME);
					if (label == null || label.isEmpty()) {
						continue;
					}
					final String className= taskElement.getAttribute(CLASS_ATTR_NAME);
					if (className == null || className.isEmpty()) {
						continue;
					}
					tasks.add(getConfiguratorTask(label, taskElement));
					continue;
				}
			default:
				continue;
			}
		}
		checkTasks(tasks);
		return tasks;
	}
	
	private void checkTasks(final List<Task> tasks) {
		if (tasks.size() <= 1) {
			return;
		}
		NatureTask prev= null;
		for (final Task task : tasks) {
			if (task instanceof NatureTask) {
				final NatureTask current= (NatureTask) task;
				if (prev != null) {
					current.addPrev(prev);
				}
				prev= current;
			}
		}
	}
	
	private NatureTask getNatureTask(String natureId) {
		NatureTask task= this.natureTasks.get(natureId);
		if (task == null) {
			natureId= natureId.intern();
			final IProjectNatureDescriptor nature= ResourcesPlugin.getWorkspace().getNatureDescriptor(natureId);
			task= (nature != null) ? new NatureTask(natureId, nature.getLabel(),
							Arrays.asList(nature.getRequiredNatureIds()) ) :
					new NatureTask(natureId, null, Collections.<String>emptyList());
			this.natureTasks.put(natureId, task);
			for (final String requiredId : task.getRequiredNatureIds()) {
				task.addPrev(getNatureTask(requiredId));
			}
		}
		return task;
	}
	
	private ConfiguratorTask getConfiguratorTask(final String label, final IConfigurationElement taskElement) {
		return new ConfiguratorTask(label, taskElement.getAttribute(NATURE_ID_ATTR_NAME),
				taskElement );
	}
	
	@Override
	public synchronized void preferenceChange(final PreferenceChangeEvent event) {
		if (this.updateJob == null) {
			this.updateJob= new UpdateJob();
		}
		this.updateJob.schedule(100);
	}
	
	private void updateActiveTasks() {
		final IPreferencesService preferences= Platform.getPreferencesService();
		for (final Map.Entry<String, AutoConfig> entry : this.contentConfigs.entrySet()) {
			final AutoConfig config= entry.getValue();
			this.activeContentTasks.put(entry.getKey(),
					(preferences.getBoolean(PREF_QUALIFIER, config.getEnabledPrefKey(), false, null)) ?
							config : DISABLED );
		}
	}
	
	
	public List<AutoConfig> getConfigs(final byte mode) {
		ArrayList<AutoConfig> list;
		this.lock.readLock().lock();
		try {
			list= new ArrayList<>(this.contentConfigs.values());
		}
		finally {
			this.lock.readLock().unlock();
		}
		for (final Iterator<AutoConfig> iter= list.iterator(); iter.hasNext();) {
			final AutoConfig config= iter.next();
			if (!(config.isAvailable() && config.isSupported(mode))) {
				iter.remove();
			}
		}
		return list;
	}
	
	public boolean hasActiveConfigs() {
		this.lock.readLock().lock();
		try {
			return !this.activeContentTasks.isEmpty();
		}
		finally {
			this.lock.readLock().unlock();
		}
	}
	
	public AutoConfig getConfig(IContentType contentType, final byte mode) {
		this.lock.readLock().lock();
		try {
			while (contentType != null) {
				final AutoConfig config= this.activeContentTasks.get(contentType.getId());
				if (config != null && config.isSupported(mode)) {
					return (config != DISABLED) ? config : null;
				}
				contentType= contentType.getBaseType();
			}
			return null;
		}
		finally {
			this.lock.readLock().unlock();
		}
	}
	
	
	public List<String> arrangeNatures(final List<String> natureIds, final int newIdx) {
		this.lock.readLock().lock();
		try {
			Collections.sort(natureIds.subList(newIdx, natureIds.size()));
			
			// Insert required
			ITER_I: for (int i= 0; i < natureIds.size(); i++) {
				CHECK_I: while(true) {
					final NatureTask task= this.natureTasks.get(natureIds.get(i));
					if (task != null) {
						for (final String requiredId : task.getRequiredNatureIds()) {
							if (!natureIds.contains(requiredId)) {
								natureIds.add(i, requiredId);
								continue CHECK_I;
							}
						}
					}
					continue ITER_I;
				}
			}
			
			final int n= natureIds.size();
			ITER_I: for (int i= 0; i < n; i++) {
				CHECK_I: for (int counter= n - i; counter > 0; counter--) {
					final String natureId= natureIds.get(i);
					final NatureTask task= this.natureTasks.get(natureId);
					if (task != null) {
						for (int j= n - 1; j > i; j--) {
							if (task.isSubsequentTo(natureIds.get(j))) {
								// move i -> j
								natureIds.remove(i);
								natureIds.add(j, natureId);
								continue CHECK_I; // check new i
							}
						}
					}
					continue ITER_I; // OK, check i + 1
				}
			}
			return natureIds;
		}
		finally {
			this.lock.readLock().unlock();
		}
	}
	
}
