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

import java.awt.Graphics2D;

import de.codesourcery.jinvaders.graphics.FlashingAnimator;
import de.codesourcery.jinvaders.graphics.ISpriteHolder;
import de.codesourcery.jinvaders.graphics.Sprite;
import de.codesourcery.jinvaders.graphics.Vec2d;

public abstract class SpriteHoldingEntity extends Entity implements ISpriteHolder
{
	private FlashingAnimator animator;
	private Sprite sprite;

	public SpriteHoldingEntity(Vec2d position, Vec2d velocity, Sprite sprite) {
		super(position, velocity, sprite.size() );
		this.sprite = sprite;
	}

	@Override
	public final Sprite getSprite() {
		return sprite;
	}

	@Override
	public final void setSprite(Sprite sprite) {
		this.sprite = sprite;
	}

	private boolean isNotFlashing() {
		return animator == null || ! animator.isFlashing;
	}

	public final void flash(final ITickContext ctx)
	{
		if ( isNotFlashing() )
		{
			animator = FlashingAnimator.create( ctx , this );
		}
	}

	public final void flash(final ITickContext ctx,Runnable callbackToExecuteAfterFlashing)
	{
		if ( isNotFlashing() )
		{
			animator = FlashingAnimator.create( ctx , this , callbackToExecuteAfterFlashing );
		}
	}

	@Override
	public final void render(Graphics2D graphics) {
		sprite.render( graphics , position );
	}
}