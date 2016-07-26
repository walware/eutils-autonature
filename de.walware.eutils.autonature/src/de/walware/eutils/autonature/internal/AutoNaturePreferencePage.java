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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;

import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.ViewerUtil.CheckboxTableComposite;


public class AutoNaturePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	
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
		composite.setLayout(LayoutUtil.createCompositeGrid(1));
		
		{	this.enableButton= new Button(composite, SWT.CHECK);
			this.enableButton.setText("Enable automatic project &configuration.");
			this.enableButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		
		LayoutUtil.addSmallFiller(composite, false);
		
		{	final Label label= new Label(composite, SWT.NONE);
			label.setText("Enable project configuration for:");
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			final CheckboxTableComposite tableComposite= UIUtils.createContentTypeTable(composite, 1);
			this.entryViewer= tableComposite.viewer;
		}
		
		Dialog.applyDialogFont(composite);
		
		loadConfigs();
		loadPrefs();
		
		return composite;
	}
	
	private void loadConfigs() {
		final ConfigManager configManager= Activator.getDefault().getConfigManager();
		this.configs= configManager.getConfigs(ConfigManager.AUTO_MODE);
		UIUtils.sortConfigs(this.configs);
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
