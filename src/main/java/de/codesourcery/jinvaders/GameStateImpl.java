package de.codesourcery.jinvaders;

import de.codesourcery.jinvaders.entity.ITickContext;
import de.codesourcery.jinvaders.graphics.UITheme;

public abstract class GameStateImpl
{
	public final GameState state;

	public GameStateImpl(GameState state)
	{
		if ( state == null ) {
			throw new IllegalArgumentException("State must not be NULL");
		}
		this.state = state;
	}

	public abstract void tick(Game game,ITickContext ctx);

	public abstract void render(Game game, UITheme ui);
}
