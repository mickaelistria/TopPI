/*
	This file is part of TopPI - see https://github.com/slide-lig/TopPI/
	
	Copyright 2016 Martin Kirchgessner, Vincent Leroy, Alexandre Termier, Sihem Amer-Yahia, Marie-Christine Rousset, Université Grenoble Alpes, LIG, CNRS
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	 http://www.apache.org/licenses/LICENSE-2.0
	 
	or see the LICENSE.txt file joined with this program.
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package fr.liglab.mining.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;


/**
 * a thread-unsafe PatternsCollector that write to the path provided at instanciation
 * @see MultiThreadedFileCollector
 */
public class FileCollector implements PatternsCollector {
	
	// this should be profiled and tuned !
	protected static final int BUFFER_CAPACITY = 4096;
	
	protected long collected = 0;
	protected long collectedLength = 0;
	protected FileOutputStream stream;
	protected FileChannel channel;
	protected ByteBuffer buffer;
	protected static final Charset charset = Charset.forName("ASCII");
	
	public FileCollector(final String path) throws IOException {
		File file = new File(path);
		
		if (file.exists()) {
			System.err.println("Warning : overwriting output file "+path);
		}
		
		stream = new FileOutputStream(file, false);
		channel = stream.getChannel();
		
		buffer = ByteBuffer.allocateDirect(BUFFER_CAPACITY);
		buffer.clear();
	}
	
	public final void collect(final int support, final int[] pattern) {
		putInt(support);
		safePut((byte) '\t'); // putChar('\t') would append TWO bytes, but in ASCII we need only one
		
		boolean addSeparator = false;
		for (int item : pattern) {
			if (addSeparator) {
				safePut((byte) ' ');
			} else {
				addSeparator = true;
			}
			
			putItem(item);
		}
		
		safePut((byte) '\n');
		this.collected++;
		this.collectedLength += pattern.length;
	}
	
	protected void putItem(final int i) {
		putInt(i);
	}
	
	protected final void putInt(final int i) {
		try {
			byte[] asBytes = Integer.toString(i).getBytes(charset);
			buffer.put(asBytes);
		} catch (BufferOverflowException e) {
			flush();
			putInt(i);
		}
	}
	
	protected final void safePut(final byte b) {
		try {
			buffer.put(b);
		} catch (BufferOverflowException e) {
			flush();
			safePut(b);
		}
	}
	
	protected final void flush() {
		try {
			buffer.flip();
			channel.write(buffer);
			buffer.clear();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}

	public final long close() {
		try {
			flush();
			channel.close();
			stream.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		
		return this.collected;
	}

	public final int getAveragePatternLength() {
		if (this.collected == 0) {
			return 0;
		} else {
			return (int) (this.collectedLength / this.collected);
		}
	}
	
	/**
	 * @return how many patterns have been written so far
	 */
	public final long getCollected() {
		return collected;
	}
	
	/**
	 * @return sum of collected patterns' lengths
	 */
	public final long getCollectedLength() {
		return collectedLength;
	}
}
