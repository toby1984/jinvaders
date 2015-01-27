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

import java.util.Random;

import de.codesourcery.jinvaders.Constants;
import de.codesourcery.jinvaders.graphics.Sprite;
import de.codesourcery.jinvaders.graphics.SpriteImpl;
import de.codesourcery.jinvaders.graphics.Vec2d;
import de.codesourcery.jinvaders.sound.SoundEffect;

public final class Invader extends SpriteHoldingEntity
{
	protected static final Random random = new Random(System.currentTimeMillis());

	public Invader(Vec2d position,Vec2d velocity,Sprite sprite) {
		super(position,velocity, sprite );
	}

	@Override
	public void onHit(ITickContext ctx)
	{
		setState( EntityState.DYING );
		flash(ctx, 30 , () -> setState(EntityState.DEAD ) );
	}

	@Override
	public boolean collides(Entity other)
	{
		// prevent invaders from being killed by their collegues
		// slightly hackish but Invaders#noInvaderBelow() check does
		// prevent invaders below and slightly to the left/right of the firing one
		// to be hit when they move left/right on the next tick
		if ( other.isBullet() && ! ( (Bullet) other).isShotByPlayer() )  {
			return false;
		}
		return super.collides(other);
	}

	@Override
	public void tick(ITickContext ctx)
	{
		super.tick(ctx);
		if ( noOtherInvaderBelow( ctx ) && random.nextFloat() > Constants.INVADER_FIRING_PROBABILITY )
		{
			final Vec2d initialPos = new Vec2d( position.x , position.y + 5 + size.height() );
			final Vec2d initialVelocity = new Vec2d( 0 , Constants.INVADER_BULLET_VELOCITY );

			final Bullet bullet = new Bullet(initialPos,initialVelocity,this,ctx.getSprite(SpriteImpl.INVADER_BULLET ) );
			ctx.addNewEntity( bullet );
			SoundEffect.INVADER_SHOOTING.play();
		}
	}

	private boolean noOtherInvaderBelow(ITickContext ctx)
	{
		return ctx.getNonStaticEntities().stream().noneMatch( entity ->
		entity != this &&
		entity.isInvader() &&
		entity.isBelow( this ) &&
		( ( entity.left() >= left()-10 && entity.left() <= right()+10 ) || ( entity.right() >= left()-10 && entity.right() <= right()+10 ) ) );
	}
}