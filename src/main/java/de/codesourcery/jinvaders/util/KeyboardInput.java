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
			if ( buffer.length() < MAX_KEYBOARD_BUFFER_SIZE ) {
				buffer.append( e.getKeyChar() );
			}
			buffer.notifyAll();
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
		}
	}

	public char readKey()
	{
		synchronized(buffer)
		{
			while ( buffer.length() == 0 )
			{
				try {
					buffer.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			final char result = buffer.charAt(0);
			buffer.deleteCharAt(0);
			return result;
		}
	}

}
