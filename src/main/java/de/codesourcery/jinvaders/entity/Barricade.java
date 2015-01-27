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
import java.util.function.Function;

import de.codesourcery.jinvaders.graphics.IRenderer;
import de.codesourcery.jinvaders.graphics.ImageHolder;
import de.codesourcery.jinvaders.graphics.Sprite;
import de.codesourcery.jinvaders.graphics.Vec2d;

public final class Barricade extends Entity {

	private final ImageHolder sprite;

	public Barricade(Vec2d position,Sprite sprite)
	{
		super(position, Vec2d.ZERO, sprite.size() );
		// copy sprite since we're going to write to it later on
		this.sprite = sprite.image().createCopy();
	}

	@Override
	public boolean destroyWhenOffScreen() {
		return false;
	}

	public boolean hitBy(Bullet entity)
	{
		if ( ! collides( entity) ) {
			return false;
		}

		// find intersecting rectangle
		final Rectangle r1 = new Rectangle(position.x,position.y,size.width(),size.height());
		final Rectangle r2 = new Rectangle(entity.position.x,entity.position.y,entity.size.width(),entity.size.height());

		final Rectangle intersection = r1.intersection(r2); // note: intersection can never be empty unless the collides(Entity) method is broken
		if ( intersection.isEmpty() ) {
			return false;
		}

		final int yIncrement;
		final int yStart; // in local coordinates !
		final Function<Integer,Boolean> condition;
		if ( entity.isMovingDown() )
		{
			// check starts at top
			yIncrement=1;
			yStart = intersection.y - position.y;
			condition = y -> y < sprite.getHeight();
		}
		else if ( entity.isMovingUp() )
		{
			// check starts at bottom
			yIncrement=-1;
			yStart = intersection.y + intersection.height - position.y - 1;
			condition = y -> y >= 0;
		} else {
			throw new RuntimeException("Internal error, bullet is moving neither up nor down ??");
		}

		// clear pixels that were hit
		boolean pixelsHit=false;

		final int xStart = intersection.x - position.x;
		final int xEnd = intersection.x + intersection.width - position.x;

		int rowsRemoved = 0;
		for ( int y = yStart ; condition.apply(y) & rowsRemoved < 3 ; y += yIncrement )
		{
			for ( int x = xStart ; x < xEnd ; x++ )
			{
				final int rgb = sprite.getRGB( x ,y ) & 0x00ffffff; // ignore alpha channel
				if ( rgb != 0 ) {
					pixelsHit=true; // stop deleting pixels after finishing this row, we'll only remove the top-most/bottom-most row of pixels on each hit
					sprite.setRGB( x , y , 0 );
				}
			}
			if ( pixelsHit ) {
				rowsRemoved++;
			}
		}
		return pixelsHit;
	}

	@Override
	public void render(IRenderer graphics)
	{
		graphics.renderImage( sprite , position.x ,position.y );
	}
}