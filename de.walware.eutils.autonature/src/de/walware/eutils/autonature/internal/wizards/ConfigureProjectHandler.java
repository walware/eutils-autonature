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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchPart;

import de.walware.ecommons.ui.util.UIAccess;
import de.walware.ecommons.workbench.ui.WorkbenchUIUtil;


public class ConfigureProjectHandler extends AbstractHandler {
	
	
	public ConfigureProjectHandler() {
	}
	
	
	private IProject getProject(final Object element) {
		if (element instanceof IProject) {
			return (IProject) element;
		}
		if (element instanceof IAdaptable) {
			return (IProject) ((IAdaptable) element).getAdapter(IProject.class);
		}
		return null;
	}
	
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISelection selection= WorkbenchUIUtil.getCurrentSelection(event.getApplicationContext());
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection structuredSelection= (IStructuredSelection) selection;
			if (structuredSelection.size() == 1) {
				final IProject project= getProject(structuredSelection.getFirstElement());
				if (project != null) {
					final IWorkbenchPart activePart= WorkbenchUIUtil.getActivePart(event.getApplicationContext());
					
					final ConfigureProjectWizard wizard= new ConfigureProjectWizard(project);
					final WizardDialog dialog= new WizardDialog(
							(activePart != null) ? activePart.getSite().getShell() : UIAccess.getActiveWorkbenchShell(true),
							wizard );
					dialog.open();
				}
			}
		}
		return null;
	}
	
}
