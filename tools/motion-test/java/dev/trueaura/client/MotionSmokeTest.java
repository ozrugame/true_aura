package dev.trueaura.client;

import com.mojang.logging.LogUtils;
import dev.trueaura.TrueAura;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in development harness, absent from production JARs. */
@Mod.EventBusSubscriber(modid=TrueAura.MOD_ID,value=Dist.CLIENT)
public final class MotionSmokeTest {
    private static int ticks;
    private static boolean failed;
    private static String shot;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc=Minecraft.getInstance();
        if(event.phase!=TickEvent.Phase.END || failed || mc.player==null || mc.level==null) return;
        ticks++;
        try {
            mc.options.pauseOnLostFocus=false;
            if(ticks%20==0) LogUtils.getLogger().info("AURA_MOTION_TEST tick={} paused={} screen={} enabled={}",ticks,mc.isPaused(),mc.screen==null?"none":mc.screen.getClass().getSimpleName(),AuraClient.isAuraEnabled());
            if(mc.getSingleplayerServer()==null) { multiplayer(mc); return; }
            if(ticks==20) {
                mc.setScreen(null); mc.options.hideGui=true; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                var server=mc.getSingleplayerServer();
                server.execute(() -> {
                    for(String cmd:new String[]{"tp AuraMotionTest 0 -60 0 0 0","time set 6000","weather clear"})
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),cmd);
                });
            }
            if(ticks==40) AuraClient.saveSettings(dev.trueaura.aura.AuraSettings.DEFAULT
                .withAppearance(1.2,1,1,dev.trueaura.aura.AuraShape.FLAME)
                .withEffect(true,dev.trueaura.aura.AuraEffect.NONE).withEffect(false,dev.trueaura.aura.AuraEffect.NONE)
                .withAnimation(new dev.trueaura.aura.AuraAnimation(false,false,40,20,1.5,true)));
            if(ticks==60) command("aura off");
            if(ticks==100) mc.setScreen(new AuraSettingsScreen(null));
            if(ticks==110) click(mc,panelWidth(mc)-28,46);
            if(ticks==130) shot="settings";
            if(ticks==140) click(mc,panelWidth(mc)-28,74);
            if(ticks>=140 && ticks<=230) require(!AuraClient.isAuraEnabled(),"preview changed actual enabled state");
            if(ticks==145) shot="rise-feet";
            if(ticks==150) shot="rise-knees";
            if(ticks==160) { require(AuraClient.previewOpacity(1)>.4 && AuraClient.previewOpacity(1)<.6,"preview fade-in duration");shot="preview-fade"; }
            if(ticks==178) shot="rise-full";
            if(ticks==200) shot="preview-out";
            if(ticks==240) { click(mc,20,74); click(mc,20,126); }
            if(ticks==245) shot="instant-settings";
            if(ticks==250) click(mc,20,218);
            if(ticks==270) require(AuraClient.settings().animation().instantAppear() && AuraClient.settings().animation().instantDisappear(),"UI modes not saved");
            if(ticks==280) command("aura on");
            if(ticks==283) { require(state().opacity(0)==1,"instant appearance first frame");shot="instant"; }
            if(ticks==310) command("aura off");
            if(ticks==313) require(state().opacity(0)==0,"instant disappearance first frame");
            if(ticks==330) command("aura set appear rise");
            if(ticks==340) command("aura on");
            if(ticks==345) { require(state().opacity(1)>0 && state().opacity(1)<.25,"runtime fade-in");shot="fade-early"; }
            if(ticks==390) { require(state().opacity(1)>.99,"fade completed");shot="rest"; }
            if(ticks==400) { mc.player.setYRot(90);mc.options.keyUp.setDown(true); }
            if(ticks==420) { require(state().motion().sample(1).energy()>.1,"walking does not excite spring"); logMotion("walking");shot="walking"; }
            if(ticks==430) mc.player.setYRot(-90);
            if(ticks==435) { logMotion("turning");shot="turning"; }
            if(ticks==440) mc.options.keyJump.setDown(true);
            if(ticks==442) mc.options.keyJump.setDown(false);
            if(ticks==447) { logMotion("jumping");shot="jumping"; }
            if(ticks==455) mc.options.keyUp.setDown(false);
            if(ticks==456) { logMotion("stopping");shot="stopping"; }
            if(ticks==510) { require(state().motion().sample(1).energy()<.05,"spring did not settle");shot="settled"; }
            if(ticks==520) AuraClient.saveSettings(AuraClient.settings().withCustom(CustomAuraLibrary.reload().entries().get(0)));
            if(ticks==530) mc.options.keyUp.setDown(true);
            if(ticks==550) { require(AuraClient.settings().custom()!=null,"import lost");shot="custom-moving"; }
            if(ticks==570) mc.options.keyUp.setDown(false);
            if(ticks==620) command("aura set motion 0");
            if(ticks==640) { require(AuraClient.settings().animation().motionStrength()==0,"disable motion setting");shot="motion-off"; }
            if(ticks==650) LogUtils.getLogger().info("AURA_MOTION_TEST COMPLETE: UI, preview, modes, duration, movement, turn, jump, settle, import, disable");
            if(ticks==680) mc.stop();
        } catch(Throwable ex) {
            failed=true; mc.options.keyUp.setDown(false);mc.options.keyJump.setDown(false);
            LogUtils.getLogger().error("AURA_MOTION_TEST FAILED tick="+ticks,ex);
        }
    }
    private static void multiplayer(Minecraft mc) throws Exception {
        boolean owner=mc.player.getGameProfile().getName().equals("AuraDev");
        if(ticks==40) { mc.options.hideGui=true; mc.options.setCameraType(owner?CameraType.THIRD_PERSON_FRONT:CameraType.FIRST_PERSON); }
        if(ticks==80 && owner) for(String cmd:new String[]{"aura set size 1","aura set intensity 1.2","aura set appear instant","aura set disappear fade","aura set fade_out 2","aura set motion 1.8","aura on"}) command(cmd);
        if(ticks==160 && owner) {mc.player.setYRot(0);mc.options.keyUp.setDown(true);}
        if(ticks==190 && !owner) {
            var other=mc.level.players().stream().filter(p->p!=mc.player && p.getGameProfile().getName().equals("AuraDev")).findFirst().orElseThrow();
            var visual=AuraClient.remote(other.getUUID());
            require(visual!=null && visual.settings().animation().instantAppear() && visual.settings().animation().disappearTicks()==40
                && visual.settings().animation().motionStrength()==1.8,"remote animation settings");
            require(visual.state().motion().sample(1).energy()>.05,"remote movement spring");
            LogUtils.getLogger().info("AURA_MOTION_TEST REMOTE VERIFIED settings={} motion={}",visual.settings().animation(),visual.state().motion().sample(1));
            shot="remote-moving";
        }
        if(ticks==210 && owner) mc.options.keyUp.setDown(false);
    }
    private static AuraState state() throws Exception {
        var f=AuraClient.class.getDeclaredField("STATE");f.setAccessible(true);return (AuraState)f.get(null);
    }
    private static void logMotion(String label) throws Exception { LogUtils.getLogger().info("AURA_MOTION_TEST {} {}",label,state().motion().sample(1)); }
    private static int panelWidth(Minecraft mc) { return Math.min(218,Math.max(148,mc.screen.width/2-24)); }
    private static void click(Minecraft mc,int dx,int dy) {
        var screen=mc.screen;int x=screen.width-panelWidth(mc)-8,y=Math.max(4,(screen.height-232)/2);
        screen.mouseClicked(x+dx,y+dy,0);screen.mouseReleased(x+dx,y+dy,0);
    }
    private static void command(String command) { Minecraft.getInstance().getConnection().sendCommand(command); }
    private static void require(boolean condition,String message) { if(!condition) throw new IllegalStateException(message); }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || shot==null) return;
        var mc=Minecraft.getInstance();String name="aura-motion-"+shot+".png";shot=null;
        Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->LogUtils.getLogger().info("AURA_MOTION_TEST {}",message.getString()));
    }
}
