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
import java.util.List;

import com.ibm.icu.text.Collator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;


public class AutoNaturePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	
	private static class AutoConfigComparator extends ViewerComparator {
		
		
		private final Collator collator= Collator.getInstance();
		
		
		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {
			return this.collator.compare(((AutoConfig) e1).getLabel(), ((AutoConfig) e2).getLabel());
		}
		
	}
	
	
	private Button enableButton;
	
	private CheckboxTableViewer entryViewer;
	
	private List<AutoConfig> configs;
	
	
	public AutoNaturePreferencePage() {
	}
	
	
	@Override
	public void init(final IWorkbench workbench) {
	}
	
	@Override
	protected Control createContents(final Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		{	final GridLayout gd= new GridLayout();
			gd.marginWidth= 0;
			gd.marginHeight= 0;
			gd.numColumns= 1;
			composite.setLayout(gd);
		}
		{	this.enableButton= new Button(composite, SWT.CHECK);
			this.enableButton.setText("Enable automatic project &configuration for:");
			this.enableButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		}
		{	final Composite tableComposite= new Composite(composite, SWT.NONE);
			this.entryViewer= new CheckboxTableViewer(
					new Table(tableComposite, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION) );
			final Table table= this.entryViewer.getTable();
			
			final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
			Dialog.applyDialogFont(tableComposite);
			gd.heightHint= table.getHeaderHeight() + table.getItemHeight() * 10;
			tableComposite.setLayoutData(gd);
			final TableColumnLayout tableLayout= new TableColumnLayout();
			tableComposite.setLayout(tableLayout);
			
			this.entryViewer.setContentProvider(new ArrayContentProvider());
			this.entryViewer.setComparator(new AutoConfigComparator());
			
			this.entryViewer.getTable().setHeaderVisible(true);
			{	final TableViewerColumn column= new TableViewerColumn(this.entryViewer, SWT.LEFT);
				tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(5, true));
				column.getColumn().setText("Content Type");
				column.setLabelProvider(new CellLabelProvider() {
					@Override
					public void update(final ViewerCell cell) {
						final AutoConfig config= (AutoConfig) cell.getElement();
						cell.setText(config.getLabel());
					}
				});
			}
			{	final TableViewerColumn column= new TableViewerColumn(this.entryViewer, SWT.LEFT);
				tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(5, true));
				column.getColumn().setText("Project Configuration");
				column.setLabelProvider(new CellLabelProvider() {
					@Override
					public void update(final ViewerCell cell) {
						final AutoConfig config= (AutoConfig) cell.getElement();
						final List<Task> tasks= config.getTasks();
						if (tasks.size() == 1) {
							cell.setText(tasks.get(0).getLabel());
						}
						else {
							final StringBuilder sb= new StringBuilder();
							sb.append(tasks.get(0).getLabel());
							for (int i= 1; i < tasks.size(); i++) {
								sb.append(", ");
								sb.append(tasks.get(i).getLabel());
							}
							cell.setText(sb.toString());
						}
					}
				});
			}
		}
		
		Dialog.applyDialogFont(composite);
		
		loadConfigs();
		loadPrefs();
		
		return composite;
	}
	
	private void loadConfigs() {
		final ConfigManager configManager= Activator.getDefault().getConfigManager();
		this.configs= configManager.getConfigs();
		this.entryViewer.setInput(this.configs);
	}
	
	private void loadPrefs() {
		final IPreferencesService preferences= Platform.getPreferencesService();
		
		this.enableButton.setSelection(preferences.getBoolean(
				Activator.PLUGIN_ID, Activator.ENABLED_PREF_KEY, true, null ));
		
		final List<AutoConfig> checked= new ArrayList<>();
		for (final AutoConfig config : this.configs) {
			if (preferences.getBoolean(
					ConfigManager.PREF_QUALIFIER, config.getEnabledPrefKey(), false, null )) {
				checked.add(config);
			}
		}
		this.entryViewer.setCheckedElements(checked.toArray());
	}
	
	private void savePrefs(final boolean flush) {
		final IEclipsePreferences node= InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		
		node.putBoolean(Activator.ENABLED_PREF_KEY, this.enableButton.getSelection());
		
		final IEclipsePreferences configsNode= InstanceScope.INSTANCE.getNode(ConfigManager.PREF_QUALIFIER);
		final List<Object> enabled= Arrays.asList(this.entryViewer.getCheckedElements());
		for (final AutoConfig config : this.configs) {
			configsNode.putBoolean(config.getEnabledPrefKey(), enabled.contains(config));
		}
		
		if (flush) {
			try {
				node.flush();
			}
			catch (final BackingStoreException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, 
						"An error occured when saving the autorun launch configuration.", e));
			}
		}
	}
	
	
	@Override
	protected void performDefaults() {
		this.enableButton.setSelection(true);
		
		final IEclipsePreferences configsNode= DefaultScope.INSTANCE.getNode(ConfigManager.PREF_QUALIFIER);
		final List<AutoConfig> checked= new ArrayList<>();
		for (final AutoConfig config : this.configs) {
			if (configsNode.getBoolean(config.getEnabledPrefKey(), false)) {
				checked.add(config);
			}
		}
		this.entryViewer.setCheckedElements(checked.toArray());
	}
	
	@Override
	protected void performApply() {
		savePrefs(true);
	}
	
	@Override
	public boolean performOk() {
		savePrefs(false);
		return true;
	}
	
}
