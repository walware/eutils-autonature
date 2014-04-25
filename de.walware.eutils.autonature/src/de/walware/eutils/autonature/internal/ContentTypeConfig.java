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

import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;


public class ContentTypeConfig extends AutoConfig {
	
	
	private final String contentTypeId;
	
	private String label;
	
	
	public ContentTypeConfig(final String contentTypeId, final List<Task> tasks) {
		super(ConfigManager.ON_FILE_CONTENT_CONTRIB + ':' + contentTypeId, tasks);
		this.contentTypeId= contentTypeId;
	}
	
	
	public String getContentTypeId() {
		return this.contentTypeId;
	}
	
	
	@Override
	public String getLabel() {
		if (this.label == null) {
			final IContentType contentType= Platform.getContentTypeManager().getContentType(this.contentTypeId);
			if (contentType != null) {
				this.label= contentType.getName();
			}
		}
		return this.label;
	}
	
}
