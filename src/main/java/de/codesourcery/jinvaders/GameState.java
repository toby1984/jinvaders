package de.codesourcery.jinvaders;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public enum GameState
{
	PLAYING {

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
		public void tick(Game game)
		{
			game.processInput();

			game.advanceGameState();
		}

		@Override
		protected GameState[] getValidTransitions() {
			return new GameState[]{ GAME_OVER };
		}
	},
	GAME_OVER
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
		public void tick(Game game)
		{
			game.processInput();
		}

		@Override
		protected GameState[] getValidTransitions() {
			return new GameState[]{ PLAYING,ENTER_HIGHSCORE,SHOW_HIGHSCORES };
		}
	},
	ENTER_HIGHSCORE {

		@Override
		public void tick(Game game)
		{
		}

		@Override
		public void render(Game game) {
		}

		@Override
		protected GameState[] getValidTransitions() {
			return new GameState[]{ PLAYING , SHOW_HIGHSCORES };
		}
	},
	SHOW_HIGHSCORES
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
		public void tick(Game game)
		{
			game.processInput();
		}

		@Override
		protected GameState[] getValidTransitions() {
			return new GameState[]{ PLAYING };
		}
	};

	private final Set<GameState> validStateTransitions;

	private GameState()
	{
		final GameState[] valid = getValidTransitions();
		if ( valid != null && valid.length > 0 ) {
			this.validStateTransitions = Collections.unmodifiableSet( new HashSet<>( Arrays.asList( valid ) ) );
		} else {
			this.validStateTransitions = Collections.emptySet();
		}
	}

	public abstract void tick(Game game);

	public abstract void render(Game game);

	protected abstract GameState[] getValidTransitions();

	public final void assertTransitionValid(GameState newState) {
		if ( ! isValidTransition(newState) ) {
			throw new IllegalStateException("Invalid game state transition "+this+" -> "+newState);
		}
	}

	public final boolean isValidTransition(GameState newState)
	{
		if ( newState == null ) {
			throw new IllegalArgumentException( "newState must not be NULL" );
		}
		return this.validStateTransitions.contains( newState );
	}
}