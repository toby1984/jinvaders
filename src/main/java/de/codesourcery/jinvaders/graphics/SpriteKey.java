package de.codesourcery.jinvaders.graphics;

public final class SpriteKey {

	private final String name;

	public SpriteKey(String name) {
		this.name = name;
		if ( name == null || name.trim().length() == 0 ) {
			throw new IllegalArgumentException("Sprite key must not be blank");
		}
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof SpriteKey)
		{
			return this.name.equals( ((SpriteKey) obj).name );
		}
		return false;
	}

	@Override
	public String toString() {
		return name;
	}
}