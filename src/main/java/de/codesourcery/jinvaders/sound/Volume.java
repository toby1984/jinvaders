package de.codesourcery.jinvaders.sound;

/**
 * Sound volume levels we're using.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public enum Volume
{
	LOUD(0.9f),
	NOT_SO_LOUD(0.7f),
	QUIET(0.5f);

	public final float gainPercentage;

	private Volume(float gainPercentage) { this.gainPercentage = gainPercentage; }
}