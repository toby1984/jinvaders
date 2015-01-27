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
import de.codesourcery.jinvaders.graphics.ISpriteProvider;
import de.codesourcery.jinvaders.graphics.Sprite;
import de.codesourcery.jinvaders.graphics.Vec2d;

public abstract class SpriteHoldingEntity extends Entity implements ISpriteProvider
{
	private ISpriteProvider spriteProvider;

	public SpriteHoldingEntity(Vec2d position, Vec2d velocity, Sprite sprite) {
		super(position, velocity, sprite.size() );
		this.spriteProvider = sprite;
	}

	@Override
	public final Sprite getSprite() {
		return spriteProvider.getSprite();
	}

	@Override
	public ISpriteProvider next() {
		return spriteProvider.next();
	}

	@Override
	public void setNext(ISpriteProvider next) {
		this.spriteProvider = next;
	}

	protected final <T extends ISpriteProvider> T findSpriteProvider(Class<T> clazz)
	{
		ISpriteProvider current = spriteProvider;
		while( current != null )
		{
			if ( clazz.isAssignableFrom( current.getClass() ) ) {
				return (T) current;
			}
			current = current.next();
		}
		return null;
	}

	protected <T extends ISpriteProvider> void removeSpriteProvider(Class<T> clazz) {

		ISpriteProvider previous = this;
		ISpriteProvider current = this.spriteProvider;
		while( current != null )
		{
			if ( clazz.isAssignableFrom( current.getClass() ) )
			{
				previous.setNext( current.next() );
			} else {
				previous = current;
			}
			current = current.next();
		}
	}

	protected void addSpriteProvider(ISpriteProvider toAdd)
	{
		toAdd.setNext( spriteProvider );
		spriteProvider = toAdd;
	}

	protected final boolean isNotFlashing()
	{
		final FlashingAnimator current = findSpriteProvider(FlashingAnimator.class);
		return current == null || (current != null && ! current.isFlashing());
	}

	public final void flash(final ITickContext ctx,int lifetimeInTicks)
	{
		flash(ctx,lifetimeInTicks,null);
	}

	public final void flash(final ITickContext ctx,int lifetimeInTicks,Runnable callbackToExecuteAfterFlashing)
	{
		if ( isNotFlashing() )
		{
			final Runnable onStop = () ->
			{
				removeSpriteProvider( FlashingAnimator.class );
				if ( callbackToExecuteAfterFlashing != null ) {
					callbackToExecuteAfterFlashing.run();
				}
			};
			addSpriteProvider( FlashingAnimator.create( ctx , this , onStop,5,lifetimeInTicks ) );
		}
	}

	@Override
	public final void render(Graphics2D graphics) {
		spriteProvider.getSprite().render( graphics , position );
	}
}