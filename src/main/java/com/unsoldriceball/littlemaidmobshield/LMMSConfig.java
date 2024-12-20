package com.unsoldriceball.littlemaidmobshield;



import static com.unsoldriceball.littlemaidmobshield.LMMSMain.ID_MOD;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;




@Config(modid = ID_MOD)
public class LMMSConfig
{
    @Config.RequiresMcRestart
    @Config.RangeInt(min = 0)
    public static int requireShieldDurabilityForInvincible = 1344;


    @Mod.EventBusSubscriber(modid = ID_MOD)
    private static class EventHandler
    {
        //Configが変更されたときに呼び出される。変更を適用する関数。
        @SubscribeEvent
        public static void onConfigChanged(final ConfigChangedEvent.OnConfigChangedEvent event)
        {
            if (event.getModID().equals(ID_MOD))
            {
                ConfigManager.sync(ID_MOD, Config.Type.INSTANCE);
            }
        }
    }
}
