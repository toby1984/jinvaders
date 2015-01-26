package de.codesourcery.jinvaders.sound;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;

import de.codesourcery.jinvaders.Main;

public enum SoundEffect
{
	PLAYER_SHOOTING("/player_laser.wav",Volume.LOUD , 3),
	ONE_LIFE_LOST("/one_life_lost.wav",Volume.LOUD , 2),
	INVADER_SHOOTING("/invader_laser.wav",Volume.QUIET , 3),
	GAME_OVER("/gameover.wav",Volume.NOT_SO_LOUD, 1),
	WAVE_COMPLETED("/wave_completed.wav",Volume.LOUD, 1),
	INVADER_DESTROYED("/invader_destroyed.wav",Volume.LOUD, 3);

	private final Clip[] clips;

	private final int concurrency; // maximum number of instances of this sound that may be playing at the same time
	private int currentClipIdx;

	private SoundEffect(String soundFileName,Volume volume,int concurrency)
	{
		this.concurrency = concurrency;
		this.clips = new Clip[ concurrency ];
		try
		{
			final URL url = Main.class.getResource(soundFileName);
			if ( url == null ) {
				throw new RuntimeException("Failed to locate sound on classpath: '"+soundFileName+"'");
			}
			for ( int i = 0 ; i < concurrency ; i++ )
			{
				clips[i] = createClip( AudioSystem.getAudioInputStream(url) , volume );
			}
		}
		catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Clip createClip(AudioInputStream stream,Volume volume) throws LineUnavailableException, IOException
	{
		final Clip clip = AudioSystem.getClip();
		clip.open( stream );

		final FloatControl masterGain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

		final float range = masterGain.getMaximum() - masterGain.getMinimum();
		final float value = masterGain.getMinimum() + range*volume.gainPercentage;
		masterGain.setValue( value );
		return clip;
	}

	public void play()
	{
		currentClipIdx = (currentClipIdx+1) % concurrency;
		final Clip clip = clips[ currentClipIdx ];
		if ( ! clip.isActive() ) {
			clip.setFramePosition(0);
			clip.start();
		}
	}
}