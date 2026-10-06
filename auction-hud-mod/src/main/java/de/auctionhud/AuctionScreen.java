package de.auctionhud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class AuctionScreen extends Screen {
	private static final int SLOT = 18;
	private static String lastMin = "1000";
	private static String lastDuration = "5m";

	private int gridX, gridY;
	private int selectedSlot = -1;
	private TextFieldWidget minBidField, durationField;
	private String error = "";

	public AuctionScreen() {
		super(Text.literal("Auktion"));
	}

	@Override
	protected void init() {
		int cx = width / 2;
		AuctionState s = AuctionState.INSTANCE;

		if (s.phase != AuctionState.Phase.IDLE) {
			String label = s.phase == AuctionState.Phase.RUNNING ? "Auktion abbrechen" : "Zurücksetzen";
			addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
				s.reset();
				close();
			}).dimensions(cx - 75, height / 2 + 20, 150, 20).build());
			return;
		}

		gridX = cx - 81;
		gridY = height / 2 - 85;

		minBidField = new TextFieldWidget(textRenderer, gridX, gridY + 94, 78, 18, Text.literal("Mindestgebot"));
		minBidField.setMaxLength(16);
		minBidField.setText(lastMin);
		addDrawableChild(minBidField);

		durationField = new TextFieldWidget(textRenderer, gridX + 84, gridY + 94, 78, 18, Text.literal("Dauer"));
		durationField.setMaxLength(12);
		durationField.setText(lastDuration);
		addDrawableChild(durationField);

		addDrawableChild(ButtonWidget.builder(Text.literal("Auktion starten"), b -> start())
				.dimensions(gridX, gridY + 120, 162, 20).build());
	}

	private void start() {
		if (client == null || client.player == null) return;
		if (selectedSlot < 0 || client.player.getInventory().getStack(selectedSlot).isEmpty()) {
			error = "Wähle zuerst ein Item aus.";
			return;
		}
		double min = AuctionState.parseAmount(minBidField.getText());
		if (min < 0) { error = "Ungültiges Mindestgebot."; return; }
		long dur = AuctionState.parseDuration(durationField.getText());
		if (dur <= 0) { error = "Ungültige Dauer (z.B. 30s, 5m, 1h30m)."; return; }

		lastMin = minBidField.getText();
		lastDuration = durationField.getText();
		AuctionState.INSTANCE.start(client.player.getInventory().getStack(selectedSlot).copy(), min, dur);
		close();
	}

	/** Inventar-Slot-Indizes in Anzeigereihenfolge: Hauptinventar (9-35), dann Hotbar (0-8). */
	private List<Integer> slotOrder() {
		List<Integer> l = new ArrayList<>();
		for (int i = 9; i < 36; i++) l.add(i);
		for (int i = 0; i < 9; i++) l.add(i);
		return l;
	}

	private int slotX(int inv) {
		int col = inv >= 9 ? (inv - 9) % 9 : inv;
		return gridX + col * SLOT;
	}

	private int slotY(int inv) {
		if (inv >= 9) return gridY + ((inv - 9) / 9) * SLOT;
		return gridY + 3 * SLOT + 4;
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		int cx = width / 2;
		AuctionState s = AuctionState.INSTANCE;

		if (s.phase != AuctionState.Phase.IDLE) {
			ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Auktion läuft"), cx, height / 2 - 30, 0xFFFFAA00);
			ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(s.item.getName().getString()), cx, height / 2 - 10, 0xFFFFFFFF);
			return;
		}

		ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Item für die Auktion wählen"), cx, gridY - 14, 0xFFFFFFFF);

		ItemStack hovered = ItemStack.EMPTY;
		if (client != null && client.player != null) {
			for (int inv : slotOrder()) {
				int x = slotX(inv), y = slotY(inv);
				ItemStack st = client.player.getInventory().getStack(inv);
				ctx.fill(x, y, x + SLOT, y + SLOT, 0xFF555555);
				ctx.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xFF8B8B8B);
				if (!st.isEmpty()) {
					ctx.drawItem(st, x + 1, y + 1);
					ctx.drawStackOverlay(textRenderer, st, x + 1, y + 1);
				}
				boolean over = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
				if (over) {
					ctx.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x80FFFFFF);
					hovered = st;
				}
				if (inv == selectedSlot) ctx.drawBorder(x, y, SLOT, SLOT, 0xFFFFAA00);
			}
		}

		ctx.drawText(textRenderer, "Mindestgebot", gridX, gridY + 83, 0xFFAAAAAA, true);
		ctx.drawText(textRenderer, "Dauer (30s/5m/1h)", gridX + 84, gridY + 83, 0xFFAAAAAA, true);
		if (!error.isEmpty()) {
			ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(error), cx, gridY + 146, 0xFFFF5555);
		}
		if (!hovered.isEmpty()) ctx.drawItemTooltip(textRenderer, hovered, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (AuctionState.INSTANCE.phase == AuctionState.Phase.IDLE && client != null && client.player != null) {
			for (int inv : slotOrder()) {
				int x = slotX(inv), y = slotY(inv);
				if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
					if (!client.player.getInventory().getStack(inv).isEmpty()) {
						selectedSlot = inv;
						error = "";
					}
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
