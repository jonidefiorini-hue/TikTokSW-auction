package de.auctionhud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

import static de.auctionhud.AuctionState.money;

public class AuctionHud {
	private record Line(String text, int color) {}

	public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		AuctionState s = AuctionState.INSTANCE;
		if (s.phase == AuctionState.Phase.IDLE || mc.options.hudHidden) return;
		TextRenderer tr = mc.textRenderer;

		List<Line> lines = new ArrayList<>();
		String name = s.item.getName().getString();
		if (s.item.getCount() > 1) name += " x" + s.item.getCount();
		lines.add(new Line(name, 0xFFFFAA00));
		lines.add(new Line("Mindestgebot: " + money(s.minBid), 0xFFAAAAAA));

		AuctionState.Bid top = s.top();
		lines.add(top == null
				? new Line("Noch kein Gebot", 0xFFFF5555)
				: new Line("Höchstgebot: " + money(top.amount()) + " (" + top.player() + ")", 0xFF55FF55));

		if (s.phase == AuctionState.Phase.RUNNING) {
			long rem = s.remainingMs();
			lines.add(new Line("Verbleibend: " + AuctionState.formatTime(rem), rem < 10_000 ? 0xFFFF5555 : 0xFFFFFFFF));
		} else {
			lines.add(new Line(top == null ? "Beendet - kein Gebot"
					: "GEWINNER: " + top.player() + " (" + money(top.amount()) + ")", 0xFFFFFF55));
		}

		for (int i = s.bids.size() - 2, n = 0; i >= 0 && n < 3; i--, n++) {
			AuctionState.Bid b = s.bids.get(i);
			lines.add(new Line("  " + b.player() + ": " + money(b.amount()), 0xFF999999));
		}

		int maxW = 0;
		for (Line l : lines) maxW = Math.max(maxW, tr.getWidth(l.text()));
		int x = AuctionConfig.INSTANCE.hudX, y = AuctionConfig.INSTANCE.hudY;
		int w = 26 + maxW + 6, h = lines.size() * 10 + 8;

		ctx.fill(x, y, x + w, y + h, 0xAA000000);
		ctx.drawBorder(x, y, w, h, 0xFFFFAA00);
		ctx.drawItem(s.item, x + 5, y + 5);
		for (int i = 0; i < lines.size(); i++) {
			ctx.drawText(tr, lines.get(i).text(), x + 26, y + 5 + i * 10, lines.get(i).color(), true);
		}
	}
}
