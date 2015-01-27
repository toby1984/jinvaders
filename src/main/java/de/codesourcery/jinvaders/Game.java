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
package de.codesourcery.jinvaders;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import de.codesourcery.jinvaders.entity.Barricade;
import de.codesourcery.jinvaders.entity.Bullet;
import de.codesourcery.jinvaders.entity.Entity;
import de.codesourcery.jinvaders.entity.ITickContext;
import de.codesourcery.jinvaders.entity.Invader;
import de.codesourcery.jinvaders.entity.ParticleSystem;
import de.codesourcery.jinvaders.entity.ParticleSystem.ParticleEffect;
import de.codesourcery.jinvaders.entity.Player;
import de.codesourcery.jinvaders.graphics.Sprite;
import de.codesourcery.jinvaders.graphics.SpriteImpl;
import de.codesourcery.jinvaders.graphics.SpriteKey;
import de.codesourcery.jinvaders.graphics.SpriteRepository;
import de.codesourcery.jinvaders.graphics.Vec2d;
import de.codesourcery.jinvaders.particles.ParticlePool;
import de.codesourcery.jinvaders.sound.SoundEffect;
import de.codesourcery.jinvaders.util.KeyboardInput;

public final class Game extends JPanel
{
	protected final SpriteRepository spriteRepository = new SpriteRepository();

	protected final List<HighscoreEntry> highscores = new ArrayList<>();

	protected GameStateImpl gameState = GameState.SHOW_HIGHSCORES.newInstance();

	protected final Vec2d currentInvaderVelocity = new Vec2d(Constants.INITIAL_INVADER_VELOCITY);

	protected Player player;

	protected int ticksTillDifficultyIncrease=Constants.DIFFICULITY_INCREASE_AFTER_TICKS;

	// list holding all non-static (moving) game entities
	protected final List<Entity> nonStaticEntities = new ArrayList<>();

	// list holding all ITickListener that are NOT entities
	protected final List<ITickListener> pureTickListeners = new ArrayList<>();

	protected final List<Barricade> barricades = new ArrayList<>();

	protected final KeyboardInput keyboardInput = new KeyboardInput();

	// number of invaders that are still alive
	protected int invadersRemaining;

	// the current tick
	protected int currentTick;
	protected float elapsedTimeInSeconds;

	// the current difficulty level
	protected int difficulty = 1;

	// player receives bonus if destroying a wave
	// before DIFFICULITY_INCREASE_AFTER_TICKS have elapsed
	protected boolean eligibleForBonus;

	// background buffer that we're going to render to
	private final Object backgroundBufferLock = new Object();

	private BufferedImage backgroundBuffer;
	private Graphics2D backgroundGraphics;

	// flag used to synchronize rendering thread (Swing EDT) with game loop execution
	// game loop will only advance after the current frame has been rendered
	public final AtomicBoolean screenRendered = new AtomicBoolean(false);

	// fonts
	private final Font defaultFont;
	private final Font bigDefaultFont;
	private final Font gameOverFont;

	private final ParticlePool particlePool = new ParticlePool(1000);

	// FPS calculation stuff
	private long appStartTime=System.currentTimeMillis(); // time that application got started
	private long framesRendered; // number of rendered frames since application start

	protected final ITickContext tickContext = new ITickContext()
	{
		@Override
		public void addTickListener(ITickListener listener)
		{
			if ( listener == null || listener instanceof Entity) { // entities registered through addNewEntity() get their tick() method called anyway so no need to register twice
				throw new IllegalArgumentException("Unsupported tick listener: "+listener);
			}
			pureTickListeners.add( listener );
		}

		@Override
		public void removeTickListener(ITickListener listener) {
			if ( listener == null ) {
				throw new IllegalArgumentException("listener must not be NULL");
			}
			pureTickListeners.remove( listener );
		}

		@Override
		public List<Entity> getNonStaticEntities() {
			return nonStaticEntities;
		}

		@Override
		public int getCurrentTick() {
			return currentTick;
		}

		@Override
		public void destroyEntity(Entity e) {
			removeEntity( e );
		}

		@Override
		public void addNewEntity(Entity e) {
			nonStaticEntities.add( e );
		}

		@Override
		public Sprite getSprite(SpriteKey key) {
			return spriteRepository.getSprite( key );
		}

		@Override
		public float getElapsedTimeInSeconds() {
			return elapsedTimeInSeconds;
		}
	};

