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

import java.util.Collections;
import java.util.List;


public abstract class AutoConfig {
	
	
	static class Dummy extends AutoConfig {
		
		public Dummy(final String id) {
			super(id, Collections.<Task>emptyList());
		}
		
		@Override
		public String getLabel() {
			return null;
		}
		
	}
	
	
	private final String id;
	
	private final String enabledPrefKey;
	
	private final List<Task> tasks;
	
	
	public AutoConfig(final String id, final List<Task> tasks) {
		this.id= id;
		this.enabledPrefKey= id + ".enabled";
		this.tasks= tasks;
	}
	
	
	public String getId() {
		return this.id;
	}
	
	public String getEnabledPrefKey() {
		return this.enabledPrefKey;
	}
	
	public boolean isAvailable() {
		if (getLabel() == null) {
			return false;
		}
		for (final Task task : this.tasks) {
			if (!task.isAvailable()) {
				return false;
			}
		}
		return true;
	}
	
	public abstract String getLabel();
	
	public List<Task> getTasks() {
		return this.tasks;
	}
	
	
	@Override
	public String toString() {
		return this.id;
	}
	
}
