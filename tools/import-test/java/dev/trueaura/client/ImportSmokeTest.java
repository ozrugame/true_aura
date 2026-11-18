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
import java.nio.file.Files;

/** Opt-in development source set. Never shipped in the mod jar. */
@Mod.EventBusSubscriber(modid=TrueAura.MOD_ID,value=Dist.CLIENT)
public final class ImportSmokeTest {
    private static int ticks;
    private static String shot, original;
    private static boolean failed;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var mc=Minecraft.getInstance();
        if(event.phase!=TickEvent.Phase.END || failed || mc.player==null || mc.level==null) return;
        ticks++;
        try {
            mc.options.pauseOnLostFocus=false;
            if(mc.getSingleplayerServer()==null) { multiplayer(mc); return; }
            if(ticks==20) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.options.hideGui=true;
                var server=mc.getSingleplayerServer();
                server.execute(() -> {
                    for(String cmd:new String[]{"tp AuraImportTest 0 -60 0 0 0","time set 6000","weather clear"})
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),cmd);
                });
            }
            if(ticks==60) { command("aura set size 1"); command("aura set intensity 1.2"); command("aura on"); }
            if(ticks==100) mc.setScreen(new AuraSettingsScreen(null));
            if(ticks==110) click(mc,20,150);
            if(ticks==120) click(mc,panelWidth(mc)-22,192);
            if(ticks==140) shot="picker";
            if(ticks==160) click(mc,20,118); // Page 2, third entry: Moon Petals
            if(ticks==190) { require(AuraClient.renderSettings().custom()!=null,"picker selection"); shot="preview"; }
            if(ticks==210) click(mc,20,218);
            if(ticks==230) {
                require(AuraClient.settings().custom()!=null,"saved custom config");
                mc.setScreen(new AuraSettingsScreen(null));
                var path=CustomAuraLibrary.folder().resolve("moon_petals.json");
                original=Files.readString(path);
                Files.writeString(path,original.replace("Moon Petals","Moon Petals Reloaded").replace("1.5","2.0"));
                Files.writeString(CustomAuraLibrary.folder().resolve("broken-test.json"),"{invalid");
            }
            if(ticks==240) click(mc,20,150);
            if(ticks==250) click(mc,panelWidth(mc)-30,72);
            if(ticks==275) {
                require(AuraClient.renderSettings().custom().name().equals("Moon Petals Reloaded"),"reload updates draft");
                require(CustomAuraLibrary.current().errors().size()==1,"bad file diagnostic"); shot="reload";
            }
            if(ticks==290) click(mc,20,218);
            if(ticks==310) { command("aura set intensity 3"); command("aura set color main aqua"); }
            if(ticks==340) {
                require(AuraClient.settings().custom()!=null && AuraClient.settings().intensity()==3,"commands preserve imported shape");
                shot="saved";
            }
            if(ticks==370) command("aura off");
            if(ticks==410) { require(!AuraClient.isAuraEnabled(),"toggle off"); shot="off"; }
            if(ticks==430) {
                command("aura set shape flame");
                if(original!=null) Files.writeString(CustomAuraLibrary.folder().resolve("moon_petals.json"),original);
                Files.deleteIfExists(CustomAuraLibrary.folder().resolve("broken-test.json"));
            }
            if(ticks==460) {
                require(AuraClient.settings().custom()==null,"builtin selection clears import");
                LogUtils.getLogger().info("AURA_IMPORT_TEST COMPLETE: picker, reload, diagnostics, persistence, commands, toggle");
            }
            if(ticks==500) mc.stop();
        } catch(Throwable ex) { failed=true; LogUtils.getLogger().error("AURA_IMPORT_TEST FAILED at tick "+ticks,ex); }
    }
    private static void multiplayer(Minecraft mc) {
        boolean owner=mc.player.getGameProfile().getName().equals("AuraDev");
        if(ticks==40) { mc.options.hideGui=true; mc.options.setCameraType(CameraType.FIRST_PERSON); }
        if(ticks==100 && owner) {
            var def=CustomAuraLibrary.reload().entries().get(0);
            AuraClient.saveSettings(AuraClient.settings().withCustom(def).withColors(0x36DFFF,0xDDFFFF,0x9955FF));
        }
        if(ticks==140 && owner) command("aura on");
        if(ticks==300 && !owner) {
            var other=mc.level.players().stream().filter(p -> p!=mc.player && p.getGameProfile().getName().equals("AuraDev")).findFirst().orElseThrow();
            var visual=AuraClient.remote(other.getUUID());
            require(visual!=null && visual.settings().custom()!=null,"remote definition received");
            require(visual.state().enabled(),"remote enabled state");
            require(!Files.exists(CustomAuraLibrary.folder()),"viewer has no local imported files");
            LogUtils.getLogger().info("AURA_IMPORT_TEST REMOTE VERIFIED id={} layers={} localFiles=false",visual.settings().custom().id(),visual.settings().custom().layers().size());
            shot="remote";
        }
    }
    private static int panelWidth(Minecraft mc) { return Math.min(218,Math.max(148,mc.screen.width/2-24)); }
    private static void click(Minecraft mc,int dx,int dy) {
        int x=mc.screen.width-panelWidth(mc)-8, y=Math.max(4,(mc.screen.height-232)/2);
        var screen=mc.screen; screen.mouseClicked(x+dx,y+dy,0); screen.mouseReleased(x+dx,y+dy,0);
    }
    private static void command(String cmd) { Minecraft.getInstance().getConnection().sendCommand(cmd); }
    private static void require(boolean condition,String label) { if(!condition) throw new IllegalStateException(label); }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        var mc=Minecraft.getInstance();
        if(event.phase!=TickEvent.Phase.END || shot==null) return;
        String name="aura-import-"+shot+".png"; shot=null;
        Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message -> LogUtils.getLogger().info("AURA_IMPORT_TEST {}",message.getString()));
    }
}
