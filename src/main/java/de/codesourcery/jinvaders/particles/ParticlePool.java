package de.codesourcery.jinvaders.particles;

import java.util.ArrayList;
import java.util.List;

public final class ParticlePool
{
	private static final boolean DEBUG_ALLOCATIONS = false;

	public Particle[] particlePool;

	final List<Subpool> availablePools = new ArrayList<>();
	final List<Subpool> usedPools = new ArrayList<>();

	private int releaseCount;

	public static final class Subpool
	{
		public final int startIndex;
		public final int endIndex;
		public final int size;

		public Subpool(int start,int end)
		{
			if ( start >= end ) {
				throw new IllegalArgumentException("start must be > end (was start="+start+",end="+end+")");
			}
			if ( start < 0 || end < 0 ) {
				throw new IllegalArgumentException("start and end must be >= 0 (was start="+start+",end="+end+")");
			}
			this.startIndex = start;
			this.endIndex= end;
			this.size = end-start;
		}

		@Override
		public int hashCode() {
			int result = 31 + endIndex;
			result = 31 * result + size;
			return 31 * result + startIndex;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj instanceof Subpool)
			{
				final Subpool other = (Subpool) obj;
				return this.startIndex == other.startIndex && this.endIndex == other.endIndex;
			}
			return false;
		}

		@Override
		public String toString() { return "Subpool[ "+startIndex+" - "+endIndex+" ("+size+") ]"; }

		public boolean canBeMergedWith(Subpool other)
		{
			return this.startIndex == other.endIndex || other.startIndex == this.endIndex;
		}

		public Subpool createMerged(Subpool other)
		{
			if ( this.endIndex == other.startIndex )
			{
				return new Subpool( this.startIndex , other.endIndex );
			}

			if ( this.startIndex == other.endIndex )
			{
				return new Subpool( other.startIndex , this.endIndex );
			}

			throw new IllegalArgumentException("Trying to merge pools that cannot be merged: "+this+" <-> "+other);
		}
	}

	public ParticlePool(int initialCapacity)
	{
		if ( initialCapacity < 0 ) {
			throw new IllegalArgumentException("Initial capacity must be >= 0");
		}
		particlePool = new Particle[initialCapacity];
		for ( int i =0 ; i < initialCapacity ; i++ ) {
			particlePool[i] = new Particle();
		}
		if ( initialCapacity > 0 ) {
			availablePools.add( new Subpool( 0 , initialCapacity ) );
		}
	}

	private Subpool extendPoolAndPutOnUsed(int count)
	{
		final int startIdx = particlePool.length;
		final int endIdx = startIdx+count;

		int newSize = particlePool.length + particlePool.length/2;
		if ( newSize < count ) {
			newSize = count + count/2;
		}

		final Particle[] tmp = new Particle[ newSize ];
		System.arraycopy( particlePool , 0 , tmp , 0 , particlePool.length );
		particlePool = tmp;

		for ( int i = startIdx ; i < newSize ; i++ )
		{
			particlePool[i] = new Particle();
		}
		final Subpool result = new Subpool(startIdx,endIdx);
		usedPools.add( result );
		return result;
	}

	public void releaseParticles(List<Subpool> list)
	{
		if ( list.isEmpty() ) {
			return;
		}

		synchronized( availablePools )
		{
			synchronized( usedPools)
			{
				for (int i = 0 , len = list.size() ; i < len ; i++ ) {
					releaseParticles( list.get(i) );
				}
				releaseCount += list.size();
				if ( releaseCount > 10 ) // we'll only trigger the slow de-fragmentation every 10 releases
				{
					slowMerge();
				}
				if ( DEBUG_ALLOCATIONS )
				{
					final int released = list.stream().mapToInt( l -> l.size ).sum();
					System.out.println("RELEASE: "+released+" => "+this);
				}
			}
		}
	}

	private void releaseParticles(Subpool pool)
	{
		if ( ! usedPools.remove( pool ) ) {
			throw new IllegalArgumentException("Failed to release pool "+pool+" , maybe already freed ?");
		}
		int idx = 0;
		final int len = availablePools.size();
		for ( ; idx < len ; idx++ )
		{
			final Subpool current = availablePools.get(idx);
			if ( pool.startIndex < current.startIndex )
			{
				if ( current.canBeMergedWith( pool ) )
				{
					availablePools.set( idx , current.createMerged( pool ) );
				} else {
					availablePools.add( idx , pool );
				}
				return;
			}
		}
		availablePools.add(pool);
	}

	public void slowMerge()
	{
		boolean merged;
		do
		{
			merged = false;
			int len = availablePools.size();
			for ( int i =0 ; i < len-1 ; i++ )
			{
				final Subpool pool1 = availablePools.get(i);
				final Subpool pool2 = availablePools.get(i+1);
				if ( pool1.canBeMergedWith( pool2 ) )
				{
					availablePools.set( i , pool1.createMerged( pool2 ) );
					availablePools.remove(i+1);
					len--;
					i--;
					merged = true;
				}
			}
		} while ( merged );

		releaseCount = 0;
	}

	public List<Subpool> allocateParticles(int count) {

		final List<Subpool> result = new ArrayList<>(5);
		int stillNeeded = count;

		synchronized( availablePools )
		{
			synchronized( usedPools)
			{
				for ( int i = 0,len = availablePools.size() ; i < len && stillNeeded > 0 ; i++)
				{
					final Subpool pool = availablePools.get(i);
					if ( pool.size > stillNeeded )
					{
						// pool bigger than necessary, split it
						result.add( splitAndPutOnUsed( pool , stillNeeded ) );
						if ( DEBUG_ALLOCATIONS ) {
							System.out.println("ALLOC: "+count+" => "+this);
						}
						return result;
					}

					availablePools.remove( pool );
					usedPools.add( pool );
					result.add( pool );
					stillNeeded -= pool.size;
				}
				if ( stillNeeded > 0 ) {
					result.add( extendPoolAndPutOnUsed( stillNeeded ) );
				}

				if ( DEBUG_ALLOCATIONS ) {
					System.out.println("ALLOC: "+count+" => "+this);
				}
			}
		}
		return result;
	}

	private Subpool splitAndPutOnUsed(Subpool pool,int desiredCount)
	{
		final int remainingItemsCount = pool.size - desiredCount;
		if ( remainingItemsCount < 0 ) {
			throw new IllegalArgumentException("Pool to small - unable to split pool "+pool+" with desired size "+desiredCount);
		}

		final Subpool result = new Subpool( pool.startIndex , pool.startIndex + desiredCount );
		availablePools.remove( pool );
		if ( remainingItemsCount > 0 ) {
			availablePools.add( new Subpool(result.endIndex, result.endIndex+remainingItemsCount) );
		}
		usedPools.add( result );
		return result;
	}

	@Override
	public String toString()
	{
		synchronized( availablePools )
		{
			synchronized( usedPools)
			{
				final int used = usedPools.stream().mapToInt( pool -> pool.size ).sum();
				final int available = availablePools.stream().mapToInt( pool -> pool.size ).sum();
				return "Pool[ size: "+this.particlePool.length+" , available: "+available+" , used: "+used+" ]";
			}
		}
	}
}