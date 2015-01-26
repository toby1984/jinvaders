package de.codesourcery.jinvaders;

import java.text.DecimalFormat;

public final class HighscoreEntry implements Comparable<HighscoreEntry>
{
	public final String name;
	public final int score;

	public HighscoreEntry(String name, int score) {
		this.name = name;
		this.score = score;
	}

	private String leftPad(String s, int len , char padding)
	{
		int delta = len - s.length();
		String pad = "";
		while( delta > 0 ) {
			pad += padding;
			delta--;
		}
		return pad+s;
	}

	private String rightPad(String s, int len , char padding)
	{
		int delta = len - s.length();
		String pad = "";
		while( delta > 0 ) {
			pad += padding;
			delta--;
		}
		return s+pad;
	}

	@Override
	public String toString()
	{
		final DecimalFormat scoreFormat = new DecimalFormat("000000");
		return rightPad( name , 8 , '_' )+"   "+scoreFormat.format( score );
	}

	@Override
	public int compareTo(HighscoreEntry o) {
		return Integer.compare( o.score,  this.score );
	}
}