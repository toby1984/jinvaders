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

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
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
import de.codesourcery.jinvaders.graphics.IRenderer;
import de.codesourcery.jinvaders.graphics.Sprite;
import de.codesourcery.jinvaders.graphics.SpriteImpl;
import de.codesourcery.jinvaders.graphics.SpriteKey;
import de.codesourcery.jinvaders.graphics.SpriteRepository;
import de.codesourcery.jinvaders.graphics.UITheme;
import de.codesourcery.jinvaders.graphics.Vec2d;
import de.codesourcery.jinvaders.particles.ParticlePool;
import de.codesourcery.jinvaders.sound.SoundEffect;
import de.codesourcery.jinvaders.util.KeyboardInput;

public final class Game extends JPanel
{
	public final SpriteRepository spriteRepository = new SpriteRepository();

	public final List<HighscoreEntry> highscores = new ArrayList<>();

	protected GameStateImpl gameState = GameState.SHOW_HIGHSCORES.newInstance();

	protected final Vec2d currentInvaderVelocity = new Vec2d(Constants.INITIAL_INVADER_VELOCITY);

	public Player player;

	// FIXME: Change to use seconds instead of ticks
	public int ticksTillDifficultyIncrease=Constants.DIFFICULITY_INCREASE_AFTER_TICKS;

	// list holding all non-static (moving) game entities
	public final List<Entity> nonStaticEntities = new ArrayList<>();

	// list holding all ITickListener that are NOT entities
	protected final List<ITickListener> pureTickListeners = new ArrayList<>();

	public final List<Barricade> barricades = new ArrayList<>();

	protected final KeyboardInput keyboardInput = new KeyboardInput();

	// number of invaders that are still alive
	protected int invadersRemaining;

	// the current tick
	protected int currentTick;
	protected float elapsedTimeInSeconds;

	// the current difficulty level
	public int difficulty = 1;

	// player receives bonus if destroying a wave
	// before DIFFICULITY_INCREASE_AFTER_TICKS have elapsed
	protected boolean eligibleForBonus;

	// background buffer that we're going to render to
	// flag used to synchronize rendering thread (Swing EDT) with game loop execution
	// game loop will only advance after the current frame has been rendered
	public final AtomicBoolean screenRendered = new AtomicBoolean(false);

	private final Object RENDERER_LOCK = new Object();
	private boolean rendererInitialized;
	public final IRenderer renderer;

	private final UITheme uiTheme;

	private final ParticlePool particlePool = new ParticlePool(1000);

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

	public Game(IRenderer renderer)
	{
		this.renderer = renderer;
		this.uiTheme = new UITheme();

		setPreferredSize(Constants.SCREEN_SIZE);
		setSize(Constants.SCREEN_SIZE );

		// we're rendering to a background buffer anyway , disable double buffering
		setDoubleBuffered(false);

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

	private Sprite sprite(SpriteKey key) {
		return spriteRepository.getSprite( key );
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
		synchronized(RENDERER_LOCK)
		{
			if ( ! rendererInitialized ) // first render pass ever, force rendering to backbuffer
			{
				gameState.render( this , uiTheme );
				rendererInitialized = true;
			}
			renderer.end( g );
		}
		screenRendered.set(true);
	}

	public void render(Consumer<IRenderer> callback)
	{
		synchronized(RENDERER_LOCK)
		{
			// if not done yet, setup background buffer to render to
			if ( ! rendererInitialized ) {
				renderer.initialize( this );
				this.uiTheme.initialize( this ,  renderer );
				rendererInitialized = true;
			}

			renderer.begin(); // note: IRenderer.end() gets called by paintComponent();

			renderer.clearScreen();

			callback.accept( renderer );
		}
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
		gameState.render(this , uiTheme );
	}
}