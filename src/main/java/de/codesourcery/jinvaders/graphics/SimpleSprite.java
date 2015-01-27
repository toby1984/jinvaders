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


public class SimpleSprite implements Sprite
{
	private final ImageHolder image;
	private final Vec2d size;
	private final int renderingPriority;

	/**
	 *
	 * @param resource
	 * @param renderingPriority Entities with higher priority values are drawn later.
	 */
	public SimpleSprite(ImageHolder resource,int renderingPriority)
	{
		this.renderingPriority = renderingPriority;
		this.image = resource;
		this.size = new Vec2d( resource.getWidth() , resource.getHeight() );
	}

	@Override
	public Sprite getSprite() {
		return this;
	}

	@Override
	public void render(IRenderer graphics,Vec2d position)
	{
		render(graphics,position.x,position.y);
	}

	@Override
	public void render(IRenderer graphics,int x,int y)
	{
		graphics.renderImage( image , x , y );
	}

	@Override
	public Vec2d size() { return size; }

	@Override
	public ImageHolder image() { return image; }

	@Override
	public ISpriteProvider next() {
		return null;
	}

	@Override
	public void setNext(ISpriteProvider next) {
		throw new UnsupportedOperationException("setNext() not supported");
	}

	@Override
	public int getRenderingPriority() {
		return renderingPriority;
	}
}