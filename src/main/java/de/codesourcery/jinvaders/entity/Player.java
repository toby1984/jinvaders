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

import de.codesourcery.jinvaders.Constants;
import de.codesourcery.jinvaders.graphics.Sprite;
import de.codesourcery.jinvaders.graphics.SpriteImpl;
import de.codesourcery.jinvaders.graphics.Vec2d;
import de.codesourcery.jinvaders.sound.SoundEffect;

public final class Player extends SpriteHoldingEntity
{
	public int tickAtLastShot=-1;
	public int score;
	public int playerBulletsInFlight;
	public int lifes = Constants.PLAYER_LIFES;

	public Player(Vec2d position,Sprite sprite) {
		super(position,Vec2d.ZERO,sprite);
	}

	@Override
	public void tick(ITickContext ctx)
	{
		super.tick(ctx);
		if ( isOutOfScreen( Constants.VIEWPORT ) ) {
			velocity.x = -velocity.x;
			position.add( velocity );
		}
	}

	@Override
	public void onHit(ITickContext ctx)
	{
		lifes--;
		if ( lifes > 0 ) {
			flash( ctx );
		}
	}

	public void increaseScore(int value) { score += value; }

	public int ticksSinceLastShot(int currentTick) { return tickAtLastShot > 0 ? currentTick - tickAtLastShot : Integer.MAX_VALUE; }

	public void shoot(ITickContext ctx)
	{
		if ( ticksSinceLastShot( ctx.getCurrentTick() ) > Constants.TICKS_PER_SHOT_LIMIT && playerBulletsInFlight < Constants.MAX_PLAYER_BULLETS_IN_FLIGHT )
		{
			final int bulletX = left() + size.width()/2;
			final int bulletY = top() - ctx.getSprite( SpriteImpl.PLAYER_BULLET ).size().height();
			final Vec2d pos = new Vec2d( bulletX, bulletY );

			ctx.addNewEntity( new Bullet(pos,new Vec2d( 0 , -Constants.PLAYER_BULLET_VELOCITY ),this, ctx.getSprite(SpriteImpl.PLAYER_BULLET) ) );

			tickAtLastShot = ctx.getCurrentTick();
			playerBulletsInFlight++;
			SoundEffect.PLAYER_SHOOTING.play();
		}
	}
}