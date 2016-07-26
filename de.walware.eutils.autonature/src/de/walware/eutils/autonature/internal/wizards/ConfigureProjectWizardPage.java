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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import de.walware.eutils.autonature.internal.Task;

import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.ViewerUtil;
import de.walware.ecommons.ui.util.ViewerUtil.CheckboxColumnControl;
import de.walware.ecommons.ui.util.ViewerUtil.TableComposite;


public class ConfigureProjectWizardPage extends WizardPage implements ICheckStateListener {
	
	
	private final IProject project;
	
	private TableComposite configTable;
	private CheckboxColumnControl<Task> configTableEnabledColumn;
	
	private Button deselectButton;
	
	private final Collection<Task> editableTasks= new HashSet<>();
	
	private final HashSet<Task> alreadyConfigTasks= new HashSet<>();
	private final HashSet<Task> toConfigTasks= new HashSet<>();
	
	
	public ConfigureProjectWizardPage(final IProject project) {
		super("AutoConfigureProject");
		
		this.project= project;
		
		setTitle(NLS.bind("Configure project ''{0}''", project.getName()));
		setDescription("Select the project configurations to apply.");
	}
	
	
	@Override
	public void createControl(final Composite parent) {
		initializeDialogUnits(parent);
		
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(LayoutUtil.createContentGrid(2));
		
		{	final TableComposite tableComposite= new TableComposite(composite,
					SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL );
			this.configTable= tableComposite;
			
			final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
			Dialog.applyDialogFont(tableComposite);
			gd.heightHint= tableComposite.table.getItemHeight() * 10;
			tableComposite.setLayoutData(gd);
			
			tableComposite.viewer.setContentProvider(new ArrayContentProvider());
			
			{	final TableViewerColumn column= new TableViewerColumn(tableComposite.viewer,
						SWT.CENTER );
				this.configTableEnabledColumn= new ViewerUtil.CheckboxColumnControl<Task>(
						tableComposite.viewer, this.toConfigTasks, this.editableTasks ) {
					@Override
					public boolean getChecked(final Object element) {
						return (ConfigureProjectWizardPage.this.alreadyConfigTasks.contains(element)
								|| super.getChecked(element) );
					}
				};
				column.setLabelProvider(this.configTableEnabledColumn);
				this.configTableEnabledColumn.configureAsMainColumn();
				tableComposite.layout.setColumnData(column.getColumn(), new ColumnPixelData(
						this.configTableEnabledColumn.hintColumnWidth(), false, true ));
				this.configTableEnabledColumn.addCheckStateListener(this);
			}
			{	final TableViewerColumn column= tableComposite.addColumn("Project Configuration",
						SWT.LEFT, new ColumnWeightData(5, true) );
				column.setLabelProvider(new CellLabelProvider() {
					@Override
					public void update(final ViewerCell cell) {
						final Task task= (Task) cell.getElement();
						cell.setText(task.getLabel());
					}
				});
			}
		}
		{	final Button button= new Button(composite, SWT.PUSH);
			button.setText("&Deselect All");
			
			final GridData gd= new GridData(SWT.FILL, SWT.TOP, false, false);
			gd.widthHint= LayoutUtil.hintWidth(button);
			button.setLayoutData(gd);
			
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					ConfigureProjectWizardPage.this.toConfigTasks.clear();
					ConfigureProjectWizardPage.this.configTable.viewer.update(ConfigureProjectWizardPage.this.editableTasks.toArray(), null);
					updateState();
				}
			});
			this.deselectButton= button;
		}
		
		setControl(composite);
		
		resetInput();
	}
	
	@Override
	public void setVisible(final boolean visible) {
		super.setVisible(visible);
		
		this.configTable.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				((ConfigureProjectWizard) getWizard()).runInit();
			}
		});
	}
	
	public void resetInput() {
		this.configTable.table.setEnabled(false);
		this.configTable.viewer.setInput(Collections.EMPTY_LIST);
		this.deselectButton.setEnabled(false);
		
		this.editableTasks.clear();
		this.toConfigTasks.clear();
		updateState();
	}
	
	public void setInput(final List<Task> allTasks, final List<Task> alreadyConfigTasks,
			final List<Task> recommendTasks) {
		for (final Task task : allTasks) {
			if (!alreadyConfigTasks.contains(task)) {
				this.editableTasks.add(task);
			}
		}
		this.alreadyConfigTasks.addAll(alreadyConfigTasks);
		this.toConfigTasks.addAll(recommendTasks);
		this.configTable.viewer.setInput(allTasks);
		this.configTable.table.setEnabled(true);
		updateState();
	}
	
	@Override
	public void checkStateChanged(final CheckStateChangedEvent event) {
		updateState();
	}
	
	private void updateState() {
		this.deselectButton.setEnabled(!this.toConfigTasks.isEmpty());
		getContainer().updateButtons();
	}
	
	@Override
	public boolean isPageComplete() {
		return (!this.toConfigTasks.isEmpty());
	}
	
	public Collection<Task> getConfigTasks() {
		return this.toConfigTasks;
	}
	
}
