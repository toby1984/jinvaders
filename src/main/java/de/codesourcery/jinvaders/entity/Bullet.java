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

import de.codesourcery.jinvaders.graphics.Sprite;
import de.codesourcery.jinvaders.graphics.Vec2d;

public final class Bullet extends SpriteHoldingEntity
{
	public final Entity owner;

	public Bullet(Vec2d position,Vec2d velocity,Entity owner,Sprite sprite) {
		super(position,velocity, sprite);
		if ( owner == null ) {
			throw new IllegalArgumentException("owner must not be NULL");
		}
		this.owner = owner;
	}

	@Override
	public void onDispose()
	{
		super.onDispose();
		if ( isShotByPlayer() ) {
			((Player) owner).playerBulletsInFlight--;
		}
	}

	public boolean isShotByPlayer() {
		return owner.isPlayer();
	}

	@Override
	public boolean collides(Entity other)
	{
		if ( other.isBullet() ) { // bullets can not collide with other bullets
			return false;
		}
		// prevent invaders from being killed by their collegues
		// slightly hackish but Invaders#noInvaderBelow() check does
		// prevent invaders below and slightly to the left/right of the firing one
		// to be hit when they move left/right on the next tick
		if ( other.isInvader() && owner.isInvader() ) {
			return false;
		}
		return super.collides(other);
	}

	@Override
	public String toString() {
		return "Bullet( shot by: "+owner+" , pos: "+position+")";
	}
}