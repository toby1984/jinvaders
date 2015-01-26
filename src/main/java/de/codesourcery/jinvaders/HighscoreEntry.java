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

import java.text.DecimalFormat;

public final class HighscoreEntry implements Comparable<HighscoreEntry>
{
	public final String name;
	public final int score;

	public HighscoreEntry(String name, int score) {
		this.name = name;
		this.score = score;
	}

	private static String leftPad(String s, int len , char padding)
	{
		int delta = len - s.length();
		String pad = "";
		while( delta > 0 ) {
			pad += padding;
			delta--;
		}
		return pad+s;
	}

	private static String rightPad(String s, int len , char padding)
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
		return format( this );
	}

	public static String format(String name,int score) {
		final DecimalFormat scoreFormat = new DecimalFormat("000000");
		return rightPad( name , 8 , '_' )+"   "+scoreFormat.format( score );
	}

	public static String format(HighscoreEntry entry) {
		return format(entry.name,entry.score);
	}

	@Override
	public int compareTo(HighscoreEntry o) {
		return Integer.compare( o.score,  this.score );
	}
}