	public Game()
	{
		setPreferredSize(Constants.SCREEN_SIZE);
		setSize(Constants.SCREEN_SIZE );

		// we're rendering to a background buffer anyway , disable double buffering
		setDoubleBuffered(false);

		// setup fonts
		final InputStream is = Main.class.getResourceAsStream("/PressStart2P.ttf");
		if ( is == null ) {
			throw new RuntimeException("Failed to load font '/PressStart2P.ttf'");
		}
		try {
			final Font font = Font.createFont(Font.TRUETYPE_FONT, is);
			defaultFont  = font.deriveFont(12f);
			bigDefaultFont  = font.deriveFont(18f);
			gameOverFont = font.deriveFont(32f);
		}
		catch (final Exception e) {
			throw new RuntimeException("Failed to load font",e);
		}

		reset();

		addKeyListener(keyboardInput);
	}

	public List<HighscoreEntry> getHighscores() {
		return highscores;
	}

	public Player getPlayer() {
		return player;
	}

	public void startGame() {
		setGameState( GameState.PLAYING );
	}

	public void setGameState(GameState newState)
	{
		this.gameState.state.assertTransitionValid( newState );

		GameState state = newState.onEnter( this.gameState.state , this );
		while ( state != newState )
		{
			final GameState tmp = newState;
			newState = state;
			state = state.onEnter( tmp , this );
		}
		this.gameState = state.newInstance();

		// flush keyboard buffer so new game state starts off with a clean slate
		getKeyboardInput().flushKeyboardBuffer();
	}

	public KeyboardInput getKeyboardInput() {
		return keyboardInput;
	}

	public void reset()
	{
		eligibleForBonus = true;
		appStartTime=System.currentTimeMillis(); // time that application got started
		framesRendered = 0; // number of rendered frames since application start

		currentTick = 0;

		currentInvaderVelocity.set(Constants.INITIAL_INVADER_VELOCITY);

		ticksTillDifficultyIncrease=Constants.DIFFICULITY_INCREASE_AFTER_TICKS;
		difficulty = 1;

		final int playerX = Constants.VIEWPORT.x + Constants.VIEWPORT.width /2;

		final Sprite playerSprite = sprite( SpriteImpl.PLAYER );
		final int playerY = Constants.VIEWPORT.y + Constants.VIEWPORT.height - playerSprite.size().height();
		player = new Player( new Vec2d( playerX , playerY ) , playerSprite );

		pureTickListeners.clear();

		nonStaticEntities.forEach( Entity::onDispose );
		nonStaticEntities.clear();
		nonStaticEntities.add( player );

		spawnInvaders();
		spawnBarricades();
	}

	private void spawnBarricades()
	{
		barricades.forEach( Entity::onDispose );
		barricades.clear();

		final int widthPerBarricade = Constants.VIEWPORT.width/ Constants.BARRICADE_COUNT;

		final Sprite barricadeSprite = sprite( SpriteImpl.BARRICADE );
		final Sprite playerSprite = sprite( SpriteImpl.PLAYER );
		final Sprite playerBulletSprite = sprite( SpriteImpl.PLAYER_BULLET );

		int xOffset = Constants.VIEWPORT.x + widthPerBarricade/2 - barricadeSprite.size().width()/2;

		final int yOffset = Constants.VIEWPORT.y + Constants.VIEWPORT.height - playerSprite.size().height() - barricadeSprite.size().height() - playerBulletSprite.size().height();

		for ( int x = 0 ; x < Constants.BARRICADE_COUNT ; x++ )
		{
			barricades.add( new Barricade(new Vec2d(xOffset,yOffset), barricadeSprite) );
			xOffset += widthPerBarricade;
		}
	}

