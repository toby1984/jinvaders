package de.codesourcery.jinvaders;

import de.codesourcery.jinvaders.entity.ITickContext;

public interface ITickListener
{
	/**
	 *
	 * @param context
	 */
	public void tick(ITickContext context);
}
