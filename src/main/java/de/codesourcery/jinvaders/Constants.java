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
package de.codesourcery.jinvaders;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;

import de.codesourcery.jinvaders.graphics.Vec2d;

public class Constants {

	// total size of screen
	public static final Dimension SCREEN_SIZE = new Dimension(800,600);

	// part of screen where we're going to draw the game action
	// (=> screen minus HUD below it)
	public static final Rectangle VIEWPORT = new Rectangle(0,100,800,SCREEN_SIZE.height - 100 );

	public static final int INVADERS_PER_ROW = 8;

	public static final int ROWS_OF_INVADERS = 5;

	public static final int BARRICADE_COUNT = 5;

	public static final Color BACKGROUND_COLOR = Color.BLACK;

	// entity velocities
	public static final Vec2d INITIAL_INVADER_VELOCITY = new Vec2d(2,1);
	public static final int INVADER_BULLET_VELOCITY = 4;
	public static final int PLAYER_VELOCITY = 3;
	public static final int PLAYER_BULLET_VELOCITY = 4;
	public static final double INVADER_FIRING_PROBABILITY = 0.995; // 0.995;
	public static final int PLAYER_LIFES = 3;

	// number of ticks that needs to be elapsed between subsequent shots by the player
	public static final int TICKS_PER_SHOT_LIMIT = 10;

	public static final int MAX_PLAYER_BULLETS_IN_FLIGHT = 2;

	// FPS target
	public static final int TICKS_PER_SECOND = 60;

	// time (in ticks) after which we're going to automatically bump the difficulty
	// if the player fails to destroy all invaders on the current level
	public static final int DIFFICULITY_INCREASE_AFTER_TICKS = 90 * TICKS_PER_SECOND;
}