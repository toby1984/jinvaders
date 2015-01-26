package de.codesourcery.jinvaders.entity;

import java.util.List;

import de.codesourcery.jinvaders.ITickListener;
import de.codesourcery.jinvaders.graphics.Sprite;
import de.codesourcery.jinvaders.graphics.SpriteKey;

public abstract class ITickContext
{
	public abstract void addTickListener(ITickListener listener);

	public abstract void removeTickListener(ITickListener listener);

	public abstract List<Entity> getNonStaticEntities();

	public abstract void addNewEntity(Entity e);

	public abstract void destroyEntity(Entity e);

	public abstract int getCurrentTick();

	public abstract Sprite getSprite(SpriteKey sprite);
}