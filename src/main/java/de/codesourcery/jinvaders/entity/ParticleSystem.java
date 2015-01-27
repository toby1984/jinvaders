package de.codesourcery.jinvaders.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Random;

import de.codesourcery.jinvaders.graphics.Vec2d;
import de.codesourcery.jinvaders.particles.Particle;
import de.codesourcery.jinvaders.particles.ParticlePool;
import de.codesourcery.jinvaders.particles.ParticlePool.Subpool;

public final class ParticleSystem extends Entity
{
	private final ParticlePool pool;
	private final List<Subpool> allocated;

	private final ParticleEffect effect;

	private int particlesAlive;

	public static final class ParticleEffect
	{
		private final Random rnd = new Random(System.currentTimeMillis());

		private final Vec2d initialPosition;
		public final int particleCount;
		public final float lifeTime;

		public float age;

		public ParticleEffect(Vec2d initialPosition,int particleCount, float lifeTime)
		{
			if ( particleCount < 1 ) {
				throw new IllegalArgumentException("Invalid particle count: "+particleCount);
			}
			if ( lifeTime <= 0 ) {
				throw new IllegalArgumentException("Invalid life time: "+lifeTime);
			}
			this.initialPosition = new Vec2d(initialPosition);
			this.particleCount = particleCount;
			this.lifeTime = lifeTime;
		}

		public void onTick(float elapsedTimeInSeconds) {
			age += elapsedTimeInSeconds;
		}

		public void init(Particle p)
		{
			p.position.set( this.initialPosition );

			p.velocity.set( rndNumber( 310) , rndNumber( 310 ) );
			p.acceleration.set( rndNumber( 80 ) , rndNumber( 80 ) );

			p.isAlive = true;
			p.lifetimeLeft = lifeTime;
		}

		protected float rndNumber(int limit) {
			return -limit + 2 * limit * rnd.nextFloat();
		}

		public void animate(float elapsedTimeInSecs, Particle p)
		{
			final float vIncX = p.acceleration.x * elapsedTimeInSecs;
			final float vIncY = p.acceleration.y * elapsedTimeInSecs;

			final float incPx = p.acceleration.x / 2 * (elapsedTimeInSecs*elapsedTimeInSecs) + p.velocity.x*elapsedTimeInSecs;
			final float incPy = p.acceleration.y / 2 * (elapsedTimeInSecs*elapsedTimeInSecs) + p.velocity.y*elapsedTimeInSecs;

			p.position.add( incPx , incPy );
			p.velocity.add( vIncX, vIncY ).limit(410);

			p.lifetimeLeft -= elapsedTimeInSecs;
			if ( p.lifetimeLeft < 0 )
			{
				p.isAlive = false;
			}
		}
	}

	public ParticleSystem(ParticlePool pool , ParticleEffect effect)
	{
		super(Vec2d.ZERO, Vec2d.ZERO, Vec2d.ZERO);

		this.effect = effect;
		this.pool = pool;

		allocated = pool.allocateParticles( effect.particleCount );
		this.particlesAlive = effect.particleCount;

		final Particle[] array = pool.particlePool;
		for ( final Subpool subPool : allocated )
		{
			for ( int i = subPool.startIndex ; i < subPool.endIndex ; i++)
			{
				effect.init( array[i] );
			}
		}
	}

	@Override
	public boolean destroyWhenOffScreen() {
		return false;
	}

	@Override
	public boolean canCollide() {
		return false;
	}

	@Override
	public void onDispose()
	{
		pool.releaseParticles( this.allocated );
	}

	@Override
	public void tick(ITickContext context)
	{
		final float elapsedTimeInSecs = context.getElapsedTimeInSeconds();

		final Particle[] array = pool.particlePool;
		final int len = allocated.size();

		effect.onTick( elapsedTimeInSecs );

		for ( int i = 0 ; i < len ; i++ )
		{
			final Subpool subPool = allocated.get(i);
			for ( int j = subPool.startIndex ; j < subPool.endIndex ; j++)
			{
				final Particle p = array[j];
				if ( p.isAlive ) {
					effect.animate(elapsedTimeInSecs, p);
					if ( ! p.isAlive ) {
						particlesAlive--;
					}
				}
			}
		}
		if ( particlesAlive <= 0 && ! allocated.isEmpty() )
		{
			pool.releaseParticles( allocated );
			allocated.clear();
		}
	}

	@Override
	public void render(Graphics2D graphics)
	{
		graphics.setColor(Color.RED);

		final Particle[] array = pool.particlePool;
		final int len = allocated.size();
		for ( int i = 0 ; i < len ; i++ )
		{
			final Subpool subPool = allocated.get(i);
			for ( int j = subPool.startIndex ; j < subPool.endIndex ; j++)
			{
				final Particle p = array[j];
				if ( p.isAlive )
				{
					graphics.drawRect( (int) p.position.x , (int) p.position.y , 1 ,1 );
				}
			}
		}
	}
}