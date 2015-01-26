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

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.Optional;

import de.codesourcery.jinvaders.entity.ITickContext;
import de.codesourcery.jinvaders.entity.Player;
import de.codesourcery.jinvaders.util.KeyboardInput;

public enum GameState
{
	PLAYING
	{
		@Override
		public GameStateImpl newInstance()
		{
			return new GameStateImpl(PLAYING)
			{
				@Override
				public void render(Game game)
				{
					game.render( g ->
					{
						game.renderEntities(g);
						game.renderHud(g);
					});
				}

				@Override
				public void tick(Game game,ITickContext ctx)
				{
					final KeyboardInput keyboardInput = game.getKeyboardInput();
					final Player player = game.getPlayer();

					if ( keyboardInput.isPressed(KeyEvent.VK_A) ) {
						player.moveLeft( Constants.PLAYER_VELOCITY );
					}
					else if ( keyboardInput.isPressed( KeyEvent.VK_D ) ) {
						player.moveRight( Constants.PLAYER_VELOCITY );
					} else {
						player.stop();
					}
					if ( keyboardInput.isPressed( KeyEvent.VK_SPACE ) )
					{
						player.shoot(ctx);
					}

					game.advanceGameState();
				}
			};
		}

		@Override
		public GameState onEnter(GameState previousState, Game game) {
			game.reset();
			return this;
		}
	},
	GAME_OVER
	{
		@Override
		public GameStateImpl newInstance()
		{
			return new GameStateImpl(GAME_OVER)
			{
				@Override
				public void render(Game game)
				{
					game.render( g ->
					{
						game.renderEntities(g);
						game.renderHud(g);
						game.renderGameOverText(g);
					});
				}

				@Override
				public void tick(Game game,ITickContext ctx)
				{
					if ( game.getKeyboardInput().isPressed( KeyEvent.VK_ENTER) ) {
						game.startGame();
					}
				}
			};
		}

		@Override
		public GameState onEnter(GameState previousState, Game game)
		{
			final Optional<HighscoreEntry> lowestScore = game.getHighscores().stream().reduce( (a,b) -> a.score < b.score ? a : b);

			if ( ! lowestScore.isPresent() || game.getPlayer().score > lowestScore.get().score )
			{
				return ENTER_HIGHSCORE;
			}
			return this;
		}
	},
	ENTER_HIGHSCORE
	{
		@Override
		public GameStateImpl newInstance()
		{
			return new GameStateImpl(ENTER_HIGHSCORE)
			{
				private final StringBuilder buffer = new StringBuilder();

				@Override
				public void render(Game game)
				{
					game.render( g ->
					{
						game.renderEntities(g);
						game.renderHud(g);
						game.renderEnterHighscore(g,buffer.toString(),game.getPlayer().score);
					});
				}

				@Override
				public void tick(Game game,ITickContext ctx)
				{
					final int c = game.getKeyboardInput().maybeReadKey();
					if ( c != -1 )
					{
						if ( c == 8 ) { // <DEL>
							if ( buffer.length() > 0 )
							{
								buffer.setLength( buffer.length() -1 );
							} else {
								Toolkit.getDefaultToolkit().beep(); // TODO: Play sample
							}
						} else if ( (c == 10 || c ==13 ) && buffer.length() > 0 ) { // <ENTER>
							game.getHighscores().add( new HighscoreEntry( buffer.toString() , game.getPlayer().score ) );
							game.setGameState(GameState.SHOW_HIGHSCORES);
						} else {
							if ( buffer.length() < 8 )
							{
								buffer.append( (char) c );
							} else {
								Toolkit.getDefaultToolkit().beep(); // TODO: Play sample
							}
						}
					}
				}
			};
		}
	},
	SHOW_HIGHSCORES
	{
		@Override
		public GameStateImpl newInstance()
		{
			return new GameStateImpl(SHOW_HIGHSCORES)
			{
				@Override
				public void render(Game game)
				{
					game.render( g ->
					{
						game.renderEntities(g);
						game.renderHud(g);
						game.renderHighscoreList(g);
					});
				}

				@Override
				public void tick(Game game,ITickContext ctx)
				{
					if ( game.getKeyboardInput().isPressed( KeyEvent.VK_ENTER) ) {
						game.startGame();
					}
				}
			};
		}
	};

	public final void assertTransitionValid(GameState newState) {
		if ( ! isValidTransition(newState) ) {
			throw new IllegalStateException("Invalid game state transition "+this+" -> "+newState);
		}
	}

	public GameState onEnter(GameState previousState, Game game) {
		return this;
	}

	public final boolean isValidTransition(GameState newState)
	{
		if ( newState == null ) {
			throw new IllegalArgumentException( "newState must not be NULL" );
		}
		switch (this) {
			case ENTER_HIGHSCORE:
				return newState.equals( SHOW_HIGHSCORES ) || newState.equals( PLAYING );
			case GAME_OVER:
				return newState.equals( PLAYING ) || newState.equals( SHOW_HIGHSCORES ) || newState.equals( ENTER_HIGHSCORE );
			case PLAYING:
				return newState.equals( GAME_OVER );
			case SHOW_HIGHSCORES:
				return newState.equals( PLAYING );
			default:
				return false;
		}
	}

	public abstract GameStateImpl newInstance();
}