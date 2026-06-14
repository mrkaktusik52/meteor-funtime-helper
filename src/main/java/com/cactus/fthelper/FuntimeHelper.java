package com.cactus.fthelper;


import com.cactus.fthelper.modules.AntiAFKPlusModule;
import com.cactus.fthelper.modules.AutoSellerModule;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;

import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class FuntimeHelper extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("FT Helper");

    @Override
    public void onInitialize() {
        LOG.info("Initializing FT Helper");

        Modules.get().add(new AutoSellerModule());
        Modules.get().add(new AntiAFKPlusModule());
    }


    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.cactus.fthelper";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("mrkaktusik52", "meteor-funtime-helper");
    }
}
