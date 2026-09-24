package com.badlogic.CaveRunner.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.CaveRunner.Config;
import com.badlogic.CaveRunner.Main;

public class Lwjgl3Launcher {

    public static void main(String[] args) {

        Lwjgl3ApplicationConfiguration config =
                new Lwjgl3ApplicationConfiguration();

        config.setTitle(Config.TITLE);

        config.setWindowedMode(960, 540);

        config.useVsync(true);

        config.setForegroundFPS(60);

        new Lwjgl3Application(
                new Main(),
                config
        );
    }
}