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
package de.codesourcery.jinvaders.particles;

import de.codesourcery.jinvaders.graphics.Vec2df;

public final class Particle
{
	public final Vec2df position=new Vec2df();
	public final Vec2df acceleration=new Vec2df();
	public final Vec2df velocity=new Vec2df();

	public boolean isAlive;
	public float lifetimeLeft;
	public int color;
}
