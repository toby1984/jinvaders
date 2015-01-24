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
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Main
{
	protected static final Dimension SCREEN_SIZE = new Dimension(640,480);

	protected static final int INVADERS_PER_ROW = 10;

	protected static final int ROWS_OF_INVADERS = 4;

	protected static final Vec2d INVADER_VELOCITY = new Vec2d(1,3);

	protected static final int INVADER_BULLET_VELOCITY = 2;

	protected static final int PLAYER_VELOCITY = 3;

	protected static final int PLAYER_BULLET_VELOCITY = 4;

	public static final Vec2d PLAYER_SIZE = new Vec2d(10,10);

	public static final Vec2d BULLET_SIZE = new Vec2d(3,7);

	public static final Vec2d INVADER_SIZE = new Vec2d(15,15);

	protected static final int TICKS_PER_SHOT_LIMIT = 10;

	protected static final int MAX_PLAYER_BULLETS_IN_FLIGHT = 10;

	protected static final int DIFFICULITY_INCREASE_AFTER_TICKS = 60 * 60;

	protected static final int TICKS_PER_SECOND = 60;

	protected static final List<Entity> entities = new ArrayList<>();

	protected static final Set<Integer> PRESSED_KEYS = new HashSet<>();

	protected static Player player;

	protected static int remainingInvaders;
	protected static int currentTick;
	protected static int ticksTillDifficultyIncrease=DIFFICULITY_INCREASE_AFTER_TICKS;

	protected static int difficulty = 1;

	protected static int playerBulletsInFlight;

	protected static boolean gameOver;

	protected static final Map<SoundEffect,ArrayBlockingQueue<SoundEffect>> SOUND_QUEUE = new HashMap<>();

	protected static final Object SOUND_QUEUE_NOT_EMPTY = new Object();
	static
	{
		final ThreadFactory tf = new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r)
			{
				final Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		};
		final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 24, TimeUnit.HOURS, new ArrayBlockingQueue<>(50),tf);

		final Thread t = new Thread( () ->
		{
			while(true)
			{
				try
				{
					final List<SoundEffect> toPlay = new ArrayList<>();
					synchronized(SOUND_QUEUE)
					{
						for ( final Entry<SoundEffect, ArrayBlockingQueue<SoundEffect>> entry : SOUND_QUEUE.entrySet() )
						{
							final SoundEffect effect = entry.getValue().poll();
							if ( effect != null ) {
								toPlay.add( effect );
							}
						}
					}
					if ( toPlay.isEmpty() )
					{
						synchronized(SOUND_QUEUE_NOT_EMPTY)
						{
							SOUND_QUEUE_NOT_EMPTY.wait();
							continue;
						}
					}
					toPlay.forEach( effect -> executor.submit( () -> effect.play() ) );
				}
				catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		} );
		t.setDaemon(true);
		t.start();
	}

	protected static void playSound(SoundEffect effect)
	{
		synchronized(SOUND_QUEUE)
		{
			ArrayBlockingQueue<SoundEffect> queue = SOUND_QUEUE.get(effect);
			if ( queue == null ) {
				queue = new ArrayBlockingQueue<>(20);
				SOUND_QUEUE.put(effect, queue);
			}
			queue.add( effect );
		}
		synchronized(SOUND_QUEUE_NOT_EMPTY)
		{
			SOUND_QUEUE_NOT_EMPTY.notifyAll();
		}
	}

	public static enum SoundEffect
	{
		PLAYER_SHOOTING("/lasergun.wav"),
		GAME_OVER("/gameover.wav"),
		INVADER_DESTROYED("/invader_destroyed.wav");

		private Clip clip;

		private final Object PLAY_LOCK = new Object();

		private SoundEffect(String soundFileName)
		{
			try
			{
				final URL url = Main.class.getResource(soundFileName);
				final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(url);
				clip = AudioSystem.getClip();
				clip.addLineListener( new LineListener() {

					@Override
					public void update(LineEvent event)
					{
						final LineEvent.Type eventType = event.getType();
						if ( eventType == LineEvent.Type.STOP )
						{
							synchronized(PLAY_LOCK) {
								PLAY_LOCK.notifyAll();
							}
						}
					}
				});
				clip.open(audioInputStream);
			}
			catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}

		public void play()
		{
			synchronized( PLAY_LOCK )
			{
				if ( clip.isActive() )
				{
					while ( clip.isActive() )
					{
						try {
							PLAY_LOCK.wait();
						} catch(final InterruptedException e) {
							e.printStackTrace();
						}
					}
					clip.flush();
				}
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
		public void add(Vec2d that) { x += that.x ; y += that.y; }
		public int width() { return x; }
		public int height() { return y; }
		@Override
		public String toString() {
			return "( "+x+" , "+y+" )";
		}
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
		public String toString() {
			return getClass().getSimpleName()+" @ "+position;
		}

		public boolean isInvader() { return this instanceof Invader; }
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

		public boolean collidesWith(Collection<Entity> others) { return others.stream().anyMatch( this::collides ); }

		public boolean collides(Entity other) { return this != other &&
				!( isBullet() && other.isBullet() ) &&
				!(isAbove(other) || isBelow(other) || isLeftOf(other) || isRightOf(other ) ); }

		public void moveLeft(int vx) { this.velocity.x = -vx; };
		public void moveRight(int vx) { this.velocity.x = vx; };

		public void tick() { position.add( velocity ); }

		public void draw(Graphics graphics) {
			graphics.fillRect( position.x - size.width() /2 , position.y - size.height() / 2 , size.width() , size.height() );
		}
	}

	protected static final class Player extends Entity
	{
		public int tickAtLastShot=-1;
		public Player(Vec2d position) { super(position,Vec2d.ZERO,PLAYER_SIZE); }

		@Override
		public void tick()
		{
			super.tick();
			if ( isOutOfScreen( SCREEN_SIZE.width , SCREEN_SIZE.height ) ) {
				velocity.x = -velocity.x;
				position.add( velocity );
			}
		}

		public int ticksSinceLastShot(int currentTick) { return tickAtLastShot > 0 ? currentTick - tickAtLastShot : Integer.MAX_VALUE; }

		public void shoot()
		{
			if ( ticksSinceLastShot( currentTick ) > TICKS_PER_SHOT_LIMIT && playerBulletsInFlight < MAX_PLAYER_BULLETS_IN_FLIGHT )
			{
				final Vec2d pos = new Vec2d( position.x , top() - 5 - BULLET_SIZE.height() );
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
			super(position,velocity, new Vec2d(3,7 ) );
			this.shotByPlayer = shotByPlayer;
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
			if ( noOtherInvaderBelow() && random.nextFloat() > 0.995 ) {
				entities.add( new Bullet( new Vec2d( position.x , position.y + 5 + size.height() ) , new Vec2d( 0 , INVADER_BULLET_VELOCITY ) , false ) );
			}
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

	protected static final class Game extends JPanel implements ActionListener
	{
		public Game()
		{
			setPreferredSize(SCREEN_SIZE);
			setSize(SCREEN_SIZE );

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
			ticksTillDifficultyIncrease=DIFFICULITY_INCREASE_AFTER_TICKS;
			difficulty = 1;
			playerBulletsInFlight=0;
			gameOver = false;

			player = new Player( new Vec2d( SCREEN_SIZE.width /2 , SCREEN_SIZE.height - PLAYER_SIZE.height() )  );

			entities.clear();
			entities.add( player );
			spawnInvaders();
		}

		private void spawnInvaders()
		{
			final int requiredWidth = INVADERS_PER_ROW * INVADER_SIZE.width() + (INVADERS_PER_ROW-1) * INVADER_SIZE.width();
			final int remainingWidth = SCREEN_SIZE.width - requiredWidth;
			final int xStartingOffset = Math.max( 0 ,  remainingWidth/2 );
			final int yStartingOffset = INVADER_SIZE.height();
			for ( int x = 0 ; x < INVADERS_PER_ROW ; x++ )
			{
				for ( int y = 0 ; y < ROWS_OF_INVADERS ; y++ )
				{
					final int xStart = xStartingOffset + x * INVADER_SIZE.width() + x*INVADER_SIZE.width();
					final int yStart = yStartingOffset + y * INVADER_SIZE.height() + y*INVADER_SIZE.height();
					entities.add( new Invader(new Vec2d(xStart,yStart) , new Vec2d( INVADER_VELOCITY.x , 0 ) ) );
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

		@Override
		public void actionPerformed(ActionEvent event) // method gets called by Swing Timer each tick
		{
			if ( ! gameOver )
			{
				currentTick++;

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

				// adjust invader movement direction if either the left-most or right-most
				// hit the screen border
				if ( ( leftMost != null && leftMost.left() < 0 ) || ( rightMost != null && rightMost.right() > SCREEN_SIZE.width ) )
				{
					entities.stream().filter( Entity::isInvader ).forEach( invader ->
					{
						if ( invader.bottom() < SCREEN_SIZE.width*0.8 ) {
							invader.position.y += 2;
						}
						invader.velocity.x = -invader.velocity.x;
					});
				}

				// tick all entities
				new ArrayList<>( entities ).forEach( Entity::tick );

				// remove colliding entities (can only be bullet<->player or bullet<->invader since bullets cannot collide with each other)
				final List<Entity> colliding = entities.stream().filter( a -> a.collidesWith(entities) ).collect(Collectors.toList());

				final List<Entity> toRemove = new ArrayList<>();
				if ( colliding.contains( player ) ) {
					gameOver = true;
					playSound(SoundEffect.GAME_OVER);
				} else {
					toRemove.addAll(colliding);
				}

				toRemove.addAll( entities.stream().filter( entity -> entity.isOutOfScreen( SCREEN_SIZE.width , SCREEN_SIZE.height )  ).collect(Collectors.toList()) );

				if ( ! gameOver )
				{
					playerBulletsInFlight -= toRemove.stream().filter( e -> e.isBullet() && ((Bullet) e).shotByPlayer ).count();
					final long invadersDestroyed = toRemove.stream().filter( Entity::isInvader ).count();
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

				entities.removeAll( toRemove );
			}
			repaint();
		}

		protected void increaseDifficulty()
		{
			INVADER_VELOCITY.x += 1;
			INVADER_VELOCITY.y += 2;
			difficulty+=1;
			ticksTillDifficultyIncrease = DIFFICULITY_INCREASE_AFTER_TICKS;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			entities.forEach( e -> e.draw(g) );

			g.setColor(Color.BLUE);
			g.drawString("Level: "+difficulty, 10,10);
			g.drawString("Invaders remaining: "+remainingInvaders, 10,20);

			final float remainingTimeInSeconds = ticksTillDifficultyIncrease / (float) TICKS_PER_SECOND;
			g.drawString("Time remaining: "+new DecimalFormat("####0.0#").format( remainingTimeInSeconds), 10,30);

			if ( gameOver )
			{
				g.setColor(Color.RED);
				g.drawString("GAME OVER !!!" , SCREEN_SIZE.width/2 - 150 , SCREEN_SIZE.height/2 - 7 );
			}
		}
	}

	public static void main(String[] args)
	{
		final JFrame frame = new JFrame("JavaInvaders");
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		final Game game = new Game();

		frame.getContentPane().add(game);
		frame.pack();
		frame.setVisible(true);

		new Timer( 1000 / TICKS_PER_SECOND , game ).start();
		game.requestFocus();
	}
}