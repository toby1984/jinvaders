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
package de.codesourcery.jinvaders.sound;

/**
 * Sound volume levels we're using.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public enum Volume
{
	LOUD(0.9f),
	NOT_SO_LOUD(0.7f),
	QUIET(0.5f);

	public final float gainPercentage;

	private Volume(float gainPercentage) { this.gainPercentage = gainPercentage; }
}