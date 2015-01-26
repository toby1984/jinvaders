# jinvaders

A crude Space Invaders clone programmed in Java. I got inspired by [a great talk by Mary Rose Cook](http://vimeo.com/105955605) ... Javascript is still an atrocious language though :D

<img src="https://raw.githubusercontent.com/toby1984/jinvaders/master/screenshot.png" width="640" height="480" />

Attribution
===========

### Font
The nice-looking retro font was released under the SIL Open Font License V1.1 and is (c) 2011 by Cody "CodeMan38" Boisclair (cody@zone38.net).

### Sound effects

I downloaded the retro sound effects from www.freesounds.org , authors are

- http://www.freesound.org/people/killkhan : explosion-3.mp3
- http://www.freesound.org/people/inferno : smalllas.wav
- http://www.freesound.org/people/soundslikewillem : laser-gun.wav
- http://www.freesound.org/people/coby12388 : bombhit01.wav
- http://www.freesound.org/people/themusicalnomad : negative-beeps.wav
- http://www.freesound.org/people/snipperbes : classiclaser.wav
- http://www.freesound.org/people/suntemple: retro-accomplished-sfx.wav

Requirements
============

- JDK >= 1.8
- Maven2 or later

Running
=======

mvn clean package exec:java

Playing
=======

Use the 'A' and 'D' keys to move left/right , hit 'SPACE' to shoot. Press 'ENTER' to start a new game after you lost.

Known issues
============

Here's a list of known issues I might (or might not) fix:

- I didn't really bother with any performance optimizations since the game never dipped below 60 fps on my dated i5-2500k but there's plenty that could be done (remove object allocations, remove copying of entity references in main loop, replace lambdas/stream.filter() code with traditional loops,faster/smarter collision checking)
- highscore screen looks kinda 'meh' , as well as the screen for entering your name
- highscores currently do not get persisted
- Main loop method is a mess since I kept adding features and didn't bother to clean it up
- The algorithm that checks whether it's safe for an invader to shoot (making sure that none of its collegues is in the way)
  is quite naive and just makes sure that there's no other invader right below. Invaders might still get hit by
  friendly fire when they advance horizontally into a bullet. To work around this I currently made sure that the 
  collision detection never triggers on bullets fired  by invaders that hit another invader. This obviously sometimes
  leads to visual glitches were shots are flying through invaders...
- When the player shoots while right below a barrier, the shots are invisible (the barrier gets properly destroyed though)
- Difficulty is kind'a wonky and probably needs adjusting
- Sprites look like done by a three-year-old (I'd gladly accept contributions though ;)
