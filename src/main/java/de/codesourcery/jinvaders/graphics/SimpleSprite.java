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
