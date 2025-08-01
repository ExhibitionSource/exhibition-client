/*
 * Copyright (c) MineSense.pub 2018.
 * Developed by Arithmo
 */

package exhibition.module.impl.player;

import exhibition.event.Event;
import exhibition.event.RegisterEvent;
import exhibition.event.impl.EventPacket;
import exhibition.event.impl.EventTick;
import exhibition.module.Module;
import exhibition.module.data.ModuleData;
import exhibition.util.PlayerUtil;
import net.minecraft.network.play.client.C03PacketPlayer;

public class PotionSaver extends Module {

    public PotionSaver(ModuleData data) {
        super(data);
    }

    @RegisterEvent(events = {EventPacket.class, EventTick.class})
    public void onEvent(Event event) {
        if(mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if(event instanceof EventPacket) {
            EventPacket ep = event.cast();
            if(ep.getPacket() instanceof C03PacketPlayer && !PlayerUtil.isMoving()) {
                ep.setCancelled(true);
            }
        }
    }

}
