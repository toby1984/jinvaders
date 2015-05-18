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
package de.codesourcery.jinvaders.graphics;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public abstract class ImageHolder {

	public static ImageHolder newAWT(final BufferedImage image) {
		return new AWTImageHolder(image);
	}

	public abstract Object image();

	protected static final class AWTImageHolder extends ImageHolder
	{
		private final BufferedImage image;

		public AWTImageHolder(BufferedImage image) {
			this.image = image;
		}

		@Override
		public BufferedImage image() {
			return image;
		}

		@Override
		public ImageHolder createCopy() {
			final BufferedImage source = image;
			final BufferedImage copy= new BufferedImage( source.getWidth() , source.getHeight() , source.getType() );
			final Graphics2D graphics = copy.createGraphics();
			graphics.drawImage( source, 0 , 0 , null );
			graphics.dispose();
			return new AWTImageHolder(copy);
		}

		@Override
		public int getHeight() {
			return image.getHeight();
		}

		@Override
		public int getWidth() {
			return image.getWidth();
		}

		@Override
		public int getRGB(int x, int y) {
			return image.getRGB(x,y);
		}

		@Override
		public void setRGB(int x, int y, int color) {
			image.setRGB(x,y,color);
		}


	}

	private ImageHolder()
	{
	}

	public abstract ImageHolder createCopy();

	public abstract int getHeight();

	public abstract int getWidth();

	public abstract int getRGB(int x,int y);

	public abstract void setRGB(int x,int y,int color);
}