	private void spawnInvaders()
	{
		final Sprite invaderSprite = sprite( SpriteImpl.INVADER );

		final int requiredWidth = Constants.INVADERS_PER_ROW * invaderSprite.size().width() + (Constants.INVADERS_PER_ROW-1) * invaderSprite.size().width();
		final int remainingWidth = Constants.VIEWPORT.width - requiredWidth;
		final int xStartingOffset = Constants.VIEWPORT.x + Math.max( 0 ,  remainingWidth/2 );
		final int yStartingOffset = Constants.VIEWPORT.y + invaderSprite.size().height();
		for ( int x = 0 ; x < Constants.INVADERS_PER_ROW ; x++ )
		{
			for ( int y = 0 ; y < Constants.ROWS_OF_INVADERS ; y++ )
			{
				final int xStart = xStartingOffset + x * invaderSprite.size().width() + x*invaderSprite.size().width()/2;
				final int yStart = yStartingOffset + y * invaderSprite.size().height() + (int) (y*invaderSprite.size().height()*0.3f);
				nonStaticEntities.add( new Invader(new Vec2d(xStart,yStart) , new Vec2d( currentInvaderVelocity.x , 0 ) , invaderSprite ) );
			}
		}
		invadersRemaining = Constants.INVADERS_PER_ROW * Constants.ROWS_OF_INVADERS;
	}

	public GameState getGameState() {
		return gameState.state;
	}

	public void advanceGameState()
	{
		// remove dead entities
		nonStaticEntities.removeIf( entity -> {
			if ( entity.isDead() ) {
				entity.onDispose();
				return true;
			}
			return false;
		});

		maybeFlipInvaderMovementDirection();

		new ArrayList<>(pureTickListeners).forEach( e -> e.tick(tickContext ) );

		// tick all entities, need to iterate over a copy here since
		// Entity#tick() might add/remove entities from the collection
		// while we're iterating (and this would cause a ConcurrentModificationException)
		new ArrayList<>( nonStaticEntities ).forEach( e -> e.tick(tickContext) );

		// find colliding entities (can only be bullet<->player or bullet<->invader since bullets cannot collide with each other)
		final List<Entity> collidingEntities = nonStaticEntities.stream().filter( a -> a.isAlive() && a.collidesWith(nonStaticEntities) ).collect(Collectors.toList());

		// find entities that are off-screen
		final List<Entity> toRemove = nonStaticEntities.stream().filter( entity -> entity.destroyWhenOffScreen() & entity.isOffScreen( Constants.VIEWPORT )  ).collect(Collectors.toList());

		if ( collidingEntities.contains( player ) ) // must be player <-> bullet collision
		{
			player.onHit(tickContext);

			if ( player.lifes == 0 )
			{
				setGameState( GameState.GAME_OVER );
				SoundEffect.GAME_OVER.play();
			} else {
				SoundEffect.ONE_LIFE_LOST.play();
				collidingEntities.remove(player);
				toRemove.addAll( collidingEntities );
			}
		} else {
			toRemove.addAll( collidingEntities );
		}

		if ( gameState.state != GameState.GAME_OVER )
		{
			final List<Entity> destroyedInvaders = toRemove.stream().filter( Entity::isInvader ).collect(Collectors.toList() );
			destroyedInvaders.forEach( e -> e.onHit(tickContext) );

			destroyedInvaders.forEach( invader ->
			{
				final ParticleEffect effect = new ParticleEffect( invader.position , 100 , 0.6f );
				nonStaticEntities.add( new ParticleSystem( particlePool , effect ) );
			});
			toRemove.removeIf( e -> e.isInvader() & ! e.isDead() );

			final long invadersDestroyed = destroyedInvaders.size();

			// increase player score
			final float percentage = Math.max(0.1f, ticksTillDifficultyIncrease / (float) Constants.DIFFICULITY_INCREASE_AFTER_TICKS);
			player.increaseScore( (int) (invadersDestroyed * 100 * difficulty *percentage) );

			for ( int i = 0 ; i < invadersDestroyed ; i++ ) {
				SoundEffect.INVADER_DESTROYED.play();
			}

			invadersRemaining -= invadersDestroyed;

			ticksTillDifficultyIncrease--;
			if ( invadersRemaining <= 0 ||  ticksTillDifficultyIncrease <= 0  ) { // no more invaders left, increase difficulty and spawn new invaders
				increaseDifficulty();
				if ( invadersRemaining <= 0 )
				{
					if ( eligibleForBonus ) {
						SoundEffect.WAVE_COMPLETED.play();
						player.increaseScore( 10000 );
					}
					eligibleForBonus = true;
					spawnInvaders();
					spawnBarricades();
				} else {
					eligibleForBonus = false;
				}
			}
		}

		// discard all bullets that collided with either the player or an invader
		removeEntities( toRemove );

		// check the remaining bullets for collisions with barricades
		final List<Entity> bullets =  nonStaticEntities.stream().filter( Entity::isBullet ).collect(Collectors.toList());
		final List<Entity> bulletsToRemove = new ArrayList<>();

		outer:
			for ( final Entity bullet: bullets )
			{
				for ( final Barricade barricade : barricades )
				{
					if ( barricade.hitBy( (Bullet) bullet ) ) {
						bulletsToRemove.add( bullet );
						continue outer;
					}
				}
			}

		removeEntities( bulletsToRemove );
	}

