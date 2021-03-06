package com.github.syuchan1005.hanashiaite;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import jp.ne.docomo.smt.dev.common.exception.SdkException;
import jp.ne.docomo.smt.dev.common.exception.ServerException;
import jp.ne.docomo.smt.dev.common.http.AuthApiKey;
import jp.ne.docomo.smt.dev.dialogue.Dialogue;
import jp.ne.docomo.smt.dev.dialogue.data.DialogueResultData;
import jp.ne.docomo.smt.dev.dialogue.param.DialogueRequestParam;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.api.internal.DiscordVoiceWS;
import sx.blah.discord.api.internal.ShardImpl;
import sx.blah.discord.api.internal.UDPVoiceSocket;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.IVoiceState;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.cache.Cache;
import sx.blah.discord.util.cache.LongMap;

/**
 * Created by syuchan on 2016/10/08.
 */
public class Hanashiaite {
	private static String DocomoAPIKey;
	private static String DiscordToken;
	private static long VoiceChannelID;
	private static long NotifyChannelID;
	private static long SiritoriChannelID;
	private static final File configFile = new File("config.properties");
	private static Properties config = new Properties();

	private static Gson gson = new Gson();
	private static Map<String, DialogueResultData> contexts;
	private static IDiscordClient discordClient;
	private static Field voiceSocketField;
	private static Field eventField;

