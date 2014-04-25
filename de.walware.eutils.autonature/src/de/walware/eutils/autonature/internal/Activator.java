/*=============================================================================#
 # Copyright (c) 2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.eutils.autonature.internal;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements IPreferenceChangeListener {
	
	public static final String PLUGIN_ID= "de.walware.eutils.autonature"; //$NON-NLS-1$
	
	public static final String ENABLED_PREF_KEY= "enabled"; //$NON-NLS-1$
	
	
	private static Activator gPlugin;
	
	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return gPlugin;
	}
	
	public static void log(final IStatus status) {
		final Activator plugin= getDefault();
		if (plugin != null) {
			final ILog log= plugin.getLog();
			if (log != null) {
				log.log(status);
			}
		}
	}
	
	
	private ConfigManager configManager;
	private ResourceListener listener;
	
	
	public Activator() {
	}
	
	
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		gPlugin= this;
		
		this.configManager= new ConfigManager();
		
		InstanceScope.INSTANCE.getNode(PLUGIN_ID).addPreferenceChangeListener(this);
	}
	
	@Override
	public void stop(final BundleContext context) throws Exception {
		gPlugin= null;
		super.stop(context);
	}
	
	
	public void runStartup() {
		updateMonitor(true);
	}
	
	@Override
	public void preferenceChange(final PreferenceChangeEvent event) {
		if (event.getKey().equals(ENABLED_PREF_KEY)) {
			updateMonitor(false);
		}
	}
	
	public ConfigManager getConfigManager() {
		return this.configManager;
	}
	
	private void updateMonitor(final boolean startup) {
		final IWorkspace workspace= ResourcesPlugin.getWorkspace();
		final boolean enabled= Platform.getPreferencesService().getBoolean(PLUGIN_ID, ENABLED_PREF_KEY, true, null);
		synchronized (this) {
			if (enabled) {
				if (this.listener == null) {
					this.listener= new ResourceListener(this.configManager);
				}
				workspace.addResourceChangeListener(this.listener, IResourceChangeEvent.POST_CHANGE);
				// TODO use save participant
				// if (startup) {
				// }
			}
			else {
				if (this.listener != null) {
					workspace.removeResourceChangeListener(this.listener);
				}
			}
		}
	}
}