	private void maybeFlipInvaderMovementDirection()
	{
		// find left-most and right-most invader

		Invader rightMost = null;
		Invader leftMost = null;
		for ( final Entity e : nonStaticEntities ) {
			if ( e.isInvader() ) {
				if ( rightMost == null || e.position.x > rightMost.position.x ) {
					rightMost = (Invader) e;
				}
				if ( leftMost == null || e.position.x < leftMost.position.x ) {
					leftMost = (Invader) e;
				}
			}
		}

		// flip invader movement direction if either the left-most or right-most
		// hit the screen border
		if ( ( leftMost != null && leftMost.left() < 0 ) || ( rightMost != null && rightMost.right() > Constants.VIEWPORT.getMaxX() ) )
		{
			nonStaticEntities.stream().filter( Entity::isInvader ).forEach( invader ->
			{
				if ( invader.bottom() < Constants.VIEWPORT.y+Constants.VIEWPORT.height*0.8 ) {
					invader.position.y += 2;
				}
				invader.velocity.x = -invader.velocity.x;
			});
		}
	}

	protected void increaseDifficulty()
	{
		currentInvaderVelocity.x += 1;
		currentInvaderVelocity.y += 2;
		difficulty+=1;
		ticksTillDifficultyIncrease = Constants.DIFFICULITY_INCREASE_AFTER_TICKS;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		synchronized(backgroundBufferLock)
		{
			if ( backgroundGraphics == null )
			{
				gameState.render( this );
			}
			g.drawImage( backgroundBuffer , 0 , 0 , null );
			framesRendered++;
		}
		screenRendered.set(true);
	}

	private static final DecimalFormat FLOAT_FORMAT = new DecimalFormat("####0.0#");
	private static final DecimalFormat PLAYER_SCORE_FORMAT = new DecimalFormat("0000000");

	private static String formatFloat(double value) {
		return FLOAT_FORMAT.format( value );
	}

	private static String formatScore(int score) {
		return PLAYER_SCORE_FORMAT.format( score );
	}

	private Sprite sprite(SpriteKey key) {
		return spriteRepository.getSprite( key );
	}

	public void render(Consumer<Graphics2D> callback)
	{
		synchronized(backgroundBufferLock)
		{
			// if not done yet, setup background buffer to render to
			if ( backgroundGraphics == null ) {
				backgroundBuffer = getGraphicsConfiguration().createCompatibleImage( Constants.SCREEN_SIZE.width ,  Constants.SCREEN_SIZE.height );
				backgroundGraphics = backgroundBuffer.createGraphics();
				backgroundGraphics.setFont(defaultFont);
			}

			final Graphics2D g = backgroundGraphics;

			// clear screen
			g.clearRect(0, 0, Constants.SCREEN_SIZE.width, Constants.SCREEN_SIZE.height);

			callback.accept( g );
		}
	}

