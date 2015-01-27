package de.codesourcery.jinvaders.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.swing.JPanel;

import de.codesourcery.jinvaders.Constants;
import de.codesourcery.jinvaders.Main;

public final class AWTRenderer implements IRenderer {

	private final Object backgroundBufferLock= new Object();
	private Graphics2D g;
	private BufferedImage buffer;

	private final FontHolder defaultFont;
	private final FontHolder bigDefaultFont;
	private final FontHolder gameOverFont;

	private Color backgroundColor = Color.BLACK;

	private long appStartTime;
	private int framesRendered;

	public AWTRenderer()
	{
		final InputStream is = Main.class.getResourceAsStream("/PressStart2P.ttf");
		if ( is == null ) {
			throw new RuntimeException("Failed to load font '/PressStart2P.ttf'");
		}

		try {
			final Font font = Font.createFont(Font.TRUETYPE_FONT, is);
			defaultFont  = new FontHolder(font.deriveFont(12f));
			bigDefaultFont  = new FontHolder(font.deriveFont(18f));
			gameOverFont = new FontHolder(font.deriveFont(32f));
		}
		catch (final Exception e) {
			throw new RuntimeException("Failed to load font",e);
		}
	}

	@Override
	public FontHolder getFont(FontKey key)
	{
		switch(key) {
			case BIG:
				return bigDefaultFont;
			case DEFAULT:
				return defaultFont;
			case GAMEOVER:
				return gameOverFont;
			default:
				throw new RuntimeException("Unknown font key: "+key);
		}
	}

	@Override
	public ImageHolder convertImage(BufferedImage image) {
		return ImageHolder.newAWT( image );
	}

	@Override
	public void setColor(int color)
	{
		g.setColor( new Color( color ) );
	}

	@Override
	public void setBackgroundColor(int color) {
		g.setBackground( new Color(color ) );
	}

	@Override
	public void setFont(FontHolder font) {
		g.setFont( (Font) font.image );
	}

	@Override
	public void drawString(String text, int x, int y) {
		g.drawString(text, x, y);
	}

	@Override
	public void clearScreen()
	{
		g.setBackground( backgroundColor );
		g.clearRect(0, 0, Constants.SCREEN_SIZE.width, Constants.SCREEN_SIZE.height);
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		g.fillRect(x, y, width, height);
	}

	@Override
	public void drawRect(int x, int y, int width, int height) {
		g.drawRect(x, y, width, height );
	}

	@Override
	public void renderPoint(int x, int y, int color) {
		buffer.setRGB(x, y, color);
	}

	@Override
	public void renderImage(ImageHolder image, int x, int y)
	{
		g.drawImage((BufferedImage) image.image()  ,x,y , null );
	}

	@Override
	public void initialize(Object context)
	{
		synchronized(backgroundBufferLock)
		{
			// if not done yet, setup background buffer to render to
			if ( g == null )
			{
				System.out.println("Using renderer: AWT");

				this.appStartTime = System.currentTimeMillis();
				final JPanel peer = (JPanel) context;
				buffer = peer.getGraphicsConfiguration().createCompatibleImage( Constants.SCREEN_SIZE.width ,  Constants.SCREEN_SIZE.height );
				g = buffer.createGraphics();
				g.setFont( (Font) defaultFont.image );
			}
		}
	}

	@Override
	public void begin() {
	}

	@Override
	public void end(Object context) {
		((Graphics2D) context).drawImage( buffer , 0 , 0 , null );
		framesRendered++;
	}

	@Override
	public void renderPoint(ImageHolder holder, int x, int y, int color) {
		((BufferedImage) holder.image() ).setRGB(x,y,color);
	}

	@Override
	public int queryPoint(ImageHolder holder,int x,int y) {
		return ((BufferedImage) holder.image() ).getRGB(x,y);
	}

	@Override
	public Rectangle getStringBounds(String text)
	{
		final Rectangle2D bounds = g.getFontMetrics().getStringBounds( text ,  g );
		return new Rectangle(0,0,(int)bounds.getWidth() ,(int) bounds.getHeight() );
	}

	@Override
	public float getAscent(String text) {
		final LineMetrics metrics = g.getFontMetrics().getLineMetrics(text, g);
		return metrics.getAscent();
	}

	@Override
	public float getDescent(String text) {
		final LineMetrics metrics = g.getFontMetrics().getLineMetrics(text, g);
		return metrics.getDescent();
	}

	@Override
	public float getFPS()
	{
		final float secondsSinceAppStart = (System.currentTimeMillis()-appStartTime)/1000f;
		final float fps = framesRendered / secondsSinceAppStart;
		return fps;
	}
}