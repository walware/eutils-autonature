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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ibm.icu.text.Collator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import de.walware.ecommons.ui.util.ViewerUtil.CheckboxTableComposite;

public class UIUtils {
	
	
	private static class AutoConfigComparator implements Comparator<AutoConfig> {
		
		private final Collator collator= Collator.getInstance();
		
		@Override
		public int compare(final AutoConfig o1, final AutoConfig o2) {
			return this.collator.compare(o1.getLabel(), o2.getLabel());
		}
		
	}
	
	private static class TaskComparator implements Comparator<Task> {
		
		private final Collator collator= Collator.getInstance();
		
		@Override
		public int compare(final Task o1, final Task o2) {
			return this.collator.compare(o1.getLabel(), o2.getLabel());
		}
		
	}
	
	
	public static void sortConfigs(final List<AutoConfig> configs) {
		Collections.sort(configs, new AutoConfigComparator());
	}
	
	public static void sortTasks(final List<Task> configs) {
		Collections.sort(configs, new TaskComparator());
	}
	
	public static CheckboxTableComposite createContentTypeTable(final Composite parent, final int hSpan) {
		final CheckboxTableComposite tableComposite= new CheckboxTableComposite(parent,
				SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION );
		
		final GridData gd= new GridData(SWT.FILL, SWT.FILL, true, true, hSpan, 1);
		Dialog.applyDialogFont(tableComposite);
		gd.heightHint= tableComposite.table.getHeaderHeight() + tableComposite.table.getItemHeight() * 10;
		tableComposite.setLayoutData(gd);
		
		tableComposite.viewer.setContentProvider(new ArrayContentProvider());
		
		tableComposite.viewer.getTable().setHeaderVisible(true);
		{	final TableViewerColumn column= tableComposite.addColumn("Content Type",
					SWT.LEFT, new ColumnWeightData(5, true) );
			column.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					final AutoConfig config= (AutoConfig) cell.getElement();
					cell.setText(config.getLabel());
				}
			});
		}
		{	final TableViewerColumn column= tableComposite.addColumn("Project Configuration",
					SWT.LEFT, new ColumnWeightData(5, true) );
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
							sb.append(" + ");
							sb.append(tasks.get(i).getLabel());
						}
						cell.setText(sb.toString());
					}
				}
			});
		}
		
		return tableComposite;
	}
	
}
