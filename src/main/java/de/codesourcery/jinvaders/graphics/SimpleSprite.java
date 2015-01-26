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
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import de.codesourcery.jinvaders.Main;

public class SimpleSprite implements Sprite
{
	private final BufferedImage image;
	private final Vec2d size;

	public SimpleSprite(String resource)
	{
		final InputStream in = Main.class.getResourceAsStream(resource);
		try {
			image = ImageIO.read( in );
			size = new Vec2d( image.getWidth() , image.getHeight() );
		} catch (final IOException e) {
			throw new RuntimeException("Failed to load sprite '"+resource+"'");
		}
	}

	@Override
	public void render(Graphics2D graphics,Vec2d position)
	{
		render(graphics,position.x,position.y);
	}

	@Override
	public void render(Graphics2D graphics,int x,int y)
	{
		graphics.drawImage( image , x , y , null );
	}

	@Override
	public Vec2d size() { return size; }

	@Override
	public BufferedImage image() { return image; }
}
