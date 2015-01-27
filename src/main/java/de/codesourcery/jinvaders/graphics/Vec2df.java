package de.codesourcery.jinvaders.graphics;


public final class Vec2df
{
	public static final Vec2df ZERO = new Vec2df();

	public float x;
	public float y;

	public Vec2df() {
	}

	public Vec2df(Vec2df other) {
		x = other.x ;
		y = other.y;
	}

	public Vec2df(float x,float y)
	{
		this.x = x;
		this.y = y;
	}

	public Vec2df set(float x,float y)
	{
		this.x = x;
		this.y = y;
		return this;
	}

	public float length() {
		return (float) Math.sqrt( x*x +y*y );
	}

	public Vec2df scale(float factor) {
		this.x *= factor;
		this.y *= factor;
		return this;
	}

	public float len2() {
		return x*x +y*y;
	}

	public Vec2df limit(float limit)
	{
		if (len2() > limit * limit) {
			normalize();
			scale(limit);
		}
		return this;
	}

	public Vec2df normalize()
	{
		float len = x*x + y*y;
		if ( len != 0 )
		{
			len = (float) Math.sqrt(len);
			x /= len;
			y /= len;
		}
		return this;
	}

	public void set(Vec2d other) {
		x = other.x ;
		y = other.y;
	}

	public void set(Vec2df other) {
		x = other.x ;
		y = other.y;
	}

	public void add(Vec2df that) {
		x += that.x ;
		y += that.y;
	}

	public Vec2df add(float x,float y) {
		this.x += x ;
		this.y += y;
		return this;
	}

	public void sub(Vec2df that) {
		x -= that.x ;
		y -= that.y;
	}

	public float width() { return x; }

	public float height() { return y; }

	@Override
	public String toString() { return "( "+x+" , "+y+" )"; }

	@Override
	public int hashCode()
	{
		return 31 * (31 + Float.hashCode(x) ) + Float.hashCode(y);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Vec2df)
		{
			final Vec2df other = (Vec2df) obj;
			return this.x == other.x && this.y == other.y;
		}
		return false;
	}
}