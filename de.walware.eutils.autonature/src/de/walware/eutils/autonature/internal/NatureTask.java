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
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;


public class NatureTask extends Task {
	
	
	private static NatureTask get(final List<NatureTask> tasks, final String natureId) {
		final int size= tasks.size();
		for (int i= 0; i < size; i++) {
			final NatureTask task= tasks.get(i);
			if (task.getNatureId() == natureId) {
				return task;
			}
		}
		return null;
	}
	
	
	private final String natureId;
	
	private final String label;
	
	private final List<String> required;
	
	private final List<NatureTask> prev= new ArrayList<>(4);
	private final List<NatureTask> prevFlat= new ArrayList<>(8);
	
	
	public NatureTask(final String natureId, final String label, final List<String> required) {
		this.natureId= natureId;
		this.label= label;
		this.required= required;
	}
	
	
	public String getNatureId() {
		return this.natureId;
	}
	
	@Override
	public String getLabel() {
		return this.label;
	}
	
	@Override
	public boolean isAvailable() {
		return (this.label != null);
	}
	
	public List<String> getRequiredNatureIds() {
		return this.required;
	}
	
	void addPrev(final NatureTask task) {
		if (!this.prev.contains(task)) {
			this.prev.add(task);
		}
	}
	
	void finish() {
		this.prevFlat.clear();
		for (int i= 0; i < this.prev.size(); i++) {
			this.prevFlat.add(this.prev.get(i));
		}
		for (int i= 0; i < this.prevFlat.size(); i++) {
			final List<NatureTask> tasks= this.prevFlat.get(i).prev;
			for (int j= 0; j < tasks.size(); j++) {
				final NatureTask task= tasks.get(j);
				if (task != this && !this.prevFlat.contains(task)) {
					this.prevFlat.add(task);
				}
			}
		}
	}
	
	boolean isSubsequentTo(final String natureId) {
		final NatureTask task= get(this.prevFlat, natureId);
		return (task != null && get(task.prevFlat, this.natureId) == null);
	}
	
	@Override
	public boolean isRequired(final IProject project) throws CoreException {
		return !project.hasNature(this.natureId);
	}
	
	
	@Override
	public String toString() {
		return "NatureTask '" + this.natureId + "'"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
}
