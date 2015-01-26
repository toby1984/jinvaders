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