	public void renderEntities(Graphics2D g)
	{
		// render all game entities

		// sort entities by draw order
		Collections.sort( nonStaticEntities);
		nonStaticEntities.forEach( e -> e.render(backgroundGraphics) );

		// sort by draw order
		barricades.forEach( e -> e.render(backgroundGraphics ) );
	}

	public void renderHud(Graphics2D g) {

		g.setColor(Color.WHITE);

		final int FONT_HEIGHT = 20;
		final int X_OFFSET = Constants.VIEWPORT.x + 10;

		int y = FONT_HEIGHT+10;

		// render difficulty level
		g.setFont( bigDefaultFont );
		g.drawString("Level: "+difficulty, X_OFFSET ,y);

		// render player score
		{
			final String score = "Score: "+formatScore( player.score );
			final Rectangle2D metrics = g.getFontMetrics().getStringBounds(score, g );
			g.drawString( score , (int) (Constants.VIEWPORT.getMaxX() - metrics.getWidth() ) ,y);
			g.setFont( defaultFont );
		}

		y= y + (int) (bigDefaultFont.getSize()*2.5f);

		// render lifes left
		{
			final String text= "Lifes:";
			final Rectangle2D metrics =  g.getFontMetrics().getStringBounds(text, g);

			final LineMetrics lineMetrics = g.getFontMetrics().getLineMetrics(text, g );

			final int fontX = X_OFFSET;
			final int fontY = (int) (y -lineMetrics.getAscent() );

			g.drawString( text , fontX ,fontY);

			final Sprite sprite = sprite( SpriteImpl.LIFE_ICON );
			int iconX =  (int) (fontX + metrics.getWidth() + 10 );
			final int iconY = fontY - sprite.size().height()/2 - 5;
			for ( int i = 0 ; i < player.lifes ; i++ )
			{
				sprite.render(g, iconX , iconY );
				iconX += sprite.size().width() + 10;
			}
		}

		// render time till next difficulty level
		{
			final String text= "Time remaining";
			final Rectangle2D metrics =  g.getFontMetrics().getStringBounds(text, g);

			final LineMetrics lineMetrics = g.getFontMetrics().getLineMetrics(text, g );

			final int fontX = Constants.VIEWPORT.x+Constants.VIEWPORT.width - (int) metrics.getWidth() - 10;
			final int fontY = (int) (y -lineMetrics.getAscent() );

			final float remainingTimePercentage = ticksTillDifficultyIncrease / (float) Constants.DIFFICULITY_INCREASE_AFTER_TICKS;
			final int barWidth = (int) ( metrics.getWidth() * remainingTimePercentage);

			g.setColor( remainingTimePercentage > 0.2f ? Color.GREEN : Color.RED );

			g.fillRect( fontX-2 , fontY-2 , barWidth+2 , 3 + (int) lineMetrics.getHeight() );

			g.setColor(Color.WHITE);
			g.drawString( text , fontX ,y);
		}

		y+= FONT_HEIGHT;

		// render FPS
		final float secondsSinceAppStart = (System.currentTimeMillis()-appStartTime)/1000f;
		final float fps = framesRendered / secondsSinceAppStart;
		g.drawString("FPS: "+formatFloat( fps ), X_OFFSET ,y);
		y+= FONT_HEIGHT;
	}

	public void renderGameOverText(Graphics2D g)
	{
		g.setColor(Color.RED);
		g.setFont( gameOverFont );

		final String text = "GAME OVER !!!";
		final Rectangle2D metrics = g.getFontMetrics().getStringBounds(text, g );

		final int textX = (int) (Constants.SCREEN_SIZE.width/2 - metrics.getWidth()/2);
		final int textY = (int) (Constants.SCREEN_SIZE.height/2 - metrics.getHeight()/2 + g.getFontMetrics().getDescent() );
		g.drawString(text,textX,textY);
		g.setFont( defaultFont );
	}

