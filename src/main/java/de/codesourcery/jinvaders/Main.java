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

import javax.swing.JFrame;
import javax.swing.Timer;

public class Main
{
	public static void main(String[] args)
	{
		final Game game = new Game();

		// setup game screen
		final JFrame frame = new JFrame("JavaInvaders");
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		frame.getContentPane().add(game);
		frame.pack();
		frame.setResizable(false); // resizing would only make sense if rendering would adapt to the screen size...which is currently not implemented
		frame.setVisible(true);

		// Swing thread/timer that redraws game screen
		new Timer( 1000 / Constants.TICKS_PER_SECOND , event -> game.repaint() ).start();

		// request focus so that game screen receives keyboard input
		game.requestFocus();

		// main game loop
		while(true)
		{
			// busy-wait until EDT has redrawn screen
			// (spinning is wasteful but rendering shouldn't take long
			// and using a lock & wait() here will probably be more costly because of
			// context switching)
			while( ! game.screenRendered.compareAndSet(true,false) ) { }

			game.tick();
		}
	}
}