/*******************************************************************************
 * Copyright (c) 2013, 2013 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package melnorme.utilbox.misc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/**
 * Some minor extensions to {@link ByteArrayOutputStream}
 */
public class ByteArrayOutputStreamExt extends ByteArrayOutputStream {
	
	public ByteArrayOutputStreamExt() {
	}
	
	public ByteArrayOutputStreamExt(int size) {
		super(size);
	}
    
	public synchronized byte[] getBuffer() {
		return buf;
	}
	
	public synchronized int getCount() {
		return count;
	}
	
	public synchronized String toString(Charset charset) {
		return new String(buf, 0, count, charset);
	}
	
}