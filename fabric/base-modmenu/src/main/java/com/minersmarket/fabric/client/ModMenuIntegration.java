package com.minersmarket.fabric.client;

import com.minersmarket.config.client.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Optional ModMenu integration. ModMenu is a compileOnly dependency: this class is
 * only instantiated by ModMenu itself, so it is inert when ModMenu is not installed.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