	public void renderHighscoreList(Graphics2D g)
	{
		final int width = (int) (Constants.SCREEN_SIZE.width*0.7f);
		final int height = (int) (Constants.SCREEN_SIZE.height*0.6f);

		final int x0 = ( Constants.SCREEN_SIZE.width - width ) / 2;
		final int y0 = ( Constants.SCREEN_SIZE.height - height) / 2;

		final Rectangle2D stringBounds = renderCenteredTextWindow( "Highscores" ,x0,y0,width,height,g);

		Collections.sort( highscores );

		final int lineHeight = defaultFont.getSize()+6;
		int y = (int) (y0 + stringBounds.getHeight() + lineHeight*2 );
		for ( int i = 0 ; i < 10 ; i++ )
		{
			String text;
			if ( i < highscores.size() ) {
				text = highscores.get(i).toString();
			} else {
				text = new HighscoreEntry("",0).toString();
			}
			renderCenteredText(text, g, x0 , y  , width, lineHeight );
			y += lineHeight;
		}

		y += lineHeight;
		g.setColor( Color.RED );
		renderCenteredText("Press <ENTER> to play", g, x0 , y  , width, lineHeight );
	}

	private Rectangle renderCenteredTextWindow(String text, int x0,int y0 , int width , int height ,Graphics2D g)
	{
		// clear window
		g.setColor(Color.BLACK);
		g.fillRect( x0 ,y0 , width , height );

		// draw border
		g.setColor(Color.WHITE);
		g.drawRect( x0 ,y0 , width , height );

		g.setFont( bigDefaultFont );
		final Rectangle rect = renderCenteredText( text , g , x0 , y0+10 , width , bigDefaultFont.getSize()+4 );

		g.setFont( defaultFont );
		return rect;
	}

	public void renderEnterHighscore(Graphics2D g, String playerName,int score)
	{
		final int width = (int) (Constants.SCREEN_SIZE.width*0.7f);
		final int height = (int) (Constants.SCREEN_SIZE.height*0.6f);

		final int x0 = ( Constants.SCREEN_SIZE.width - width ) / 2;
		final int y0 = ( Constants.SCREEN_SIZE.height - height) / 2;

		final Rectangle rect = renderCenteredTextWindow( "New highscore" ,x0,y0,width,height,g);

		g.drawString( HighscoreEntry.format( playerName,score ) , x0 , (int) (rect.getMaxY()+rect.getHeight()) );
	}

	private Rectangle renderCenteredText(String text, Graphics2D g, int x, int y , int width, int height ) {

		final Rectangle2D metrics =  g.getFontMetrics().getStringBounds(text, g);

		final int centerX = x + width/2;
		final int centerY = y + height/2;

		final int textWidth = (int) metrics.getWidth();
		final int textHeight = (int) metrics.getHeight();

		final int fontX = centerX - textWidth/2;
		final int fontY = centerY - textHeight/2 + 1 + (int) metrics.getHeight();

		g.drawString( text , fontX ,fontY);
//		return new Rectangle(x, (int) (y + metrics.getY()) , (int) metrics.getWidth()  , (int) metrics.getHeight() );
		return new Rectangle(x, y , (int) metrics.getWidth()  , (int) metrics.getHeight() );
	}

	private void removeEntity(Entity toRemove) {
		toRemove.onDispose();
		nonStaticEntities.remove( toRemove );
	}

	private void removeEntities(Collection<Entity> toRemove) {
		toRemove.forEach( Entity::onDispose );
		nonStaticEntities.removeAll( toRemove );
	}

	public void tick(float elapsedSeconds)
	{
		currentTick++;

		elapsedTimeInSeconds = elapsedSeconds;

		// advance game state
		gameState.tick(this , tickContext );

		// update screen
		gameState.render(this);
	}
}