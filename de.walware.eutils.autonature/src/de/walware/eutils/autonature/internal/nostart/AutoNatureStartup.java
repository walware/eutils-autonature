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

package de.walware.eutils.autonature.internal.nostart;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.ui.IStartup;

import de.walware.eutils.autonature.internal.Activator;


public class AutoNatureStartup implements IStartup {
	
	
	public AutoNatureStartup() {
	}
	
	
	@Override
	public void earlyStartup() {
		final IPreferencesService preferences= Platform.getPreferencesService();
		if (preferences.getBoolean(Activator.PLUGIN_ID, Activator.ENABLED_PREF_KEY, true, null)) {
			Activator.getDefault().runStartup();
		}
	}
	
}
