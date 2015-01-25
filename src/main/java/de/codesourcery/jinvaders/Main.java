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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Main
{
	protected static final Dimension SCREEN_SIZE = new Dimension(800,600);

	protected static final Dimension VIEWPORT_SIZE = new Dimension(800,500);

	protected static final int INVADERS_PER_ROW = 8;

	protected static final int ROWS_OF_INVADERS = 5;

	protected static final Vec2d INITIAL_INVADER_VELOCITY = new Vec2d(2,1);
	protected static final Vec2d CURRENT_INVADER_VELOCITY = new Vec2d(INITIAL_INVADER_VELOCITY);

	protected static final int INVADER_BULLET_VELOCITY = 4;

	protected static final int PLAYER_VELOCITY = 3;

	protected static final int PLAYER_BULLET_VELOCITY = 4;

	public static final Vec2d PLAYER_SIZE = new Vec2d(32,32);

	public static final Vec2d BULLET_SIZE = new Vec2d(6,14);

	public static final Vec2d INVADER_SIZE = new Vec2d(32,32);

	protected static final int TICKS_PER_SHOT_LIMIT = 10;

	protected static final int MAX_PLAYER_BULLETS_IN_FLIGHT = 1;

	protected static final int TICKS_PER_SECOND = 60;

	protected static final int DIFFICULITY_INCREASE_AFTER_TICKS = 30 * TICKS_PER_SECOND;

	protected static final List<Entity> entities = new ArrayList<>();

	protected static final Set<Integer> PRESSED_KEYS = new HashSet<>();

	protected static Player player;

	protected static int remainingInvaders;
	protected static int currentTick;
	protected static int ticksTillDifficultyIncrease=DIFFICULITY_INCREASE_AFTER_TICKS;

	protected static int difficulty = 1;

	protected static int playerBulletsInFlight;

	protected static boolean gameOver;

	protected static void playSound(SoundEffect effect)
	{
		effect.play();
	}

	protected static enum Volume
	{
		LOUD(0.9f),
		NOT_SO_LOUD(0.7f),
		QUIET(0.5f);

		public final float gainPercentage;

		private Volume(float gainPercentage) { this.gainPercentage = gainPercentage; }
	}

	public static enum SoundEffect
	{
		PLAYER_SHOOTING("/player_laser.wav",Volume.LOUD , 3),
		INVADER_SHOOTING("/invader_laser.wav",Volume.QUIET , 3),
		GAME_OVER("/gameover.wav",Volume.NOT_SO_LOUD, 1),
		INVADER_DESTROYED("/invader_destroyed.wav",Volume.LOUD, 3);

		private final List<Clip> clips=new ArrayList<>();

		private final int concurrency;
		private int currentClipIdx;

		private SoundEffect(String soundFileName,Volume volume,int concurrency)
		{
			this.concurrency = concurrency;
			try
			{
				final URL url = Main.class.getResource(soundFileName);
				if ( url == null ) {
					throw new RuntimeException("Failed to locate sound on classpath: '"+soundFileName+"'");
				}
				for ( int i = 0 ; i < concurrency ; i++ )
				{
					clips.add(createClip( AudioSystem.getAudioInputStream(url) , volume ));
				}
			}
			catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}

		private Clip createClip(AudioInputStream stream,Volume volume)
		{
			try {
				final Clip clip = AudioSystem.getClip();
				clip.open( stream );

				final FloatControl masterGain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

				final float range = masterGain.getMaximum() - masterGain.getMinimum();
				final float value = masterGain.getMinimum() + range*volume.gainPercentage;
				masterGain.setValue( value );
				return clip;
			} catch (final Exception e) {
				throw new RuntimeException("Failed to obtain audio line",e);
			}
		}

		public void play()
		{
			currentClipIdx = (currentClipIdx+1) % concurrency;
			final Clip clip = clips.get(currentClipIdx);
			if ( ! clip.isActive() ) {
				clip.setFramePosition(0);
				clip.start();
			}
		}
	}

	protected static class Vec2d
	{
		protected static final Vec2d ZERO = new Vec2d(0,0);

		public int x;
		public int y;

		public Vec2d(Vec2d other) { x = other.x ; y = other.y; }

		public Vec2d(int x,int y) { this.x = x; this.y = y; }

		public void set(int x,int y) { this.x = x; this.y = y; }
		public void set(Vec2d other) { x = other.x ; y = other.y; }

		public void add(Vec2d that) { x += that.x ; y += that.y; }
		public void sub(Vec2d that) { x -= that.x ; y -= that.y; }
		public int width() { return x; }
		public int height() { return y; }
		@Override
		public String toString() { return "( "+x+" , "+y+" )"; }
	}

	protected static abstract class Entity
	{
		public Vec2d position;
		public Vec2d velocity;
		public Vec2d size;

		public Entity(Vec2d position,Vec2d velocity,Vec2d size)
		{
			this.velocity = new Vec2d(velocity);
			this.position = new Vec2d(position);
			this.size = new Vec2d(size);
		}

		@Override
		public String toString() { return getClass().getSimpleName()+" @ "+position; }

		public boolean isInvader() { return this instanceof Invader; }
		public boolean isPlayer() { return this instanceof Player; }
		public boolean isBullet() { return this instanceof Bullet; }

		public boolean isOutOfScreen(int width,int height) { return bottom() < 0 || top() > height|| right() < 0 || left() > width; }

		public void stop() { velocity.set(0,0); }
		public int left() { return position.x; }
		public int right() { return position.x+size.width(); }
		public int top() { return position.y; }
		public int bottom() { return position.y+size.height(); }

		public boolean isAbove(int y) { return bottom() < y; }
		public boolean isBelow(int y) { return top() > y; }
		public boolean isLeftOf(int x) { return right() < x; }
		public boolean isRightOf(int x) { return left() > x; }

		public boolean isAbove(Entity e) { return isAbove( e.top() ); }
		public boolean isBelow(Entity e) { return isBelow( e.bottom() ); }
		public boolean isLeftOf(Entity e) { return isLeftOf( e.left() ); }
		public boolean isRightOf(Entity e) { return isRightOf( e.right() ); }

		public boolean isMovingUp() { return velocity.y < 0; }
		public boolean isMovingDown() { return velocity.y > 0; }

		public boolean collidesWith(Collection<Entity> others) { return others.stream().anyMatch( this::collides ); }

		public boolean collides(Entity other) { return this != other &&
				!( isBullet() && other.isBullet() ) &&
				!(isAbove(other) || isBelow(other) || isLeftOf(other) || isRightOf(other ) ); }

		public void moveLeft(int vx) { this.velocity.x = -vx; };
		public void moveRight(int vx) { this.velocity.x = vx; };

		public void tick() { position.add( velocity ); }

		public void draw(Graphics2D graphics) {
			graphics.fillRect( position.x - size.width() /2 , position.y - size.height() / 2 , size.width() , size.height() );
		}
	}

	protected static final class Player extends Entity
	{
		public int tickAtLastShot=-1;
		public int score;

		public Player(Vec2d position) { super(position,Vec2d.ZERO,PLAYER_SIZE); }

		@Override
		public void tick()
		{
			super.tick();
			if ( isOutOfScreen( VIEWPORT_SIZE.width , VIEWPORT_SIZE.height ) ) {
				velocity.x = -velocity.x;
				position.add( velocity );
			}
		}

		public void increaseScore(int value) { score += value; }

		public int ticksSinceLastShot(int currentTick) { return tickAtLastShot > 0 ? currentTick - tickAtLastShot : Integer.MAX_VALUE; }

		@Override
		public void draw(Graphics2D graphics) {
			Sprite.PLAYER_SPRITE.render( graphics , position );
		}

		public void shoot()
		{
			if ( ticksSinceLastShot( currentTick ) > TICKS_PER_SHOT_LIMIT && playerBulletsInFlight < MAX_PLAYER_BULLETS_IN_FLIGHT )
			{
				final int bulletX = left() + size.width()/2;
//				final int bulletY = top() - BULLET_SIZE.height();
				final int bulletY = top() - BULLET_SIZE.height();
				final Vec2d pos = new Vec2d( bulletX, bulletY );
				entities.add( new Bullet( pos , new Vec2d( 0 , -PLAYER_BULLET_VELOCITY ) , true ) );
				tickAtLastShot = currentTick;
				playerBulletsInFlight++;
				playSound( SoundEffect.PLAYER_SHOOTING );
			}
		}
	}

	protected static final class Bullet extends Entity
	{
		public final boolean shotByPlayer;

		public Bullet(Vec2d position,Vec2d velocity,boolean shotByPlayer) {
			super(position,velocity, BULLET_SIZE );
			this.shotByPlayer = shotByPlayer;
		}

		@Override
		public void draw(Graphics2D graphics)
		{
			if ( isMovingDown() ) {
				Sprite.INVADER_BULLET_SPRITE.render( graphics , position );
			} else {
				Sprite.PLAYER_BULLET_SPRITE.render( graphics , position );
			}
		}
	}

	protected static final class Invader extends Entity
	{
		protected static final Random random = new Random(System.currentTimeMillis());

		public Invader(Vec2d position,Vec2d velocity) { super(position,velocity,INVADER_SIZE); }

		@Override
		public void tick()
		{
			super.tick();
			if ( noOtherInvaderBelow() && random.nextFloat() > 0.995 )
			{
				entities.add( new Bullet( new Vec2d( position.x , position.y + 5 + size.height() ) , new Vec2d( 0 , INVADER_BULLET_VELOCITY ) , false ) );
				SoundEffect.INVADER_SHOOTING.play();
			}
		}

		@Override
		public void draw(Graphics2D graphics) {
			Sprite.INVADER_SPRITE.render( graphics , position );
		}

		private boolean noOtherInvaderBelow()
		{
			return entities.stream().noneMatch( entity ->
			entity != this &&
			entity.isInvader() &&
			entity.isBelow( this ) &&
			( ( entity.left() >= left() && entity.left() <= right() ) ||
					( entity.right() >= left() && entity.right() <= right() )
					) );
		}
	}

	protected static enum Sprite
	{
		PLAYER_SPRITE("/player_sprite.png"),
		INVADER_SPRITE("/invader_sprite.png"),
		PLAYER_BULLET_SPRITE("/player_bullet_sprite.png"),
		INVADER_BULLET_SPRITE("/invader_bullet_sprite.png");

		private final String resource;
		private BufferedImage image;

		private Sprite(String resource)
		{
			this.resource = resource;
		}

		private void loadImage() {
			final InputStream in = Main.class.getResourceAsStream(resource);
			try {
				image = ImageIO.read( in );
			} catch (final IOException e) {
				throw new RuntimeException("Failed to load sprite '"+resource+"'");
			}
		}

		public void render(Graphics2D graphics,Vec2d position)
		{
			render(graphics,position.x,position.y);
		}

		public void render(Graphics2D graphics,int x,int y)
		{
			if ( image == null ) {
				loadImage();
			}
			graphics.drawImage( image , x , y , null );
		}
	}

	protected static final class Game extends JPanel
	{
		private final Object BACKGROUND_BUFFER_LOCK = new Object();
		private BufferedImage backgroundBuffer;
		private Graphics2D backgroundGraphics;

		public final AtomicBoolean screenRendered = new AtomicBoolean(false);

		private final Font defaultFont;
		private final Font bigDefaultFont;
		private final Font gameOverFont;

		private final long appStartTime=System.currentTimeMillis();
		private long framesRendered;

		public Game()
		{
			setPreferredSize(SCREEN_SIZE);
			setSize(SCREEN_SIZE );
			setDoubleBuffered(false);

			final InputStream is = Main.class.getResourceAsStream("/PressStart2P.ttf");
			if ( is == null ) {
				throw new RuntimeException("Failed to load font '/PressStart2P.ttf'");
			}
			try {
				final Font font = Font.createFont(Font.TRUETYPE_FONT, is);
				defaultFont  = font.deriveFont(12f);
				bigDefaultFont  = font.deriveFont(18f);
				gameOverFont = font.deriveFont(32f);
			} catch (final Exception e) {
				throw new RuntimeException("Failed to load font",e);
			}

			init();

			addKeyListener( new KeyAdapter()
			{
				@Override
				public void keyPressed(KeyEvent e)
				{
					if ( gameOver && e.getKeyChar() == KeyEvent.VK_ENTER )
					{
						init();
					} else {
						PRESSED_KEYS.add( e.getKeyCode() );
					}
				}

				@Override
				public void keyReleased(KeyEvent e) {
					PRESSED_KEYS.remove( e.getKeyCode() );
				}
			} );
		}

		private void init()
		{
			currentTick = 0;

			CURRENT_INVADER_VELOCITY.set(INITIAL_INVADER_VELOCITY);

			ticksTillDifficultyIncrease=DIFFICULITY_INCREASE_AFTER_TICKS;
			difficulty = 1;
			playerBulletsInFlight=0;
			gameOver = false;

			final int playerX = VIEWPORT_SIZE.width /2;
			final int playerY = VIEWPORT_SIZE.height - PLAYER_SIZE.height();
			player = new Player( new Vec2d( playerX , playerY )  );

			entities.clear();
			entities.add( player );
			spawnInvaders();
		}

		private void spawnInvaders()
		{
			final int requiredWidth = INVADERS_PER_ROW * INVADER_SIZE.width() + (INVADERS_PER_ROW-1) * INVADER_SIZE.width();
			final int remainingWidth = VIEWPORT_SIZE.width - requiredWidth;
			final int xStartingOffset = Math.max( 0 ,  remainingWidth/2 );
			final int yStartingOffset = INVADER_SIZE.height();
			for ( int x = 0 ; x < INVADERS_PER_ROW ; x++ )
			{
				for ( int y = 0 ; y < ROWS_OF_INVADERS ; y++ )
				{
					final int xStart = xStartingOffset + x * INVADER_SIZE.width() + x*INVADER_SIZE.width()/2;
					final int yStart = yStartingOffset + y * INVADER_SIZE.height() + (int) (y*INVADER_SIZE.height()*0.3f);
					entities.add( new Invader(new Vec2d(xStart,yStart) , new Vec2d( CURRENT_INVADER_VELOCITY.x , 0 ) ) );
				}
			}
			remainingInvaders = INVADERS_PER_ROW * ROWS_OF_INVADERS;
		}

		public void processInput()
		{
			if ( PRESSED_KEYS.contains( KeyEvent.VK_A) ) {
				player.moveLeft( PLAYER_VELOCITY );
			}
			else if ( PRESSED_KEYS.contains( KeyEvent.VK_D ) ) {
				player.moveRight( PLAYER_VELOCITY );
			} else {
				player.stop();
			}
			if ( PRESSED_KEYS.contains( KeyEvent.VK_SPACE ) )
			{
				player.shoot();
			}
		}

		public void tick()
		{
			if ( gameOver )
			{
				render();
				return;
			}
			currentTick++;

			// handle keyboard input
			processInput();

			// find left-most and right-most invader
			Invader rightMost = null;
			Invader leftMost = null;
			for ( final Entity e : entities ) {
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
			if ( ( leftMost != null && leftMost.left() < 0 ) || ( rightMost != null && rightMost.right() > VIEWPORT_SIZE.width ) )
			{
				entities.stream().filter( Entity::isInvader ).forEach( invader ->
				{
					if ( invader.bottom() < VIEWPORT_SIZE.width*0.8 ) {
						invader.position.y += 2;
					}
					invader.velocity.x = -invader.velocity.x;
				});
			}

			// tick all entities, need to iterate over a copy here since
			// Entity#tick() might add/remove entities from the collection
			// while we're iterating (and this would cause a ConcurrentModificationException)
			new ArrayList<>( entities ).forEach( Entity::tick );

			// remove colliding entities (can only be bullet<->player or bullet<->invader since bullets cannot collide with each other)
			final List<Entity> colliding = entities.stream().filter( a -> a.collidesWith(entities) ).collect(Collectors.toList());
			final List<Entity> offScreen = entities.stream().filter( entity -> entity.isOutOfScreen( VIEWPORT_SIZE.width , VIEWPORT_SIZE.height )  ).collect(Collectors.toList());

			final List<Entity> toRemove = offScreen;
			if ( colliding.contains( player ) )
			{
				gameOver = true;
				playSound(SoundEffect.GAME_OVER);
			} else {
				toRemove.addAll( colliding );
			}

			if ( ! gameOver )
			{
				playerBulletsInFlight -= toRemove.stream().filter( e -> e.isBullet() && ((Bullet) e).shotByPlayer ).count();

				final long invadersDestroyed = toRemove.stream().filter( Entity::isInvader ).count();

				// increase player score
				final float percentage = Math.max(0.1f, ticksTillDifficultyIncrease / (float) DIFFICULITY_INCREASE_AFTER_TICKS);
				player.increaseScore( (int) (invadersDestroyed * 100 * difficulty *percentage) );

				for ( int i = 0 ; i < invadersDestroyed ; i++ ) {
					playSound( SoundEffect.INVADER_DESTROYED );
				}

				remainingInvaders -= invadersDestroyed;

				ticksTillDifficultyIncrease--;
				if ( remainingInvaders <= 0 ||  ticksTillDifficultyIncrease <= 0  ) { // no more invaders left, increase difficulty and spawn new invaders
					increaseDifficulty();
					if ( remainingInvaders <= 0 ) {
						spawnInvaders();
					}
				}
			}

			// remove all entities that are either destroyed or off-screen
			entities.removeAll( toRemove );

			// render to background buffer
			render();
		}

		protected void increaseDifficulty()
		{
			CURRENT_INVADER_VELOCITY.x += 1;
			CURRENT_INVADER_VELOCITY.y += 2;
			difficulty+=1;
			ticksTillDifficultyIncrease = DIFFICULITY_INCREASE_AFTER_TICKS;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			synchronized(BACKGROUND_BUFFER_LOCK)
			{
				if ( backgroundGraphics == null )
				{
					render( );
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

		private void render()
		{
			synchronized(BACKGROUND_BUFFER_LOCK)
			{
				// if not done yet, setup background buffer to render to
				if ( backgroundGraphics == null ) {
					backgroundBuffer = getGraphicsConfiguration().createCompatibleImage( SCREEN_SIZE.width ,  SCREEN_SIZE.height );
					backgroundGraphics = backgroundBuffer.createGraphics();
					backgroundGraphics.setFont(defaultFont);
				}
				final Graphics2D g = backgroundGraphics;

				// clear screen
				g.clearRect(0, 0, SCREEN_SIZE.width, SCREEN_SIZE.height);

				// render all game entities
				entities.forEach( e -> e.draw(backgroundGraphics) );

				g.setColor(Color.WHITE);

				// render line separating playing field from score board
				g.fillRect(0,VIEWPORT_SIZE.height + 3 , VIEWPORT_SIZE.width , 3 );

				final int FONT_HEIGHT = 20;
				final int X_OFFSET = 10;

				int y = VIEWPORT_SIZE.height+FONT_HEIGHT+10;

				// render difficulty level
				g.setFont( bigDefaultFont );
				g.drawString("Level: "+difficulty, X_OFFSET ,y);

				// render player score
				final String score = "Score: "+formatScore( player.score );
				FontMetrics fontMetrics = g.getFontMetrics();
				Rectangle2D metrics = fontMetrics.getStringBounds(score, g );
				g.drawString( score , (int) (VIEWPORT_SIZE.width - metrics.getWidth() ) ,y);

				g.setFont( defaultFont );
				y= y + (int) (bigDefaultFont.getSize()*1.4f);

				// render number of remaining invaders
				g.drawString("Invaders remaining: "+remainingInvaders, X_OFFSET,y);
				y+= FONT_HEIGHT;

				// render time till next difficulty level
				String text= "Time remaining: ";
				metrics = fontMetrics.getStringBounds(text, g);

				g.drawString( text , X_OFFSET,y);

				final float remainingTimePercentage = ticksTillDifficultyIncrease / (float) DIFFICULITY_INCREASE_AFTER_TICKS;
				final int barWidth = (int) (100 * remainingTimePercentage);

				g.setColor( remainingTimePercentage > 0.2f ? Color.GREEN : Color.RED );

				g.fillRect( (int) (X_OFFSET+metrics.getWidth()) , (int) (y - metrics.getHeight()) , barWidth , 10 );

				g.setColor(Color.WHITE);

				y+= FONT_HEIGHT;

				// render FPS
				final float secondsSinceAppStart = (System.currentTimeMillis()-appStartTime)/1000f;
				final float fps = framesRendered / secondsSinceAppStart;
				g.drawString("FPS: "+formatFloat( fps ), X_OFFSET ,y);
				y+= FONT_HEIGHT;

				if ( gameOver ) // render game over text
				{
					g.setColor(Color.RED);
					g.setFont( gameOverFont );

					text = "GAME OVER !!!";
					fontMetrics = g.getFontMetrics();
					metrics = fontMetrics.getStringBounds(text, g );

					final int textX = (int) (VIEWPORT_SIZE.width/2 - metrics.getWidth()/2);
					final int textY = (int) (VIEWPORT_SIZE.height/2 - metrics.getHeight()/2 + fontMetrics.getDescent() );
					g.drawString(text,textX,textY);
					g.setFont( defaultFont );
				}
			}
		}
	}

	public static void main(String[] args)
	{
		final Game game = new Game();

		// setup game screen
		final JFrame frame = new JFrame("JavaInvaders");
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		frame.getContentPane().add(game);
		frame.pack();
		frame.setVisible(true);

		// Swing thread/timer that redraws game screen
		new Timer( 1000 / TICKS_PER_SECOND , event -> game.repaint() ).start();

		// Thread that runs game loop
		final Thread gameLoop = new Thread( () ->
		{
			while(true)
			{
				// busy-wait until EDT has redrawn screen
				while( ! game.screenRendered.compareAndSet(true,false) ) { }

				// advance game
				game.tick();
			}
		});

		gameLoop.setDaemon(true);
		gameLoop.start();

		// request focus so that game screen receives keyboard input
		game.requestFocus();
	}
}