package com.example.omegasnipers;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(OmegaSnipersMod.MOD_ID)
public class OmegaSnipersMod {

    public static final String MOD_ID = "omegasnipers";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OmegaSnipersMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        OmegaCreativeTab.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(new OmegaCombatHandler());
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(OmegaSniperRegistry::refresh);
    }

    private void onServerStarting(final ServerStartingEvent event) {
        // Authoritative refresh: by now every TaCZ addon's gun pack has finished loading.
        OmegaSniperRegistry.refresh();
    }

    private void onRegisterCommands(final RegisterCommandsEvent event) {
        OmegaCommands.register(event.getDispatcher());
    }
}
