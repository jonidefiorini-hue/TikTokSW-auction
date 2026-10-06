package de.auctionhud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Config-Datei: .minecraft/config/auctionhud.json */
public class AuctionConfig {
	public int hudX = 6;
	public int hudY = 6;
	/** true = alle eingehenden Servernachrichten in latest.log schreiben (zum Anpassen der Patterns). */
	public boolean debug = false;
	/** true = mehrere /pay desselben Spielers werden addiert. */
	public boolean cumulativeBids = false;
	/** Regex mit benannten Gruppen (?<player>...) und (?<amount>...). Gegen die Nachricht ohne Farbcodes. */
	public List<String> paymentPatterns = new ArrayList<>(List.of(
			"(?<player>\\w{3,16}) (?:hat dir|has sent you|sent you|has paid you|paid you|zahlte dir|hat dir gezahlt) \\$?(?<amount>[\\d.,]+[kKmMbB]?)",
			"(?:Du hast|You received|You have received) \\$?(?<amount>[\\d.,]+[kKmMbB]?)\\$? (?:von|from) (?<player>\\w{3,16})"
	));

	public static AuctionConfig INSTANCE = new AuctionConfig();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("auctionhud.json");
		try {
			if (Files.exists(file)) {
				AuctionConfig c = GSON.fromJson(Files.readString(file), AuctionConfig.class);
				if (c != null) INSTANCE = c;
			}
			Files.writeString(file, GSON.toJson(INSTANCE));
		} catch (Exception e) {
			AuctionHudClient.LOGGER.error("Config konnte nicht geladen werden", e);
		}
	}
}
