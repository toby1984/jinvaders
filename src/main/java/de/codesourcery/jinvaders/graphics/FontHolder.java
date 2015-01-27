package de.codesourcery.jinvaders.graphics;

import java.awt.Font;

public final class FontHolder {

	public final Object image;

	public FontHolder(Object image)
	{
		if ( image == null || ! (image instanceof Font ) ) {
			throw new IllegalArgumentException();
		}
		this.image=image;
	}

	public int getSize() {
		return ((Font) image).getSize();
	}
}