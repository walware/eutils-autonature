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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.SubMonitor;

import de.walware.eutils.autonature.core.IProjectConfigurator;


public class ConfiguratorTask extends Task {
	
	
	private final String label;
	
	private final String natureId;
	
	private IConfigurationElement configElement;
	
	private IProjectConfigurator configurator;
	
	
	public ConfiguratorTask(final String label, final String natureId,
			final IConfigurationElement configElement) {
		this.label= label;
		this.natureId= natureId;
		this.configElement= configElement;
	}
	
	
	private synchronized IProjectConfigurator getProjectConfigurator() throws CoreException {
		if (this.configElement != null) {
			try {
				this.configurator= (IProjectConfigurator) this.configElement
						.createExecutableExtension(ConfigManager.CLASS_ATTR_NAME);
			}
			finally {
				this.configElement= null;
			}
		}
		return this.configurator;
	}
	
	@Override
	public String getLabel() {
		return this.label;
	}
	
	@Override
	public boolean isAvailable() {
		return (this.label != null);
	}
	
	@Override
	public boolean isSupported(final byte mode) {
		return (mode == ConfigManager.MANUAL_MODE);
	}
	
	@Override
	public byte check(final IProject project, final int flags, final SubMonitor m) throws CoreException {
		if (this.natureId != null) {
			try {
				if (project.hasNature(this.natureId)) {
					return IProjectConfigurator.ALREADY_CONFIGURED;
				}
			}
			catch (final CoreException e) {}
		}
		if ((flags & CONTENT_MATCH) == 0) {
			return IProjectConfigurator.NOT_CONFIGURABLE;
		}
		final IProjectConfigurator configurator= getProjectConfigurator();
		if (configurator == null) {
			return 0;
		}
		return configurator.check(project, m);
	}
	
	public void configure(final IProject project, final SubMonitor m) {
		this.configurator.configure(project, m);
	}
	
	
	@Override
	public String toString() {
		return "ConfiguratorTask '" + this.label + "'"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
}