	public static void main(String[] args) {
		loadConfig();
		DocomoAPIKey = config.getProperty("DocomoAPIKey");
		DiscordToken = config.getProperty("DiscordToken");
		VoiceChannelID = Long.parseLong(config.getProperty("VoiceChannelID"));
		NotifyChannelID = Long.parseLong(config.getProperty("NotifyChannelID"));
		SiritoriChannelID = Long.parseLong(config.getProperty("SiritoriChannelID"));
		BukkitWatcher.setCommitModels(
				config.getProperty("Bukkit", ""),
				config.getProperty("CraftBukkit", ""),
				config.getProperty("Spigot", ""));

		try {
			voiceSocketField = DiscordVoiceWS.class.getDeclaredField("voiceSocket");
			voiceSocketField.setAccessible(true);
			eventField = UDPVoiceSocket.class.getDeclaredField("address");
			eventField.setAccessible(true);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
		if (contexts == null) contexts = new HashMap<>();
		AuthApiKey.initializeAuth(DocomoAPIKey);
		try {
			discordClient = new ClientBuilder().withToken(DiscordToken).login();
			EventDispatcher dispatcher = discordClient.getDispatcher();
			if (config.getProperty("Chat").equalsIgnoreCase("true")) {
				dispatcher.registerListener(new IListener<MessageReceivedEvent>() {
					@Override
					public void handle(MessageReceivedEvent event) {
						IMessage message = event.getMessage();
						String name = message.getChannel().getName();
						if (name.equals("hanasiaite") || name.equals("hanashiaite")) {
							if (message.getContent().equals("$restart")) {
								try {
									DialogueRequestParam dialogueRequestParam = new DialogueRequestParam();
									dialogueRequestParam.setUtt("-restart");
									contexts.put(name, new Dialogue().request(dialogueRequestParam));
								} catch (SdkException | ServerException e) {
									e.printStackTrace();
								}
								return;
							}
							DialogueRequestParam param = new DialogueRequestParam();
							param.setUtt(message.getContent());
							if (contexts.containsKey(name)) {
								param.setContext(contexts.get(name).getContext());
								param.setMode(contexts.get(name).getMode());
							}
							Dialogue dialogue = new Dialogue();
							try {
								DialogueResultData resultData = dialogue.request(param);
								message.getChannel().sendMessage(resultData.getUtt());
								contexts.put(name, resultData);
							} catch (SdkException | ServerException | DiscordException | RateLimitException | MissingPermissionsException e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
			if (config.getProperty("Notification").equalsIgnoreCase("true")) {
				dispatcher.registerListener(new IListener<UserVoiceChannelJoinEvent>() {
					@Override
					public void handle(UserVoiceChannelJoinEvent event) {
						try {
							IChannel voiceTextChannel = discordClient.getChannelByID(VoiceChannelID);
							IMessage message = voiceTextChannel.sendMessage("$" + event.getUser().getDisplayName(voiceTextChannel.getGuild()) + "さんが" +
									event.getVoiceChannel().getName() + "に入室しました");
							addDeleteTask(message);
						} catch (MissingPermissionsException | RateLimitException | DiscordException e) {
							e.printStackTrace();
						}
					}
				});
				dispatcher.registerListener(new IListener<UserVoiceChannelLeaveEvent>() {
					@Override
					public void handle(UserVoiceChannelLeaveEvent event) {
						try {
							IChannel voiceTextChannel = discordClient.getChannelByID(VoiceChannelID);
							IMessage message = voiceTextChannel.sendMessage("$" +
									event.getUser().getDisplayName(voiceTextChannel.getGuild()) + "さんが" +
									event.getVoiceChannel().getName() + "を退室しました");
							addDeleteTask(message);
						} catch (MissingPermissionsException | RateLimitException | DiscordException e) {
							e.printStackTrace();
						}
					}
				});
			}
			if (config.getProperty("Minecraft").equalsIgnoreCase("true")) {
				Timer timer = new Timer();
				timer.schedule(new BukkitWatcher(), 0, TimeUnit.MINUTES.toMillis(30));
			}
			if (config.getProperty("AskIP").equalsIgnoreCase("true")) {
				dispatcher.registerListener(new IListener<MessageReceivedEvent>() {
					@Override
					public void handle(MessageReceivedEvent event) {
						try {
							IMessage message = event.getMessage();
							if (message.getContent().equals("-ip")) {
								LongMap<IVoiceState> connectedVoiceChannels = message.getAuthor().getVoiceStatesLong();
								if (connectedVoiceChannels.size() < 1) {
									IMessage iMessage = message.getChannel().sendMessage("$" + "このコマンドはVCに接続してから実行して下さい");
									addDeleteTask(iMessage);
									return;
								}
								IVoiceChannel textChannel = connectedVoiceChannels.get(0).getChannel();
								ShardImpl shard = (ShardImpl) textChannel.getShard();
								Timer leaveTaskTimer = new Timer("VSLeaveTask");
								leaveTaskTimer.schedule(new TimerTask() {
									@Override
									public void run() {
										Cache<DiscordVoiceWS> voiceWebSockets = shard.voiceWebSockets;
										for (DiscordVoiceWS voiceWS : voiceWebSockets) {
											InetSocketAddress inetAddress = null;
											try {
												inetAddress = (InetSocketAddress) eventField.get(voiceSocketField.get(voiceWS));
												byte[] address = inetAddress.getAddress().getAddress();
												String ip = "";
												for (int i = 0; i < 4; ++i) {
													int t = 0xFF & address[i];
													ip += "." + t;
												}
												ip = ip.substring(1);
												try {
													IMessage iMessage = message.getChannel().sendMessage("$" + "```Channel: " + textChannel.getName() +
															"\nEndPoint: " + inetAddress.getHostString() +
															"\nIP: " + ip + "```");
													addDeleteTask(iMessage);
												} catch (MissingPermissionsException | RateLimitException | DiscordException e) {
													e.printStackTrace();
												}
											} catch (IllegalAccessException e) {
												e.printStackTrace();
											}
										}
										leaveTaskTimer.schedule(new TimerTask() {
											@Override
											public void run() {
												textChannel.leave();
												leaveTaskTimer.cancel();
											}
										}, TimeUnit.SECONDS.toMillis(2));
									}
								}, TimeUnit.SECONDS.toMillis(1));
							}
						} catch (MissingPermissionsException | RateLimitException | DiscordException e) {
							e.printStackTrace();
						}
					}
				});
			}
			if (config.getProperty("ShowMem").equalsIgnoreCase("true")) {
				Runtime runtime = Runtime.getRuntime();
				Timer memRefreshTimer = new Timer();
				memRefreshTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						long usage = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
						discordClient.changePlayingText("Usage: " + usage + "MB");
					}
				}, TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(1));
			}
			if (config.getProperty("Siritori").equalsIgnoreCase("true")) {
				try {
					Siritori siritori = new Siritori(config.getProperty("GooLabsAppID"));
					dispatcher.registerListener(new IListener<MessageReceivedEvent>() {
						@Override
						public void handle(MessageReceivedEvent event) {
							if (event.getChannel().getLongID() != SiritoriChannelID) return;
							try {
								String word = event.getMessage().getContent();
								if (!siritori.isFollowed(word)) {
									String lastWordPhonetic = siritori.getLastWordPhonetic();
									event.getChannel().sendMessage(String.format(Siritori.NOT_MATCH, lastWordPhonetic, siritori.getLastChar(lastWordPhonetic)));
									return;
								}
								Siritori.HistoryData history = siritori.getHistory(word);
								if (history != null) {
									event.getChannel().sendMessage(String.format(Siritori.USED_WORD, history.getId() + 1, history.getSender()));
									return;
								}
								if (siritori.isFinished(word)) {
									event.getChannel().sendMessage(Siritori.WIN);
									event.getChannel().sendMessage(Siritori.CLEAN_HISTORY);
									Siritori.HistoryData lastHistory = siritori.getLastHistory();
									event.getChannel().sendMessage(String.format("**今回の対決は%d回, わたしの勝ちでした.**", lastHistory.getId() + 1, lastHistory.getSender()));
									try {
										siritori.init();
									} catch (SQLException ignored) {
									}
									return;
								}
								siritori.insertHistory(event.getAuthor().getName(), word);
								List<String> returnWords = siritori.getReturnWords(word);
								String last = siritori.getLastChar(word);
								for (String returnWord : returnWords) {
									siritori.incrementOffset(last);
									if (siritori.getHistory(returnWord) == null && !siritori.isFinished(returnWord)) {
										event.getChannel().sendMessage(returnWord);
										siritori.insertHistory("Hanashiaite", returnWord);
										return;
									}
								}
								event.getChannel().sendMessage(Siritori.LOSE);
								event.getChannel().sendMessage(Siritori.CLEAN_HISTORY);
								Siritori.HistoryData lastHistory = siritori.getLastHistory();
								event.getChannel().sendMessage(String.format("**今回の対決は%d回, %sさんの勝ちでした.**", lastHistory.getId() + 1, lastHistory.getSender()));
								siritori.init();
							} catch (SQLException ignored) {
								ignored.printStackTrace();
							}
						}
					});
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} catch (DiscordException e) {
			e.printStackTrace();
		}
	}

	private static void loadConfig() {
		try (InputStream is = new FileInputStream(configFile);
			 InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			 BufferedReader reader = new BufferedReader(isr)) {
			config.load(reader);
		} catch (Exception ignored) {
		}
	}

	public static Properties getConfig() {
		return config;
	}

	public static void setConfig(CommitModel model) {
		config.setProperty(model.getCommitType(), model.getDisplayId());
		try {
			configFile.createNewFile();
			OutputStream outputStream = new FileOutputStream(configFile);
			config.store(outputStream, "");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static IChannel notifyChannel;

	public static void sendBukkitMessage(EmbedObject message) {
		try {
			while (notifyChannel == null) {
				notifyChannel = discordClient.getChannelByID(NotifyChannelID);
			}
			if (notifyChannel.getShard().isReady()) {
				notifyChannel.sendMessage("", message, false);
			} else {
				new Thread(() -> {
					while (!notifyChannel.getShard().isReady()) {
					}
					notifyChannel.sendMessage("", message, false);
				}).start();
			}
		} catch (MissingPermissionsException | RateLimitException | DiscordException e) {
			e.printStackTrace();
		}
	}

	private static void addDeleteTask(IMessage message) {
		Timer deleteTaskTimer = new Timer("MessageDeleteTask");
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				try {
					message.delete();
				} catch (MissingPermissionsException | RateLimitException | DiscordException e) {
					e.printStackTrace();
				}
				deleteTaskTimer.cancel();
			}
		};
		deleteTaskTimer.schedule(task, TimeUnit.SECONDS.toMillis(30));
	}

}
