package de.codesourcery.jinvaders.graphics;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public interface IRenderer
{
	public void initialize(Object context);

	public FontHolder getFont(FontKey key);

	public ImageHolder convertImage(BufferedImage image);

	public void setColor(int color);

	public void setBackgroundColor(int color);

	public void setFont(FontHolder font);

	public void drawString(String text,int x,int y);

	public Rectangle getStringBounds(String text);

	public void clearScreen();

	public void fillRect(int x,int y,int width,int height);

	public void drawRect(int x,int y,int width,int height);

	public void renderPoint(int x,int y,int color);

	public void renderPoint(ImageHolder holder,int x,int y,int color);

	public int queryPoint(ImageHolder holder,int x,int y);

	public void renderImage(ImageHolder image,int x,int y);

	public void begin();

	public void end(Object context);

	public float getAscent(String text);

	public float getDescent(String text);

	public float getFPS();
}