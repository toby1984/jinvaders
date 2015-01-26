package de.codesourcery.jinvaders.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import de.codesourcery.jinvaders.ITickListener;
import de.codesourcery.jinvaders.entity.ITickContext;

public final class FlashingAnimator implements ITickListener {

	public boolean isFlashing;

	private boolean flashingState;
	private int flashingTicksRemaining;

	private final Sprite blackSprite;
	private final Sprite initialSprite;

	private final Runnable onStopFlashingCallback;
	private final ISpriteHolder spriteHolder;

	public static FlashingAnimator create(ITickContext ctx , ISpriteHolder spriteHolder,Runnable onStopFlashing)
	{
		if ( onStopFlashing == null ) {
			throw new IllegalArgumentException( "onStopFlashing must not be NULL");
		}
		final FlashingAnimator result = new FlashingAnimator(spriteHolder,onStopFlashing);
		ctx.addTickListener( result );
		return result;
	}

	public static FlashingAnimator create(ITickContext ctx , ISpriteHolder spriteHolder)
	{
		final FlashingAnimator result = new FlashingAnimator(spriteHolder,null);
		ctx.addTickListener( result );
		return result;
	}

	private FlashingAnimator(final ISpriteHolder spriteHolder,Runnable onStopFlashing)
	{
		if ( spriteHolder == null ) {
			throw new IllegalArgumentException( "spriteHolder must not be NULL");
		}
		this.spriteHolder = spriteHolder;
		this.initialSprite = spriteHolder.getSprite();

		if ( initialSprite == null ) {
			throw new IllegalArgumentException( "spriteHolder returned NULL initialSprite ??");
		}

		this.onStopFlashingCallback = onStopFlashing;
		this.isFlashing= true;
		this.flashingTicksRemaining = 60;

		this.blackSprite = new Sprite()
		{
			@Override
			public Vec2d size() { return initialSprite.size(); }

			@Override
			public BufferedImage image() { throw new UnsupportedOperationException("image() not implemented"); }

			@Override
			public void render(Graphics2D graphics, Vec2d position) { render(graphics,position.x,position.y); }

			@Override
			public void render(Graphics2D graphics, int x, int y) {
				graphics.setColor( Color.BLACK );
				graphics.fillRect(x, y ,  size().width() ,  size().height() );
			}
		};
	}

	@Override
	public void tick(final ITickContext context)
	{
		if ( isFlashing )
		{
			flashingTicksRemaining--;
			if ( flashingTicksRemaining == 0 )
			{
				isFlashing=false;
				spriteHolder.setSprite( initialSprite );
				context.removeTickListener( this );
				if ( onStopFlashingCallback != null ) {
					onStopFlashingCallback.run();
				}
				return;
			}

			if ( ( flashingTicksRemaining % 5 ) == 0 )
			{
				flashingState = ! flashingState;
			}

			if ( flashingState )
			{
				spriteHolder.setSprite( blackSprite );
			}
			else
			{
				spriteHolder.setSprite( initialSprite );
			}
		}
		else
		{
			spriteHolder.setSprite( initialSprite );
		}
	}
}