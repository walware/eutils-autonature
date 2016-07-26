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

package de.walware.eutils.autonature.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.eutils.autonature.internal.Activator;
import de.walware.eutils.autonature.internal.AutoConfig;
import de.walware.eutils.autonature.internal.ConfigManager;
import de.walware.eutils.autonature.internal.ContentFinder;
import de.walware.eutils.autonature.internal.Task;
import de.walware.eutils.autonature.internal.TaskProcessor;

import de.walware.ecommons.ui.util.DialogUtil;


public class ConfigureProjectWizard extends Wizard {
	
	
	private final IProject project;
	
	private final ConfigManager configManager;
	
	private ConfigureProjectWizardPage configPage;
	
	private List<Task> allTasks;
	
	
	public ConfigureProjectWizard(final IProject project) {
		this.project= project;
		this.configManager= Activator.getDefault().getConfigManager();
		
		setDialogSettings(DialogUtil.getDialogSettings(Activator.getDefault(), "ConfigureProjectWizard")); //$NON-NLS-1$
		setWindowTitle("Detect and Add Project Natures");
		setNeedsProgressMonitor(true);
	}
	
	
	@Override
	public void addPages() {
		this.configPage= new ConfigureProjectWizardPage(this.project);
		addPage(this.configPage);
	}
	
	
	void runInit() {
		if (this.allTasks != null) {
			return;
		}
		try {
			final List<Task> allTasks= new ArrayList<>();
			final List<Task> notConfigTasks= new ArrayList<>();
			final List<Task> alreadyConfigTasks= new ArrayList<>();
			final List<Task> selectedTasks= new ArrayList<>();
			
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException {
					final SubMonitor m= SubMonitor.convert(monitor, 3 + 2);
					
					try {
						if (monitor.isCanceled()) {
							throw new OperationCanceledException();
						}
						
						final ContentFinder contentFinder= new ContentFinder(
								ConfigureProjectWizard.this.project, null,
								ConfigureProjectWizard.this.configManager, ConfigManager.MANUAL_MODE,
								m.newChild(3) );
						ConfigureProjectWizard.this.project.accept(contentFinder);
						
						if (monitor.isCanceled()) {
							throw new OperationCanceledException();
						}
						
						final List<AutoConfig> recommendConfigs= contentFinder.getConfigs();
						final List<AutoConfig> otherConfigs= new ArrayList<>();
						for (final AutoConfig config : ConfigureProjectWizard.this.configManager.getConfigs(ConfigManager.MANUAL_MODE)) {
							if (!recommendConfigs.contains(config)) {
								otherConfigs.add(config);
							}
						}
						
						TaskProcessor.aggregateTasks(ConfigureProjectWizard.this.project,
								recommendConfigs, Task.CONTENT_MATCH,
								selectedTasks, alreadyConfigTasks, notConfigTasks,
								m.newChild(1) );
						
						allTasks.addAll(selectedTasks);
						TaskProcessor.aggregateTasks(ConfigureProjectWizard.this.project,
								otherConfigs, 0,
								allTasks, alreadyConfigTasks, notConfigTasks,
								m.newChild(1) );
						allTasks.addAll(alreadyConfigTasks);
						notConfigTasks.addAll(alreadyConfigTasks);
					}
					catch (final CoreException e) {
						if (e.getStatus().getSeverity() == IStatus.CANCEL) {
							throw new InvocationTargetException(new OperationCanceledException());
						}
						throw new InvocationTargetException(e);
					}
				}
			});
			
			this.allTasks= allTasks;
			
			this.configPage.setInput(allTasks, alreadyConfigTasks, selectedTasks);
		}
		catch (final InterruptedException e) {
			return;
		}
		catch (final InvocationTargetException e) {
			final Throwable cause= e.getCause();
			if (cause instanceof InterruptedException) {
				return;
			}
			
			StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0,
					"An error occurred when detecting the project configuration proposal.", e ),
					StatusManager.SHOW | StatusManager.LOG );
			return;
		}
	}
	
	void reset() {
		this.allTasks= null;
		this.configPage.resetInput();
		
		runInit();
	}
	
	
	@Override
	public boolean canFinish() {
		return (this.configPage != null && this.configPage.isPageComplete());
	}
	
	@Override
	public boolean performFinish() {
		final Collection<Task> configTasks= this.configPage.getConfigTasks();
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException {
					final SubMonitor m= SubMonitor.convert(monitor, "Configure projects...", 1);
					
					final IJobManager jobManager= Job.getJobManager();
					final ISchedulingRule rule= ConfigureProjectWizard.this.project.getWorkspace().getRoot();
					try {
						jobManager.beginRule(rule, m);
						
						final IStatus projectStatus= TaskProcessor.process(ConfigureProjectWizard.this.project, configTasks,
								m.newChild(1) );
						if (!projectStatus.isOK()) {
							throw new InvocationTargetException(new CoreException(projectStatus));
						}
					}
					catch (final OperationCanceledException e) {
						throw new InvocationTargetException(e);
					}
					finally {
						jobManager.endRule(rule);
					}
				}
			});
		}
		catch (final InterruptedException e) {
			reset();
			return false;
		}
		catch (final InvocationTargetException e) {
			final Throwable cause= e.getCause();
			if (cause instanceof InterruptedException || cause instanceof OperationCanceledException) {
				reset();
				return false;
			}
			
			StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0,
					"The project configuration failed for 1 project.", cause ),
					StatusManager.SHOW | StatusManager.LOG );
			return false;
		}
		return true;
	}
	
}
