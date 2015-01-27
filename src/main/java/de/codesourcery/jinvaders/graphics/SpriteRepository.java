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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import de.codesourcery.jinvaders.Main;

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
			// entities with lower priority values get rendered first
			// draw bullets first so any collision detection glitches etc. will just paint over the bullet  ;)
			final int renderingPrio = (key == SpriteImpl.INVADER_BULLET || key == SpriteImpl.PLAYER_BULLET ) ? 0 : 10;
			spritesByKey.put( key , new SimpleSprite( loadSprite( resource ) , renderingPrio  ) );
		});
	}

	private ImageHolder loadSprite(String resource)
	{
		final InputStream in = Main.class.getResourceAsStream(resource);
		try {
			final BufferedImage image = ImageIO.read( in );
			return ImageHolder.newAWT( image );
		} catch (final IOException e) {
			throw new RuntimeException("Failed to load sprite '"+resource+"'");
		}
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