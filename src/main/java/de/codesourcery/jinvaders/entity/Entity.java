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
package de.codesourcery.jinvaders.entity;

import java.awt.Rectangle;
import java.util.Collection;

import de.codesourcery.jinvaders.ITickListener;
import de.codesourcery.jinvaders.graphics.IRenderer;
import de.codesourcery.jinvaders.graphics.Vec2d;

public abstract class Entity implements ITickListener, Comparable<Entity>
{
	public final Vec2d position;
	public final Vec2d velocity;
	public final Vec2d size;

	private EntityState state=EntityState.ALIVE;

	public Entity(Vec2d position,Vec2d velocity,Vec2d size)
	{
		this.velocity = new Vec2d(velocity);
		this.position = new Vec2d(position);
		this.size = new Vec2d(size);
	}

	public void setState(EntityState newState)
	{
		if ( ! this.state.canTransitionTo( newState ) ) {
			throw new IllegalStateException("Invalid state transition for entity "+this+": "+this.state+" -> "+newState);
		}
		this.state = newState;
	}

	public void onDispose() {
	}

	public void onHit(ITickContext ctd) {
	}

	public boolean isAlive() { return state == EntityState.ALIVE; }

	public boolean isDying() { return state == EntityState.DYING; }

	public boolean isDead() { return state == EntityState.DEAD; }

	@Override
	public String toString() { return getClass().getName()+" @ "+position; }

	public boolean isInvader() { return this instanceof Invader; }
	public boolean isPlayer() { return this instanceof Player; }
	public boolean isBullet() { return this instanceof Bullet; }

	public boolean isOffScreen(Rectangle r) {
		return bottom() < r.y || top() > r.y+r.height|| right() < r.x || left() > r.x+r.width;
	}

	public void stop() { velocity.set(0,0); }
	public int left() { return position.x; }
	public int right() { return position.x+size.width(); }
	public int top() { return position.y; }
	public int bottom() { return position.y+size.height(); }

	public boolean isAbove(int y) { return bottom() < y; }
	public boolean isBelow(int y) { return top() > y; }
	public boolean isLeftOf(int x) { return right() < x; }
	public boolean isRightOf(int x) { return left() > x; }

	public boolean isAbove(Entity e) { return isAbove( e.top() ); }
	public boolean isBelow(Entity e) { return isBelow( e.bottom() ); }
	public boolean isLeftOf(Entity e) { return isLeftOf( e.left() ); }
	public boolean isRightOf(Entity e) { return isRightOf( e.right() ); }

	public boolean isMovingUp() { return velocity.y < 0; }
	public boolean isMovingDown() { return velocity.y > 0; }

	public boolean collidesWith(Collection<Entity> others) { return others.stream().anyMatch( this::collides ); }

	public boolean canCollide() { return isAlive(); }

	public boolean destroyWhenOffScreen() { return true; }

	public boolean collides(Entity other) { return this != other &&
			this.canCollide() &
			other.canCollide() &&
			!(isAbove(other) || isBelow(other) || isLeftOf(other) || isRightOf(other ) ); }

	public void moveLeft(int vx) { this.velocity.x = -vx; };
	public void moveRight(int vx) { this.velocity.x = vx; };

	@Override
	public void tick(ITickContext context) {
		position.add( velocity );
	}

	/**
	 * Entities with higher priority values are drawn later.
	 * @return
	 */
	public int getRenderingPriority() {
		return 0;
	}

	@Override
	public final int compareTo(Entity o)
	{
		final int o1 = getRenderingPriority();
		final int o2 = o.getRenderingPriority();
		if ( o1 < o2 ) {
			return -1;
		}
		if ( o1 > o2 ) {
			return 1;
		}
		return 0;
	}

	public abstract void render(IRenderer graphics);
}