package de.codesourcery.jinvaders.graphics;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.Collections;

import de.codesourcery.jinvaders.Constants;
import de.codesourcery.jinvaders.Game;
import de.codesourcery.jinvaders.HighscoreEntry;


public class UITheme {

	private static final DecimalFormat FLOAT_FORMAT = new DecimalFormat("####0.0#");
	private static final DecimalFormat PLAYER_SCORE_FORMAT = new DecimalFormat("0000000");

	private FontHolder defaultFont;
	private FontHolder bigDefaultFont;
	private FontHolder gameOverFont;

	private Game game;

	public UITheme()
	{
	}

	public void initialize(Game game , IRenderer renderer) {
		this.game = game;
		this.defaultFont = renderer.getFont(FontKey.DEFAULT);
		this.bigDefaultFont = renderer.getFont(FontKey.BIG);
		this.gameOverFont = renderer.getFont(FontKey.GAMEOVER);
	}

	public void renderHud(IRenderer g) {

		g.setColor( 0xffffff ); // WHITE

		final int FONT_HEIGHT = 20;
		final int X_OFFSET = Constants.VIEWPORT.x + 10;

		int y = FONT_HEIGHT+10;

		// render difficulty level
		g.setFont( bigDefaultFont );
		g.drawString("Level: "+game.difficulty, X_OFFSET ,y);

		// render player score
		{
			final String score = "Score: "+formatScore( game.player.score );
			final Rectangle metrics = g.getStringBounds(score);
			g.drawString( score , (int) (Constants.VIEWPORT.getMaxX() - metrics.getWidth() ) ,y);
			g.setFont( defaultFont );
		}

		y= y + (int) (bigDefaultFont.getSize()*2.5f);

		// render lifes left
		{
			final String text= "Lifes:";
			final Rectangle metrics =  g.getStringBounds(text);

			final int fontX = X_OFFSET;
			final int fontY = (int) (y - g.getAscent(text) );

			g.drawString( text , fontX ,fontY);

			final Sprite sprite = sprite( SpriteImpl.LIFE_ICON );
			int iconX =  (int) (fontX + metrics.getWidth() + 10 );
			final int iconY = fontY - sprite.size().height()/2 - 5;
			for ( int i = 0 ; i < game.player.lifes ; i++ )
			{
				sprite.render(g, iconX , iconY );
				iconX += sprite.size().width() + 10;
			}
		}

		// render time till next difficulty level
		{
			final String text= "Time remaining";
			final Rectangle2D metrics =  g.getStringBounds(text);

			final int fontX = Constants.VIEWPORT.x+Constants.VIEWPORT.width - (int) metrics.getWidth() - 10;
			final int fontY = (int) (y - g.getAscent(text) );

			final float remainingTimePercentage = game.ticksTillDifficultyIncrease / (float) Constants.DIFFICULITY_INCREASE_AFTER_TICKS;
			final int barWidth = (int) ( metrics.getWidth() * remainingTimePercentage);

			g.setColor( remainingTimePercentage > 0.2f ? 0x00ee00 : 0xff0000); // green or red

			g.fillRect( fontX-2 , fontY-2 , barWidth+2 , 3 + (int) metrics.getHeight() );

			g.setColor( 0xffffff ); // WHITE
			g.drawString( text , fontX ,y);
		}

		y+= FONT_HEIGHT;

		// render FPS
		g.drawString("FPS: "+formatFloat( g.getFPS() ), X_OFFSET ,y);
		y+= FONT_HEIGHT;
	}

	private Rectangle renderCenteredText(String text, int x, int y , int width, int height , IRenderer g) {

		final Rectangle2D metrics =  g.getStringBounds(text);

		final int centerX = x + width/2;
		final int centerY = y + height/2;

		final int textWidth = (int) metrics.getWidth();
		final int textHeight = (int) metrics.getHeight();

		final int fontX = centerX - textWidth/2;
		final int fontY = centerY - textHeight/2 + 1 + (int) metrics.getHeight();

		g.drawString( text , fontX ,fontY);
		return new Rectangle(x, y , (int) metrics.getWidth()  , (int) metrics.getHeight() );
	}

