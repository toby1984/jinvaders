package de.codesourcery.jinvaders.entity;

import java.awt.Graphics2D;

import de.codesourcery.jinvaders.graphics.FlashingAnimator;
import de.codesourcery.jinvaders.graphics.ISpriteHolder;
import de.codesourcery.jinvaders.graphics.Sprite;
import de.codesourcery.jinvaders.graphics.Vec2d;

public abstract class SpriteHoldingEntity extends Entity implements ISpriteHolder
{
	private FlashingAnimator animator;
	private Sprite sprite;

	public SpriteHoldingEntity(Vec2d position, Vec2d velocity, Sprite sprite) {
		super(position, velocity, sprite.size() );
		this.sprite = sprite;
	}

	@Override
	public final Sprite getSprite() {
		return sprite;
	}

	@Override
	public final void setSprite(Sprite sprite) {
		this.sprite = sprite;
	}

	private boolean isNotFlashing() {
		return animator == null || ! animator.isFlashing;
	}

	public final void flash(final ITickContext ctx)
	{
		if ( isNotFlashing() )
		{
			animator = FlashingAnimator.create( ctx , this );
		}
	}

	public final void flash(final ITickContext ctx,Runnable callbackToExecuteAfterFlashing)
	{
		if ( isNotFlashing() )
		{
			animator = FlashingAnimator.create( ctx , this , callbackToExecuteAfterFlashing );
		}
	}

	@Override
	public final void render(Graphics2D graphics) {
		sprite.render( graphics , position );
	}
}