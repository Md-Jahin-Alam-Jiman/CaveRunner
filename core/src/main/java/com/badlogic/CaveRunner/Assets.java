package com.badlogic.CaveRunner;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Module 3 rendering assets.
 *
 * A single white texture is enough because SpriteBatch
 * can tint it into different tile colors.
 */
public class Assets implements Disposable {

    private final Texture pixelTexture;
    private final TextureRegion pixel;

    public Assets() {

        Pixmap pixmap = new Pixmap(
                1,
                1,
                Pixmap.Format.RGBA8888
        );

        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();

        pixelTexture = new Texture(pixmap);

        pixmap.dispose();

        pixel = new TextureRegion(pixelTexture);
    }

    /**
     * Returns the 1x1 white texture region.
     */
    public TextureRegion pixel() {
        return pixel;
    }

    @Override
    public void dispose() {
        pixelTexture.dispose();
    }
}
