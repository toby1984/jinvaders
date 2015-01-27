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
package de.codesourcery.jinvaders.graphics;

public final class Vec2d
{
	public static final Vec2d ZERO = new Vec2d();

	public int x;
	public int y;

	public Vec2d() {
	}

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

	@Override
	public int hashCode()
	{
		return 31 * (31 + x) + y;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Vec2d)
		{
			Vec2d other = (Vec2d) obj;
			return this.x == other.x && this.y == other.y;
		}
		return false;
	}
}