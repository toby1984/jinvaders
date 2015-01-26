package de.codesourcery.jinvaders.entity;

import de.codesourcery.jinvaders.graphics.Sprite;
import de.codesourcery.jinvaders.graphics.Vec2d;

public final class Bullet extends SpriteHoldingEntity
{
	public final Entity owner;

	public Bullet(Vec2d position,Vec2d velocity,Entity owner,Sprite sprite) {
		super(position,velocity, sprite);
		if ( owner == null ) {
			throw new IllegalArgumentException("owner must not be NULL");
		}
		this.owner = owner;
	}

	@Override
	public void onDispose()
	{
		super.onDispose();
		if ( isShotByPlayer() ) {
			((Player) owner).playerBulletsInFlight--;
		}
	}

	public boolean isShotByPlayer() {
		return owner.isPlayer();
	}

	@Override
	public boolean collides(Entity other)
	{
		if ( other.isBullet() ) { // bullets can not collide with other bullets
			return false;
		}
		// prevent invaders from being killed by their collegues
		// slightly hackish but Invaders#noInvaderBelow() check does
		// prevent invaders below and slightly to the left/right of the firing one
		// to be hit when they move left/right on the next tick
		if ( other.isInvader() && owner.isInvader() ) {
			return false;
		}
		return super.collides(other);
	}

	@Override
	public String toString() {
		return "Bullet( shot by: "+owner+" , pos: "+position+")";
	}
}