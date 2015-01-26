/**
 * Copyright 2015 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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