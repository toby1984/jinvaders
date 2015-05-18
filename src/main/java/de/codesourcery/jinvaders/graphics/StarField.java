package de.codesourcery.jinvaders.graphics;

import java.util.Random;

import de.codesourcery.jinvaders.Constants;
import de.codesourcery.jinvaders.ITickListener;
import de.codesourcery.jinvaders.entity.ITickContext;

public class StarField implements ITickListener
{
	private final Random rnd = new Random(System.currentTimeMillis());

	private final int starCount;
	private final float[] x;
	private final float[] y;
	private final float[] radius;
	private final float[] speed;

	public StarField(int starCount) {
		this.starCount = starCount;
		x      = new float[starCount];
		y      = new float[starCount];
		radius = new float[starCount];
		speed = new float[starCount];
		populate();
	}

	private void populate() {

		for ( int i = 0 ; i < starCount ; i++ )
		{
			x[i] = rnd.nextFloat();
			y[i] = rnd.nextFloat();
			speed[i] = rnd.nextFloat();
			radius[i] = rnd.nextFloat();
		}
	}

	public void render(IRenderer renderer)
	{
		renderer.setColor( 0x00ffffff );
		for ( int i = 0 ; i < starCount ; i++ )
		{
			final int x = (int) ( this.x[i] *  Constants.SCREEN_SIZE.width);
			final int y = (int) ( this.y[i] *  Constants.SCREEN_SIZE.height);
			renderer.renderCircle( x , y , (int) Math.max( 1 , 4*this.radius[i]) , true );
		}
	}

	@Override
	public void tick(ITickContext context)
	{
		float delta = context.getElapsedTimeInSeconds();
		for ( int i = 0 ; i < starCount ; i++ )
		{
			float offset = delta*speed[i]*0.05f;
			this.x[i] -= offset;
			if ( this.x[i] < 0 ) {
				this.x[i] +=1;
			}
		}
	}
}
