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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.font.LineMetrics;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Main
{
	// total size of screen
	protected static final Dimension SCREEN_SIZE = new Dimension(800,600);

	// part of screen where we're going to draw the game action
	// (=> screen minus HUD below it)
	protected static final Rectangle VIEWPORT = new Rectangle(0,100,800,SCREEN_SIZE.height - 100 );

	protected static final int INVADERS_PER_ROW = 8;

	protected static final int ROWS_OF_INVADERS = 5;

	protected static final int BARRICADE_COUNT = 5;

	protected static final Color BACKGROUND_COLOR = Color.BLACK;

	// entity velocities
	protected static final Vec2d INITIAL_INVADER_VELOCITY = new Vec2d(2,1);
	protected static final Vec2d CURRENT_INVADER_VELOCITY = new Vec2d(INITIAL_INVADER_VELOCITY);
	protected static final int INVADER_BULLET_VELOCITY = 4;
	protected static final int PLAYER_VELOCITY = 3;
	protected static final int PLAYER_BULLET_VELOCITY = 4;

	protected static final double INVADER_FIRING_PROBABILITY = 0.995; // 0.995;

	protected static final int PLAYER_LIFES = 3;

	// number of ticks that needs to be elapsed between subsequent shots by the player
	protected static final int TICKS_PER_SHOT_LIMIT = 10;

	protected static final int MAX_PLAYER_BULLETS_IN_FLIGHT = 1;

	// FPS target
	protected static final int TICKS_PER_SECOND = 60;

	// time (in ticks) after which we're going to automatically bump the difficulty
	// if the player fails to destroy all invaders on the current level
	protected static final int DIFFICULITY_INCREASE_AFTER_TICKS = 30 * TICKS_PER_SECOND;

	protected static int ticksTillDifficultyIncrease=DIFFICULITY_INCREASE_AFTER_TICKS;

	// list holding all non-static (moving) game entities
	protected static final List<Entity> nonStaticEntities = new ArrayList<>();

	protected static final List<Barricade> barricades = new ArrayList<>();

	// set holding the key codes of all keys that are currently pressed
	protected static final Set<Integer> PRESSED_KEYS = new HashSet<>();

	protected static Player player;

	// number of invaders that are still alive
	protected static int invadersRemaining;

	// the current tick
	protected static int currentTick;

	// the current difficulty level
	protected static int difficulty = 1;

	// flag indicating whether the game is over
	protected static boolean gameOver;

	/**
	 * Sound volume levels we're using.
	 *
	 * @author tobias.gierke@code-sourcery.de
	 */
	public static enum Volume
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
		ONE_LIFE_LOST("/one_life_lost.wav",Volume.LOUD , 3),
		INVADER_SHOOTING("/invader_laser.wav",Volume.QUIET , 3),
		GAME_OVER("/gameover.wav",Volume.NOT_SO_LOUD, 1),
		INVADER_DESTROYED("/invader_destroyed.wav",Volume.LOUD, 3);

		private final Clip[] clips;

		private final int concurrency; // maximum number of instances of this sound that may be playing at the same time
		private int currentClipIdx;

		private SoundEffect(String soundFileName,Volume volume,int concurrency)
		{
			this.concurrency = concurrency;
			this.clips = new Clip[ concurrency ];
			try
			{
				final URL url = Main.class.getResource(soundFileName);
				if ( url == null ) {
					throw new RuntimeException("Failed to locate sound on classpath: '"+soundFileName+"'");
				}
				for ( int i = 0 ; i < concurrency ; i++ )
				{
					clips[i] = createClip( AudioSystem.getAudioInputStream(url) , volume );
				}
			}
			catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}

		private Clip createClip(AudioInputStream stream,Volume volume) throws LineUnavailableException, IOException
		{
			final Clip clip = AudioSystem.getClip();
			clip.open( stream );

			final FloatControl masterGain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

			final float range = masterGain.getMaximum() - masterGain.getMinimum();
			final float value = masterGain.getMinimum() + range*volume.gainPercentage;
			masterGain.setValue( value );
			return clip;
		}

		public void play()
		{
			currentClipIdx = (currentClipIdx+1) % concurrency;
			final Clip clip = clips[ currentClipIdx ];
			if ( ! clip.isActive() ) {
				clip.setFramePosition(0);
				clip.start();
			}
		}
	}

	public static class Vec2d
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

	public static enum EntityState
	{
		ALIVE{
			@Override
			public boolean canTransitionTo(EntityState other) {
				return other == DYING;
			}
		},
		DYING{
			@Override
			public boolean canTransitionTo(EntityState other) {
				return other == DEAD;
			}
		},
		DEAD;

		public boolean canTransitionTo(EntityState other) { return false; }
	}

	public static abstract class Entity
	{
		public final Vec2d position;
		public final Vec2d velocity;
		public final Vec2d size;

		private EntityState state=EntityState.ALIVE;

		public boolean isFlashing;
		public boolean flashingState;
		public int flashingTicksRemaining;

		public Entity(Vec2d position,Vec2d velocity,Vec2d size)
		{
			this.velocity = new Vec2d(velocity);
			this.position = new Vec2d(position);
			this.size = new Vec2d(size);
		}

		public void setState(EntityState newState)
		{
			if ( ! this.state.canTransitionTo( newState ) ) {
				throw new IllegalStateException("Invalid state transition for entity "+this+": "+this.state+" -> "+newState);
			}
			this.state = newState;
		}

		public void onDispose() {
		}

		public void entityHit() {
		}

		public void flash()
		{
			this.isFlashing = true;
			this.flashingTicksRemaining=30;
			this.flashingState = true;
		}

		protected void renderMaybeFlashing(Sprite sprite,Graphics2D graphics)
		{
			renderMaybeFlashing( sprite.image , graphics );
		}

		protected void renderMaybeFlashing(BufferedImage image,Graphics2D graphics)
		{
			if ( isFlashing )
			{
				flashingTicksRemaining--;
				if ( flashingTicksRemaining == 0 ) {
					isFlashing=false;
				}

				if ( ( flashingTicksRemaining % 5 ) == 0 )
				{
					flashingState = ! flashingState;
				}

				if ( flashingState )
				{
					graphics.setColor(BACKGROUND_COLOR);
					graphics.fillRect( position.x , position.y , size.width() , size.height() );
				} else {
					graphics.drawImage( image , position.x , position.y , null );
				}
			} else {
				graphics.drawImage( image , position.x , position.y , null );
			}
		}

		public boolean isAlive() { return state == EntityState.ALIVE; }

		public boolean isDying() { return state == EntityState.DYING; }

		public boolean isDead() { return state == EntityState.DEAD; }

		@Override
		public String toString() { return getClass().getSimpleName()+" @ "+position; }

		public boolean isInvader() { return this instanceof Invader; }
		public boolean isPlayer() { return this instanceof Player; }
		public boolean isBullet() { return this instanceof Bullet; }

		public boolean isOutOfScreen(Rectangle r) {
			return bottom() < r.y || top() > r.y+r.height|| right() < r.x || left() > r.x+r.width;
		}

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

		public boolean canCollide() { return isAlive(); }

		public boolean collides(Entity other) { return this != other &&
				other.canCollide() &
				this.canCollide() &&
				!(isAbove(other) || isBelow(other) || isLeftOf(other) || isRightOf(other ) ); }

		public void moveLeft(int vx) { this.velocity.x = -vx; };
		public void moveRight(int vx) { this.velocity.x = vx; };

		public void tick() { position.add( velocity ); }

		public abstract void render(Graphics2D graphics);
	}

	public static final class Player extends Entity
	{
		public int tickAtLastShot=-1;
		public int score;
		public int playerBulletsInFlight;
		public int lifes = PLAYER_LIFES;

		public Player(Vec2d position) { super(position,Vec2d.ZERO,Sprite.PLAYER.size); }

		@Override
		public void tick()
		{
			super.tick();
			if ( isOutOfScreen( VIEWPORT ) ) {
				velocity.x = -velocity.x;
				position.add( velocity );
			}
		}

		@Override
		public void entityHit()
		{
			lifes--;
			if ( lifes > 0 ) {
				flash();
			}
		}

		public void increaseScore(int value) { score += value; }

		public int ticksSinceLastShot(int currentTick) { return tickAtLastShot > 0 ? currentTick - tickAtLastShot : Integer.MAX_VALUE; }

		@Override
		public void render(Graphics2D graphics) {
			renderMaybeFlashing(Sprite.PLAYER, graphics);
		}

		public void shoot()
		{
			if ( ticksSinceLastShot( currentTick ) > TICKS_PER_SHOT_LIMIT && playerBulletsInFlight < MAX_PLAYER_BULLETS_IN_FLIGHT )
			{
				final int bulletX = left() + size.width()/2;
				final int bulletY = top() - Sprite.PLAYER_BULLET.size.height();
				final Vec2d pos = new Vec2d( bulletX, bulletY );
				nonStaticEntities.add( new Bullet( pos , new Vec2d( 0 , -PLAYER_BULLET_VELOCITY ) , true ) );
				tickAtLastShot = currentTick;
				playerBulletsInFlight++;
				SoundEffect.PLAYER_SHOOTING.play();
			}
		}
	}

	public static final class Barricade extends Entity {

		private final BufferedImage sprite;

		public Barricade(Vec2d position,Sprite sprite) {
			super(position, Vec2d.ZERO, Sprite.BARRICADE.size );
			this.sprite = new BufferedImage( sprite.image.getWidth() , sprite.image.getHeight() , sprite.image.getType());
			final Graphics2D graphics = this.sprite.createGraphics();
			graphics.drawImage( sprite.image , 0 , 0 , null );
			graphics.dispose();
		}

		public boolean hitBy(Bullet entity)
		{
			if ( ! collides( entity) ) {
				return false;
			}

			// find intersecting rectangle
			final Rectangle r1 = new Rectangle(position.x,position.y,size.width(),size.height());
			final Rectangle r2 = new Rectangle(entity.position.x,entity.position.y,entity.size.width(),entity.size.height());

			final Rectangle intersection = r1.intersection(r2); // note: intersection can never be empty unless the collides(Entity) method is broken
			if ( intersection.isEmpty() ) {
				return false;
			}

			final int yIncrement;
			final int yStart; // in local coordinates !
			final int yEnd; // in local coordinates !
			final Function<Integer,Boolean> condition;
			if ( entity.isMovingDown() )
			{
				// check starts at top
				yIncrement=1;
				yStart = intersection.y - position.y;
				yEnd = intersection.y + intersection.height - position.y;
				condition = y -> y < sprite.getHeight();
			}
			else if ( entity.isMovingUp() )
			{
				// check starts at bottom
				yIncrement=-1;
				yStart = intersection.y + intersection.height - position.y - 1;
				yEnd = intersection.y - position.y;
				condition = y -> y >= 0;
			} else {
				throw new RuntimeException("Internal error, bullet is moving neither up nor down ??");
			}

			// clear pixels that were hit
			boolean pixelsHit=false;

			final int xStart = intersection.x - position.x;
			final int xEnd = intersection.x + intersection.width - position.x;

			if ( yStart < 0 || yStart > sprite.getHeight() ) {
				throw new RuntimeException("Invalid yStart: "+yStart+" (max. "+sprite.getHeight()+")");
			}
			if ( yEnd < 0 || yEnd > sprite.getHeight() ) {
				throw new RuntimeException("Invalid yEnd: "+yEnd+" (max. "+sprite.getHeight()+")");
			}

			if ( xStart < 0 || xStart > sprite.getWidth() ) {
				throw new RuntimeException("Invalid xStart: "+xStart+" (max. "+sprite.getWidth()+")");
			}
			if ( xEnd < 0 || xEnd > sprite.getWidth() ) {
				throw new RuntimeException("Invalid xEnd: "+xEnd+" (max. "+sprite.getWidth()+")");
			}

			for ( int y = yStart ; condition.apply(y) && ! pixelsHit ; y += yIncrement )
			{
				for ( int x = xStart ; x < xEnd ; x++ )
				{
					final int rgb = sprite.getRGB( x ,y ) & 0x00ffffff; // ignore alpha channel
					if ( rgb != 0 ) {
						pixelsHit=true; // stop deleting pixels after finishing this row, we'll only remove the top-most/bottom-most row of pixels on each hit
						sprite.setRGB( x , y , 0 );
					}
				}
			}
			return pixelsHit;
		}

		@Override
		public void render(Graphics2D graphics) {
			renderMaybeFlashing( sprite , graphics);
		}
	}

	public static final class Bullet extends Entity
	{
		public final boolean shotByPlayer;

		public Bullet(Vec2d position,Vec2d velocity,boolean shotByPlayer) {
			super(position,velocity, shotByPlayer ? Sprite.PLAYER_BULLET.size : Sprite.INVADER_BULLET.size );
			this.shotByPlayer = shotByPlayer;
		}

		@Override
		public void onDispose()
		{
			super.onDispose();
			if ( shotByPlayer ) {
				player.playerBulletsInFlight--;
			}
		}

		@Override
		public boolean collides(Entity other)
		{
			if ( other.isBullet() ) { // bullets can not collide with other bullets
				return false;
			}
			// prevent invaders from being killed by their collegues
			// slightly hackish but Invaders#noInvaderBelow() check does
			// prevent invaders below and slightly to the left/right of the firing one
			// to be hit when they move left/right on the next tick
			if ( other.isInvader() && ! shotByPlayer ) {
				return false;
			}
			return super.collides(other);
		}

		@Override
		public String toString() {
			return "Bullet( player: "+shotByPlayer+" , pos: "+position+")";
		}

		@Override
		public void render(Graphics2D graphics)
		{
			if ( isMovingDown() ) {
				Sprite.INVADER_BULLET.render( graphics , position );
			} else {
				Sprite.PLAYER_BULLET.render( graphics , position );
			}
		}
	}

	public static final class Invader extends Entity
	{
		protected static final Random random = new Random(System.currentTimeMillis());

		public Invader(Vec2d position,Vec2d velocity) { super(position,velocity,Sprite.INVADER.size); }

		@Override
		public void entityHit()
		{
			setState( EntityState.DYING );
			flash();
		}

		@Override
		public boolean collides(Entity other)
		{
			// prevent invaders from being killed by their collegues
			// slightly hackish but Invaders#noInvaderBelow() check does
			// prevent invaders below and slightly to the left/right of the firing one
			// to be hit when they move left/right on the next tick
			if ( other.isBullet() && !( (Bullet) other).shotByPlayer )  {
				return false;
			}
			return super.collides(other);
		}

		@Override
		public void tick()
		{
			super.tick();
			if ( noOtherInvaderBelow() && random.nextFloat() > INVADER_FIRING_PROBABILITY )
			{
				nonStaticEntities.add( new Bullet( new Vec2d( position.x , position.y + 5 + size.height() ) , new Vec2d( 0 , INVADER_BULLET_VELOCITY ) , false ) );
				SoundEffect.INVADER_SHOOTING.play();
			}
		}

		@Override
		public void render(Graphics2D graphics)
		{
			renderMaybeFlashing(Sprite.INVADER, graphics);
		}

		private boolean noOtherInvaderBelow()
		{
			return nonStaticEntities.stream().noneMatch( entity ->
			entity != this &&
			entity.isInvader() &&
			entity.isBelow( this ) &&
			( ( entity.left() >= left()-10 && entity.left() <= right()+10 ) || ( entity.right() >= left()-10 && entity.right() <= right()+10 ) ) );
		}
	}

	public static enum Sprite
	{
		PLAYER("/player_sprite.png"),
		LIFE_ICON("/player_sprite.png"),
		INVADER("/invader_sprite.png"),
		PLAYER_BULLET("/player_bullet_sprite.png"),
		BARRICADE("/barricade_sprite.png"),
		INVADER_BULLET("/invader_bullet_sprite.png");

		public final BufferedImage image;
		public final Vec2d size;

		private Sprite(String resource)
		{
			final InputStream in = Main.class.getResourceAsStream(resource);
			try {
				image = ImageIO.read( in );
				size = new Vec2d( image.getWidth() , image.getHeight() );
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
			graphics.drawImage( image , x , y , null );
		}
	}

	public static final class Game extends JPanel
	{
		// background buffer that we're going to render to
		private final Object BACKGROUND_BUFFER_LOCK = new Object();
		private BufferedImage backgroundBuffer;
		private Graphics2D backgroundGraphics;

		// flag used to synchronize rendering thread (Swing EDT) with game loop execution
		// game loop will only advance after the current frame has been rendered
		public final AtomicBoolean screenRendered = new AtomicBoolean(false);

		// fonts
		private final Font defaultFont;
		private final Font bigDefaultFont;
		private final Font gameOverFont;

		// FPS calculation stuff
		private long appStartTime=System.currentTimeMillis(); // time that application got started
		private long framesRendered; // number of rendered frames since application start

		public Game()
		{
			setPreferredSize(SCREEN_SIZE);
			setSize(SCREEN_SIZE );

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
			} catch (final Exception e) {
				throw new RuntimeException("Failed to load font",e);
			}

			reset();

			addKeyListener( new KeyAdapter()
			{
				@Override
				public void keyPressed(KeyEvent e)
				{
					if ( gameOver && e.getKeyChar() == KeyEvent.VK_ENTER )
					{
						reset();
					} else {
						PRESSED_KEYS.add( e.getKeyCode() );
					}
				}

				@Override
				public void keyReleased(KeyEvent e) { PRESSED_KEYS.remove( e.getKeyCode() ); }
			} );
		}

		private void reset()
		{
			appStartTime=System.currentTimeMillis(); // time that application got started
			framesRendered = 0; // number of rendered frames since application start

			currentTick = 0;

			CURRENT_INVADER_VELOCITY.set(INITIAL_INVADER_VELOCITY);

			ticksTillDifficultyIncrease=DIFFICULITY_INCREASE_AFTER_TICKS;
			difficulty = 1;
			gameOver = false;

			final int playerX = VIEWPORT.x + VIEWPORT.width /2;
			final int playerY = VIEWPORT.y + VIEWPORT.height - Sprite.PLAYER.size.height();
			player = new Player( new Vec2d( playerX , playerY )  );

			nonStaticEntities.clear();
			barricades.clear();

			nonStaticEntities.add( player );
			spawnInvaders();
			spawnBarricades();
		}

		private void spawnBarricades()
		{
			final int widthPerBarricade = VIEWPORT.width/ BARRICADE_COUNT;

			int xOffset = VIEWPORT.x + widthPerBarricade/2 - Sprite.BARRICADE.size.width()/2;

			final int yOffset = VIEWPORT.y + VIEWPORT.height - Sprite.PLAYER.size.height() - Sprite.BARRICADE.size.height() - Sprite.PLAYER_BULLET.size.height();

			for ( int x = 0 ; x < BARRICADE_COUNT ; x++ )
			{
				barricades.add( new Barricade(new Vec2d(xOffset,yOffset), Sprite.BARRICADE ) );
				xOffset += widthPerBarricade;
			}
		}

		private void spawnInvaders()
		{
			final int requiredWidth = INVADERS_PER_ROW * Sprite.INVADER.size.width() + (INVADERS_PER_ROW-1) * Sprite.INVADER.size.width();
			final int remainingWidth = VIEWPORT.width - requiredWidth;
			final int xStartingOffset = VIEWPORT.x + Math.max( 0 ,  remainingWidth/2 );
			final int yStartingOffset = VIEWPORT.y + Sprite.INVADER.size.height();
			for ( int x = 0 ; x < INVADERS_PER_ROW ; x++ )
			{
				for ( int y = 0 ; y < ROWS_OF_INVADERS ; y++ )
				{
					final int xStart = xStartingOffset + x * Sprite.INVADER.size.width() + x*Sprite.INVADER.size.width()/2;
					final int yStart = yStartingOffset + y * Sprite.INVADER.size.height() + (int) (y*Sprite.INVADER.size.height()*0.3f);
					nonStaticEntities.add( new Invader(new Vec2d(xStart,yStart) , new Vec2d( CURRENT_INVADER_VELOCITY.x , 0 ) ) );
				}
			}
			invadersRemaining = INVADERS_PER_ROW * ROWS_OF_INVADERS;
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

			// remove dead entities
			nonStaticEntities.removeIf( e -> e.isDead() || ( e.isDying() && ! e.isFlashing ) );

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
			if ( ( leftMost != null && leftMost.left() < 0 ) || ( rightMost != null && rightMost.right() > VIEWPORT.getMaxX() ) )
			{
				nonStaticEntities.stream().filter( Entity::isInvader ).forEach( invader ->
				{
					if ( invader.bottom() < VIEWPORT.y+VIEWPORT.height*0.8 ) {
						invader.position.y += 2;
					}
					invader.velocity.x = -invader.velocity.x;
				});
			}

			// tick all entities, need to iterate over a copy here since
			// Entity#tick() might add/remove entities from the collection
			// while we're iterating (and this would cause a ConcurrentModificationException)
			new ArrayList<>( nonStaticEntities ).forEach( Entity::tick );

			// find colliding entities (can only be bullet<->player or bullet<->invader since bullets cannot collide with each other)
			final List<Entity> colliding = nonStaticEntities.stream().filter( a -> a.collidesWith(nonStaticEntities) ).collect(Collectors.toList());

			// find entities that are off-screen
			final List<Entity> offScreen = nonStaticEntities.stream().filter( entity -> entity.isOutOfScreen( VIEWPORT )  ).collect(Collectors.toList());

			final List<Entity> toRemove = offScreen;
			if ( colliding.contains( player ) )
			{
				player.entityHit();

				if ( player.lifes == 0 )
				{
					gameOver = true;
					SoundEffect.GAME_OVER.play();
				} else {
					SoundEffect.ONE_LIFE_LOST.play();
					colliding.remove(player);
					toRemove.addAll( colliding );
				}
			} else {
				toRemove.addAll( colliding );
			}

			if ( ! gameOver )
			{
				final List<Entity> destroyedInvaders = toRemove.stream().filter( Entity::isInvader ).collect(Collectors.toList() );
				destroyedInvaders.forEach( Entity::entityHit );

				toRemove.removeIf( e -> e.isInvader() & ! e.isDead() );

				final long invadersDestroyed = destroyedInvaders.size();

				// increase player score
				final float percentage = Math.max(0.1f, ticksTillDifficultyIncrease / (float) DIFFICULITY_INCREASE_AFTER_TICKS);
				player.increaseScore( (int) (invadersDestroyed * 100 * difficulty *percentage) );

				for ( int i = 0 ; i < invadersDestroyed ; i++ ) {
					SoundEffect.INVADER_DESTROYED.play();
				}

				invadersRemaining -= invadersDestroyed;

				ticksTillDifficultyIncrease--;
				if ( invadersRemaining <= 0 ||  ticksTillDifficultyIncrease <= 0  ) { // no more invaders left, increase difficulty and spawn new invaders
					increaseDifficulty();
					if ( invadersRemaining <= 0 ) {
						spawnInvaders();
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

			// render to background buffer
			render();
		}

		private void removeEntities(Collection<Entity> toRemove) {
			toRemove.forEach( Entity::onDispose );
			nonStaticEntities.removeAll( toRemove );
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
				nonStaticEntities.forEach( e -> e.render(backgroundGraphics) );
				barricades.forEach( e -> e.render(backgroundGraphics ) );

				g.setColor(Color.WHITE);

				final int FONT_HEIGHT = 20;
				final int X_OFFSET = VIEWPORT.x + 10;

				int y = FONT_HEIGHT+10;

				// render difficulty level
				g.setFont( bigDefaultFont );
				g.drawString("Level: "+difficulty, X_OFFSET ,y);

				// render player score
				{
					final String score = "Score: "+formatScore( player.score );
					final Rectangle2D metrics = g.getFontMetrics().getStringBounds(score, g );
					g.drawString( score , (int) (VIEWPORT.getMaxX() - metrics.getWidth() ) ,y);
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

					int iconX =  (int) (fontX + metrics.getWidth() + 10 );
					final int iconY = fontY - Sprite.LIFE_ICON.size.height()/2 - 5;
					for ( int i = 0 ; i < player.lifes ; i++ )
					{
						Sprite.LIFE_ICON.render(g, iconX , iconY );
						iconX += Sprite.LIFE_ICON.size.width() + 10;
					}
				}

				// render time till next difficulty level
				{
					final String text= "Time remaining";
					final Rectangle2D metrics =  g.getFontMetrics().getStringBounds(text, g);

					final LineMetrics lineMetrics = g.getFontMetrics().getLineMetrics(text, g );

					final int fontX = VIEWPORT.x+VIEWPORT.width - (int) metrics.getWidth() - 10;
					final int fontY = (int) (y -lineMetrics.getAscent() );

					final float remainingTimePercentage = ticksTillDifficultyIncrease / (float) DIFFICULITY_INCREASE_AFTER_TICKS;
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

				if ( gameOver ) // render game over text
				{
					g.setColor(Color.RED);
					g.setFont( gameOverFont );

					final String text = "GAME OVER !!!";
					final Rectangle2D metrics = g.getFontMetrics().getStringBounds(text, g );

					final int textX = (int) (SCREEN_SIZE.width/2 - metrics.getWidth()/2);
					final int textY = (int) (SCREEN_SIZE.height/2 - metrics.getHeight()/2 + g.getFontMetrics().getDescent() );
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