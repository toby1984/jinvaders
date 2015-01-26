/**
 * Copyright 2015 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.jinvaders.util;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public final class KeyboardInput extends KeyAdapter
{
	private static final int MAX_KEYBOARD_BUFFER_SIZE = 255;

	private final Set<Integer> PRESSED_KEYS = new HashSet<>();
	private final StringBuilder buffer = new StringBuilder();

	// start: KeyAdapter methods
	@Override
	public void keyPressed(KeyEvent e) {
		PRESSED_KEYS.add( e.getKeyCode() );
	}

	@Override
	public void keyReleased(KeyEvent e) {
		PRESSED_KEYS.remove( e.getKeyCode() );
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		synchronized(buffer)
		{
			if ( e.getKeyChar() != KeyEvent.CHAR_UNDEFINED )
			{
				if ( buffer.length() < MAX_KEYBOARD_BUFFER_SIZE ) {
					buffer.append( e.getKeyChar() );
				}
			}
		}
	}

	// end: KeyAdapter methods

	public boolean isPressed(int keyCode)
	{
		return PRESSED_KEYS.contains( keyCode );
	}

	public void flushKeyboardBuffer()
	{
		synchronized(buffer)
		{
			buffer.setLength(0);
			PRESSED_KEYS.clear();
		}
	}

	public int maybeReadKey()
	{
		synchronized(buffer)
		{
			if ( buffer.length() == 0 ) {
				return -1;
			}
			final int result = buffer.charAt(0);
			buffer.deleteCharAt(0);
			return result;
		}
	}

}
