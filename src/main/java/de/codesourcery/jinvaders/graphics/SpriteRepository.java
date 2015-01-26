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

import java.util.HashMap;
import java.util.Map;

public class SpriteRepository {

	private final Map<SpriteKey,String> keyToFile = new HashMap<>();
	private final Map<SpriteKey,Sprite> spritesByKey = new HashMap<>();

	public SpriteRepository()
	{
		// TODO: Maybe externalize this mapping (config file) ??
		keyToFile.put( SpriteImpl.PLAYER         , "/player_sprite.png");
		keyToFile.put( SpriteImpl.LIFE_ICON      , "/player_sprite.png");
		keyToFile.put( SpriteImpl.INVADER        , "/invader_sprite.png");
		keyToFile.put( SpriteImpl.PLAYER_BULLET  , "/player_bullet_sprite.png");
		keyToFile.put( SpriteImpl.BARRICADE      , "/barricade_sprite.png");
		keyToFile.put( SpriteImpl.INVADER_BULLET , "/invader_bullet_sprite.png");

		loadSprites();
	}

	private void loadSprites()
	{
		keyToFile.forEach( (key,resource) ->
		{
			spritesByKey.put( key , new SimpleSprite( resource ) );
		});
	}

	public Sprite getSprite(SpriteKey key)
	{
		final Sprite result = spritesByKey.get(key);
		if ( result == null ) {
			throw new IllegalArgumentException("Unknown sprite key "+key);
		}
		return result;
	}
}