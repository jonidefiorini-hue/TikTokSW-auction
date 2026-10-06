package de.auctionhud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuctionState {
	public static final AuctionState INSTANCE = new AuctionState();

	public enum Phase { IDLE, RUNNING, ENDED }
	public record Bid(String player, double amount) {}

	public Phase phase = Phase.IDLE;
	public ItemStack item = ItemStack.EMPTY;
	public double minBid;
	public long endTime;
	public final List<Bid> bids = new ArrayList<>();
	private final Map<String, Double> totals = new HashMap<>();

	public void start(ItemStack stack, double min, long durationMs) {
		reset();
		item = stack;
		minBid = min;
		endTime = System.currentTimeMillis() + durationMs;
		phase = Phase.RUNNING;
	}

	public void reset() {
		phase = Phase.IDLE;
		item = ItemStack.EMPTY;
		bids.clear();
		totals.clear();
	}

	public Bid top() {
		return bids.isEmpty() ? null : bids.get(bids.size() - 1);
	}

	public long remainingMs() {
		return Math.max(0, endTime - System.currentTimeMillis());
	}

	public void tick() {
		if (phase == Phase.RUNNING && System.currentTimeMillis() >= endTime) {
			phase = Phase.ENDED;
			play(true);
		}
	}

	public void onPayment(String player, double amount) {
		if (phase != Phase.RUNNING) return;
		double value = AuctionConfig.INSTANCE.cumulativeBids
				? totals.merge(player, amount, Double::sum) : amount;
		if (value < minBid) return;
		Bid t = top();
		if (t != null && value <= t.amount()) return;
		bids.add(new Bid(player, value));
		play(false);
	}

	private void play(boolean end) {
		MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(
				end ? SoundEvents.ENTITY_PLAYER_LEVELUP : SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
	}

	// ---------- Hilfsfunktionen ----------

	public static String money(double v) {
		if (v == Math.floor(v)) return "$" + String.format(Locale.GERMANY, "%,d", (long) v);
		return "$" + String.format(Locale.GERMANY, "%,.2f", v);
	}

	public static String formatTime(long ms) {
		long s = (ms + 999) / 1000;
		long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
		return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%02d:%02d", m, sec);
	}

	/** "1000", "1.000", "1,5k", "2M" ... -> Zahl, -1 bei Fehler */
	public static double parseAmount(String raw) {
		try {
			String s = raw.trim().toLowerCase(Locale.ROOT).replace("$", "").replace(" ", "");
			double mult = 1;
			if (s.endsWith("k")) { mult = 1e3; s = s.substring(0, s.length() - 1); }
			else if (s.endsWith("m")) { mult = 1e6; s = s.substring(0, s.length() - 1); }
			else if (s.endsWith("b")) { mult = 1e9; s = s.substring(0, s.length() - 1); }

			int last = Math.max(s.lastIndexOf('.'), s.lastIndexOf(','));
			String num = s;
			if (last >= 0) {
				char sep = s.charAt(last);
				int count = 0;
				for (char c : s.toCharArray()) if (c == '.' || c == ',') count++;
				boolean otherSep = s.indexOf(sep == '.' ? ',' : '.') >= 0;
				int after = s.length() - last - 1;
				boolean decimal = otherSep || (count == 1 && after != 3);
				if (decimal) {
					String intPart = s.substring(0, last).replaceAll("[.,]", "");
					String frac = s.substring(last + 1);
					num = (intPart.isEmpty() ? "0" : intPart) + (frac.isEmpty() ? "" : "." + frac);
				} else {
					num = s.replaceAll("[.,]", "");
				}
			}
			return Double.parseDouble(num) * mult;
		} catch (Exception e) {
			return -1;
		}
	}

	/** "30s", "5m", "1h30m"; reine Zahl = Minuten. -1 bei Fehler. */
	public static long parseDuration(String in) {
		String s = in.trim().toLowerCase(Locale.ROOT);
		if (s.matches("\\d+")) return Long.parseLong(s) * 60_000L;
		Matcher m = Pattern.compile("(\\d+)\\s*([hms])").matcher(s);
		long total = 0;
		boolean found = false;
		while (m.find()) {
			found = true;
			long v = Long.parseLong(m.group(1));
			switch (m.group(2)) {
				case "h" -> total += v * 3_600_000L;
				case "m" -> total += v * 60_000L;
				default -> total += v * 1_000L;
			}
		}
		return found ? total : -1;
	}
}
