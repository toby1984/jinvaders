package de.codesourcery.jinvaders.particles;

import de.codesourcery.jinvaders.graphics.Vec2df;

public final class Particle
{
	public final Vec2df position=new Vec2df();
	public final Vec2df acceleration=new Vec2df();
	public final Vec2df velocity=new Vec2df();

	public boolean isAlive;
	public float lifetimeLeft;
	public int color;
}
