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

import de.codesourcery.jinvaders.ITickListener;
import de.codesourcery.jinvaders.entity.ITickContext;

public final class FlashingAnimator implements ITickListener , ISpriteProvider {

	private boolean flashingState;
	private int flashingTicksRemaining;

	private final Runnable onStopFlashingCallback;
	private ISpriteProvider spriteProvider;

	private final int frequency;

	private final Sprite blackSprite = new Sprite()
	{
		@Override
		public Vec2d size() { return spriteProvider.getSprite().size(); }

		@Override
		public ImageHolder image() { throw new UnsupportedOperationException("image() not implemented"); }

		@Override
		public void render(IRenderer graphics, Vec2d position) { render(graphics,position.x,position.y); }

		@Override
		public void render(IRenderer graphics, int x, int y) {
			graphics.setColor( 0 ); // BLACK
			graphics.fillRect(x, y ,  size().width() ,  size().height() );
		}

		@Override
		public Sprite getSprite() { return this; }

		@Override
		public ISpriteProvider next() { return spriteProvider; }

		@Override
		public void setNext(ISpriteProvider next) {
			spriteProvider = next;
		}

		@Override
		public int getRenderingPriority() {
			return spriteProvider.getRenderingPriority();
		}
	};

	public static FlashingAnimator create(ITickContext ctx , ISpriteProvider spriteProvider,Runnable onStopFlashing,int frequency,int lifetimeInTicks)
	{
		if ( onStopFlashing == null ) {
			throw new IllegalArgumentException( "onStopFlashing must not be NULL");
		}
		final FlashingAnimator result = new FlashingAnimator(spriteProvider,onStopFlashing,frequency,lifetimeInTicks);
		ctx.addTickListener( result );
		return result;
	}

	public static FlashingAnimator create(ITickContext ctx , ISpriteProvider spriteHolder,int frequency,int lifetimeInTicks)
	{
		final FlashingAnimator result = new FlashingAnimator(spriteHolder,null,frequency,lifetimeInTicks);
		ctx.addTickListener( result );
		return result;
	}

	private FlashingAnimator(final ISpriteProvider spriteHolder,Runnable onStopFlashing,int frequency,int lifetimeInTicks)
	{
		if ( spriteHolder == null ) {
			throw new IllegalArgumentException( "spriteHolder must not be NULL");
		}
		if( lifetimeInTicks< 1) {
			throw new IllegalArgumentException( "lifeTimeInTicks must be >= 1");
		}
		if( frequency< 2) {
			throw new IllegalArgumentException( "frequency must be at least 2");
		}
		this.frequency = frequency;
		this.spriteProvider = spriteHolder;
		this.onStopFlashingCallback = onStopFlashing;
		this.flashingTicksRemaining = lifetimeInTicks;
	}

	public boolean isFlashing() {
		return this.flashingTicksRemaining > 0;
	}

	@Override
	public void tick(final ITickContext context)
	{
		if ( isFlashing() )
		{
			flashingTicksRemaining--;
			if ( flashingTicksRemaining <= 0 )
			{
				context.removeTickListener( this );
				if ( onStopFlashingCallback != null ) {
					onStopFlashingCallback.run();
				}
				return;
			}

			if ( ( flashingTicksRemaining % frequency ) == 0 )
			{
				flashingState = ! flashingState;
			}
		}
	}

	@Override
	public Sprite getSprite()
	{
		return isFlashing() && flashingState ? blackSprite : spriteProvider.getSprite();
	}

	@Override
	public ISpriteProvider next() {
		return this.spriteProvider;
	}

	@Override
	public void setNext(ISpriteProvider next) {
		this.spriteProvider = next;
	}

	@Override
	public int getRenderingPriority() {
		return this.spriteProvider.getRenderingPriority();
	}
}