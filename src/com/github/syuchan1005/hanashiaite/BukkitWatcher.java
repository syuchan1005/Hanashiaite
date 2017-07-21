package com.github.syuchan1005.hanashiaite;

import com.google.gson.Gson;
import java.awt.Color;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.TimerTask;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

/**
 * Created by syuchan on 2017/01/11.
 */
public class BukkitWatcher extends TimerTask {
	private static String BaseURL = "https://hub.spigotmc.org/stash/projects/SPIGOT/repos";
	private static String BukkitCommit = BaseURL + "/bukkit/commits";
	private static String CraftBukkitCommit = BaseURL + "/craftbukkit/commits";
	private static String SpigotCommit = BaseURL + "/spigot/commits";
	private static Gson gson = new Gson();
	private static CommitModel bukkitLatest = new CommitModel();
	private static CommitModel craftBukkitLatest = new CommitModel();
	private static CommitModel spigotLatest = new CommitModel();

	@Override
	public void run() {
		try {
			// Bukkit
			CommitModel latestCommitData = getLatestCommitData(Jsoup.connect(BukkitCommit).get());
			if (latestCommitData != null && !bukkitLatest.getDisplayId().equals(latestCommitData.getDisplayId())) {
				bukkitLatest = latestCommitData;
				bukkitLatest.setCommitType("Bukkit");
				sendCommitModel(bukkitLatest);
			}
			// CraftBukkit
			latestCommitData = getLatestCommitData(Jsoup.connect(CraftBukkitCommit).get());
			if (latestCommitData != null && !craftBukkitLatest.getDisplayId().equals(latestCommitData.getDisplayId())) {
				craftBukkitLatest = latestCommitData;
				craftBukkitLatest.setCommitType("CraftBukkit");
				sendCommitModel(craftBukkitLatest);
			}
			// Spigot
			latestCommitData = getLatestCommitData(Jsoup.connect(SpigotCommit).get());
			if (latestCommitData != null && !spigotLatest.getDisplayId().equals(latestCommitData.getDisplayId())) {
				spigotLatest = latestCommitData;
				spigotLatest.setCommitType("Spigot");
				sendCommitModel(spigotLatest);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static CommitModel getLatestCommitData(Document document) throws IOException {
		Elements elements = document.getElementsByClass("commit-row");
		return (elements.size() >= 1) ? gson.fromJson(elements.get(0).attr("data-commit-json"), CommitModel.class) : null;
	}

	private static void sendCommitModel(CommitModel commitModel) {
		String mainColorHex = "";
		try {
			URL url = new URL(commitModel.getAuthor().getAvatarUrl());
			mainColorHex = ColorUtil.getMainColorHex(url);
		} catch (IOException ignored) {}
		EmbedObject embed = new EmbedBuilder()
				.withColor(Color.decode(mainColorHex))
				.withAuthorName(commitModel.getAuthor().getName())
				.withDescription(commitModel.getMessage())
				.withTitle(commitModel.getCommitType() + "'s new commit: " + commitModel.getDisplayId())
				.withUrl(BaseURL + "/" + commitModel.getCommitType() + "/commits/" + commitModel.getDisplayId())
				.withThumbnail(commitModel.getAuthor().getAvatarUrl())
				.withTimestamp(commitModel.getAuthorTimestamp())
				.build();
		Hanashiaite.sendBukkitMessage(embed);
		Hanashiaite.setConfig(commitModel);
	}

	public static void setCommitModels(String bukkit, String craftBukkit, String spigot) {
		bukkitLatest.setDisplayId(bukkit);
		craftBukkitLatest.setDisplayId(craftBukkit);
		spigotLatest.setDisplayId(spigot);
	}

	public static OffsetDateTime format(String str) {
		String s = str.substring(0, str.length() - 2) + ":" + str.substring(str.length() - 2);
		return OffsetDateTime.parse(s);
	}
}
