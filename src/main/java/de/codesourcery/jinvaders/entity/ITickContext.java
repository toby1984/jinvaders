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

import java.util.List;

import de.codesourcery.jinvaders.ITickListener;
import de.codesourcery.jinvaders.graphics.Sprite;
import de.codesourcery.jinvaders.graphics.SpriteKey;

public abstract class ITickContext
{
	public abstract float getElapsedTimeInSeconds();

	public abstract void addTickListener(ITickListener listener);

	public abstract void removeTickListener(ITickListener listener);

	public abstract List<Entity> getNonStaticEntities();

	public abstract void addNewEntity(Entity e);

	public abstract void destroyEntity(Entity e);

	/**
	 *
	 * @return
	 * @deprecated will be removed. Use {@link #getElapsedTimeInSeconds()} instead.
	 */
	@Deprecated
	public abstract int getCurrentTick(); // FIXME: REMOVE METHOD

	public abstract Sprite getSprite(SpriteKey sprite);
}