package com.tac.guns.client.handler;

import com.tac.guns.Reference;
import com.tac.guns.client.KeyBinds;
import com.tac.guns.client.render.animation.M1014AnimationController;
import com.tac.guns.client.render.animation.*;
import com.tac.guns.client.render.animation.module.*;
import com.tac.guns.common.Gun;
import com.tac.guns.event.GunFireEvent;
import com.tac.guns.event.GunReloadEvent;
import com.tac.guns.item.GunItem;
import com.tac.guns.util.GunEnchantmentHelper;
import de.javagl.jgltf.model.animation.AnimationRunner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Mainly controls when the animation should play.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public enum AnimationHandler {
    INSTANCE;

    public static void preloadAnimations(){
        AA12AnimationController.getInstance();
        Dp28AnimationController.getInstance();
        Glock17AnimationController.getInstance();
        HkMp5a5AnimationController.getInstance();
        HK416A5AnimationController.getInstance();
        M870AnimationController.getInstance();
        Mp7AnimationController.getInstance();
        Type81AnimationController.getInstance();
        Ak47AnimationController.getInstance();
        AWPAnimationController.getInstance();
        M60AnimationController.getInstance();
        M1014AnimationController.getInstance();
    }

    public void onGunReload(boolean reloading, ItemStack itemStack) {
        PlayerEntity player = Minecraft.getInstance().player;
        if (player == null) return;
        if (itemStack.getItem() instanceof GunItem) {
            GunItem gunItem = (GunItem) itemStack.getItem();
            CompoundNBT tag = itemStack.getOrCreateTag();
            int reloadingAmount = GunEnchantmentHelper.getAmmoCapacity(itemStack, gunItem.getGun()) - tag.getInt("AmmoCount");
            if (reloadingAmount <= 0) return;
        }
        GunAnimationController controller = GunAnimationController.fromItem(itemStack.getItem());
        if (controller == null) return;
        if(!reloading)
            return;
        AnimationMeta reloadEmptyMeta = controller.getAnimationFromLabel(GunAnimationController.AnimationLabel.RELOAD_EMPTY);
        AnimationMeta reloadNormalMeta = controller.getAnimationFromLabel(GunAnimationController.AnimationLabel.RELOAD_NORMAL);
        if (Gun.hasAmmo(itemStack)) {
            if (!controller.getPreviousAnimation().equals(reloadNormalMeta))
                controller.stopAnimation();
            controller.runAnimation(GunAnimationController.AnimationLabel.RELOAD_NORMAL);
        } else {
            if (!controller.getPreviousAnimation().equals(reloadEmptyMeta))
                controller.stopAnimation();
            controller.runAnimation(GunAnimationController.AnimationLabel.RELOAD_EMPTY);
        }
    }

    @SubscribeEvent
    public void onGunFire(GunFireEvent.Pre event) {
        if (!event.isClient()) return;
        if(Minecraft.getInstance().player == null) return;
        if(!event.getPlayer().getUniqueID().equals(Minecraft.getInstance().player.getUniqueID())) return;
        GunAnimationController controller = GunAnimationController.fromItem(event.getStack().getItem());
        if (controller == null) return;
        if (controller.isAnimationRunning()) {
            AnimationMeta meta = controller.getPreviousAnimation();
            if(meta == null) return;
            if (meta.equals(controller.getAnimationFromLabel(GunAnimationController.AnimationLabel.INSPECT)))
                controller.stopAnimation();
            else {
                AnimationRunner runner = Animations.getAnimationRunner(meta.getResourceLocation());
                if(runner == null) return;
                float current = runner.getAnimationManager().getCurrentTimeS();
                float max = runner.getAnimationManager().getMaxEndTimeS();
                if(max - current <= 0.25f) return;
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onPumpShotgunFire(GunFireEvent.Post event) {
        if (!event.isClient()) return;
        if(Minecraft.getInstance().player == null) return;
        if(!event.getPlayer().getUniqueID().equals(Minecraft.getInstance().player.getUniqueID())) return;
        GunAnimationController controller = GunAnimationController.fromItem(event.getStack().getItem());
        if (controller instanceof PumpShotgunAnimationController) {
            controller.runAnimation(GunAnimationController.AnimationLabel.PUMP);
        }
    }

    @SubscribeEvent
    public void onBoltActionRifleFire(GunFireEvent.Post event){
        if (!event.isClient()) return;
        if(Minecraft.getInstance().player == null) return;
        if(!event.getPlayer().getUniqueID().equals(Minecraft.getInstance().player.getUniqueID())) return;
        GunAnimationController controller = GunAnimationController.fromItem(event.getStack().getItem());
        if (controller instanceof BoltActionAnimationController) {
            controller.runAnimation(GunAnimationController.AnimationLabel.PULL_BOLT);
        }
    }

    @SubscribeEvent
    public void onInspect(InputEvent.KeyInputEvent event) {
        PlayerEntity player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack itemStack = player.inventory.getCurrentItem();
        if (KeyBinds.KEY_INSPECT.isKeyDown() && event.getAction() == GLFW.GLFW_PRESS) {
            GunAnimationController controller = GunAnimationController.fromItem(itemStack.getItem());
            if (controller != null) {
                if(controller.isAnimationRunning()) return;
                controller.stopAnimation();
                controller.runAnimation(GunAnimationController.AnimationLabel.INSPECT);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event){
        AnimationSoundManager.INSTANCE.onPlayerDeath(event.getPlayer());
    }

    @SubscribeEvent
    public void onClientPlayerReload(GunReloadEvent.Pre event){
        if(event.isClient()){
            GunAnimationController controller = GunAnimationController.fromItem(event.getStack().getItem());
            if(controller != null){
                if(controller.isAnimationRunning(GunAnimationController.AnimationLabel.DRAW) ||
                        controller.isAnimationRunning(GunAnimationController.AnimationLabel.PUMP))
                    event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event){
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if(player == null) return;
        ItemStack itemStack = player.inventory.getCurrentItem();
        GunAnimationController controller = GunAnimationController.fromItem(itemStack.getItem());
        if(controller == null) return;
        if(controller.isAnimationRunning()){

        }
    }

    public boolean isReloadingIntro(Item item){
        GunAnimationController controller = GunAnimationController.fromItem(item);
        if(controller == null) return false;
        return controller.isAnimationRunning(GunAnimationController.AnimationLabel.RELOAD_INTRO);
    }

    public void onReloadLoop(Item item){
        GunAnimationController controller = GunAnimationController.fromItem(item);
        if(controller == null) return;
        controller.stopAnimation();
        controller.runAnimation(GunAnimationController.AnimationLabel.RELOAD_LOOP);
    }

    public void onReloadEnd(Item item){
        GunAnimationController controller = GunAnimationController.fromItem(item);
        if(controller == null) return;
        if(controller instanceof PumpShotgunAnimationController ) {
            if(controller.getAnimationFromLabel(GunAnimationController.AnimationLabel.RELOAD_NORMAL_END) != null) {
                controller.stopAnimation();
                controller.runAnimation(GunAnimationController.AnimationLabel.RELOAD_NORMAL_END);
            }
        }else{
            controller.stopAnimation();
            controller.runAnimation(GunAnimationController.AnimationLabel.STATIC);
            controller.stopAnimation();
        }
    }
}