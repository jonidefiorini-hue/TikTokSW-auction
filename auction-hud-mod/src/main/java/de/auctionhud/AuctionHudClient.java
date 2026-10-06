package de.auctionhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionHudClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("auctionhud");
	public static KeyBinding openKey;

	@Override
	public void onInitializeClient() {
		AuctionConfig.load();
		ChatListener.compile();

		openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.auctionhud.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.auctionhud"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			AuctionState.INSTANCE.tick();
			while (openKey.wasPressed()) {
				if (client.currentScreen == null && client.player != null) {
					client.setScreen(new AuctionScreen());
				}
			}
		});

		HudRenderCallback.EVENT.register(AuctionHud::render);

		// Server-/Plugin-Nachrichten (z.B. "X hat dir $500 gesendet") kommen als Game-Messages an.
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) ChatListener.handle(message.getString());
		});
	}
}
