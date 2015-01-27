package de.codesourcery.jinvaders.particles;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import de.codesourcery.jinvaders.particles.ParticlePool.Subpool;

public class ParticlePoolTest extends TestCase {

	private ParticlePool pool;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testAllocFromEmptyPool()
	{
		pool = new ParticlePool(0);

		// allocate
		final List<Subpool> allocated = pool.allocateParticles( 100 );
		assertEquals(1,allocated.size());
		Subpool sub = allocated.get(0);
		assertNotNull(sub);
		assertEquals(0,sub.startIndex);
		assertEquals(100,sub.endIndex);
		assertEquals( 100 , sub.size );

		assertInitialized( sub );

		assertEquals( 0 , pool.availablePools.size() );
		assertEquals( 1 , pool.usedPools.size() );
		assertTrue( pool.usedPools.contains( sub ) );

		// release

		pool.releaseParticles( Arrays.asList( sub ) );

		assertEquals( 1 , pool.availablePools.size() );
		assertEquals( 0 , pool.usedPools.size() );
		assertTrue( pool.availablePools.contains( sub ) );

		try {
			pool.releaseParticles( Arrays.asList( sub ) );
			fail("Double-free should've failed");
		} catch(Exception e) {
			/* ok */
		}
	}

	public void testAllocFromPoolWithSameSize()
	{
		pool = new ParticlePool(100);

		// allocate
		final List<Subpool> allocated = pool.allocateParticles( 100 );
		assertEquals(1,allocated.size());
		Subpool sub = allocated.get(0);
		assertNotNull(sub);
		assertEquals(0,sub.startIndex);
		assertEquals(100,sub.endIndex);
		assertEquals( 100 , sub.size );

		assertInitialized( sub );

		assertEquals( 0 , pool.availablePools.size() );
		assertEquals( 1 , pool.usedPools.size() );
		assertTrue( pool.usedPools.contains( sub ) );

		// release

		pool.releaseParticles( Arrays.asList( sub ) );

		assertEquals( 1 , pool.availablePools.size() );
		assertEquals( 0 , pool.usedPools.size() );
		assertTrue( pool.availablePools.contains( sub ) );

		try {
			pool.releaseParticles( Arrays.asList( sub ) );
			fail("Double-free should've failed");
		} catch(Exception e) {
			/* ok */
		}
	}

	public void testAllocFromPoolLargerSize()
	{
		pool = new ParticlePool(200);

		// allocate
		final List<Subpool> allocated = pool.allocateParticles( 100 );
		assertEquals(1,allocated.size());

		Subpool sub = allocated.get(0);
		assertNotNull(sub);
		assertEquals(0,sub.startIndex);
		assertEquals(100,sub.endIndex);
		assertEquals( 100 , sub.size );

		assertInitialized( sub );

		assertEquals( 1 , pool.availablePools.size() );
		assertTrue( pool.availablePools.contains( new Subpool( 100 , 200 ) ) );

		assertEquals( 1 , pool.usedPools.size() );
		assertTrue( pool.usedPools.contains( sub ) );

		// release
		pool.releaseParticles( Arrays.asList( sub ) );

		assertEquals( 1 , pool.availablePools.size() );
		assertEquals( 0 , pool.usedPools.size() );
		assertTrue( pool.availablePools.contains( new Subpool( 0 , 200 ) ) );

		try {
			pool.releaseParticles( Arrays.asList( sub ) );
			fail("Double-free should've failed");
		} catch(Exception e) {
			/* ok */
		}
	}

	private void assertInitialized(List<Subpool> list)
	{
		list.forEach( this::assertInitialized );
	}

	private void assertInitialized(Subpool sub)
	{
		for ( int i = sub.startIndex ; i < sub.endIndex ; i++ ) {
			assertNotNull( pool.particlePool[i] );
		}
	}

	public void testDefragmentation()
	{
		pool = new ParticlePool(60);

		// allocate
		final List<Subpool> allocated1 = pool.allocateParticles( 10 );
		final List<Subpool> allocated2 = pool.allocateParticles( 20 );
		final List<Subpool> allocated3 = pool.allocateParticles( 30 );

		assertEquals( 1 , allocated1.size());
		assertEquals( 1 , allocated2.size());
		assertEquals( 1 , allocated3.size());

		assertInitialized( allocated1 );
		assertInitialized( allocated2 );
		assertInitialized( allocated3 );

		assertEquals( 0 , pool.availablePools.size() );
		assertEquals( 3 , pool.usedPools.size() );

		allocated1.forEach( a -> assertTrue( pool.usedPools.contains( a ) ) );
		allocated2.forEach( a -> assertTrue( pool.usedPools.contains( a ) ) );
		allocated3.forEach( a -> assertTrue( pool.usedPools.contains( a ) ) );

		// release
		pool.releaseParticles( allocated2 );
		pool.releaseParticles( allocated1 );
		pool.releaseParticles( allocated3 );

		pool.slowMerge();

		assertEquals( pool.toString() , 1 , pool.availablePools.size() );
		assertEquals( 0 , pool.usedPools.size() );
		assertTrue( pool.availablePools.contains( new Subpool( 0 , 60 ) ) );
	}
}
