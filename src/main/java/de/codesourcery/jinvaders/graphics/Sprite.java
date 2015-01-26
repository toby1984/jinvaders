package de.codesourcery.jinvaders.graphics;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public interface Sprite
{
	public Vec2d size();

	public BufferedImage image();

	public void render(Graphics2D graphics,Vec2d position);

	public void render(Graphics2D graphics,int x,int y);
}
