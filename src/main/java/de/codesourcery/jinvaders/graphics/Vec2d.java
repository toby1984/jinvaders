package de.codesourcery.jinvaders.graphics;

public final class Vec2d
{
	public static final Vec2d ZERO = new Vec2d(0,0);

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