	public void renderEnterHighscore(String playerName,int score,IRenderer g)
	{
		final int width = (int) (Constants.SCREEN_SIZE.width*0.7f);
		final int height = (int) (Constants.SCREEN_SIZE.height*0.6f);

		final int x0 = ( Constants.SCREEN_SIZE.width - width ) / 2;
		final int y0 = ( Constants.SCREEN_SIZE.height - height) / 2;

		final Rectangle rect = renderCenteredTextWindow( "New highscore" ,x0,y0,width,height,g);

		g.drawString( HighscoreEntry.format( playerName,score ) , x0 , (int) (rect.getMaxY()+rect.getHeight()) );
	}

	private Rectangle renderCenteredTextWindow(String text, int x0,int y0 , int width , int height ,IRenderer g)
	{
		// clear window
		g.setColor( 0 ); // BLACK
		g.fillRect( x0 ,y0 , width , height );

		// draw border
		g.setColor( 0xffffff ); // WHITE
		g.drawRect( x0 ,y0 , width , height );

		g.setFont( bigDefaultFont );
		final Rectangle rect = renderCenteredText( text , x0 , y0+10 , width , bigDefaultFont.getSize()+4 , g );

		g.setFont( defaultFont );
		return rect;
	}

	public void renderGameOverText(IRenderer g)
	{
		g.setColor( 0xff0000 ); // RED
		g.setFont( gameOverFont );

		final String text = "GAME OVER !!!";
		final Rectangle2D metrics = g.getStringBounds(text);

		final int textX = (int) (Constants.SCREEN_SIZE.width/2 - metrics.getWidth()/2);
		final int textY = (int) (Constants.SCREEN_SIZE.height/2 - metrics.getHeight()/2 + g.getDescent(text) );
		g.drawString(text,textX,textY);
		g.setFont( defaultFont );
	}

	public void renderEntities(IRenderer g)
	{
		// render all game entities

		// sort entities by draw order
		Collections.sort( game.nonStaticEntities);
		game.nonStaticEntities.forEach( e -> e.render(g) );

		// sort by draw order
		game.barricades.forEach( e -> e.render(g ) );
	}

	public void renderHighscoreList(IRenderer g)
	{
		final int width = (int) (Constants.SCREEN_SIZE.width*0.7f);
		final int height = (int) (Constants.SCREEN_SIZE.height*0.6f);

		final int x0 = ( Constants.SCREEN_SIZE.width - width ) / 2;
		final int y0 = ( Constants.SCREEN_SIZE.height - height) / 2;

		final Rectangle2D stringBounds = renderCenteredTextWindow( "Highscores" ,x0,y0,width,height,g);

		Collections.sort( game.highscores );

		final int lineHeight = defaultFont.getSize()+6;
		int y = (int) (y0 + stringBounds.getHeight() + lineHeight*2 );
		for ( int i = 0 ; i < 10 ; i++ )
		{
			String text;
			if ( i < game.highscores.size() ) {
				text = game.highscores.get(i).toString();
			} else {
				text = new HighscoreEntry("",0).toString();
			}
			renderCenteredText(text, x0 , y  , width, lineHeight , g );
			y += lineHeight;
		}

		y += lineHeight;
		g.setColor( 0xff0000); // Color.RED
		renderCenteredText("Press <ENTER> to play", x0 , y  , width, lineHeight , g );
	}

	private Sprite sprite(SpriteKey key) {
		return game.spriteRepository.getSprite( key );
	}

	private static String formatFloat(double value) {
		return FLOAT_FORMAT.format( value );
	}

	private static String formatScore(int score) {
		return PLAYER_SCORE_FORMAT.format( score );
	}
}
