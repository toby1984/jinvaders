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

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public interface IRenderer
{
	public void initialize(Object context);

	public FontHolder getFont(FontKey key);

	public ImageHolder convertImage(BufferedImage image);

	public void setColor(int color);

	public void setBackgroundColor(int color);

	public void setFont(FontHolder font);

	public void drawString(String text,int x,int y);

	public Rectangle getStringBounds(String text);

	public void clearScreen();

	public void fillRect(int x,int y,int width,int height);

	public void drawRect(int x,int y,int width,int height);

	public void renderPoint(int x,int y,int color);

	public void renderPoint(ImageHolder holder,int x,int y,int color);

	public int queryPoint(ImageHolder holder,int x,int y);

	public void renderImage(ImageHolder image,int x,int y);

	public void begin();

	public void end(Object context);

	public float getAscent(String text);

	public float getDescent(String text);

	public float getFPS();

	public void renderCircle(int x, int y, int radius,boolean fill);
}