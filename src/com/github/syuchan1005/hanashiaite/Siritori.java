package com.github.syuchan1005.hanashiaite;

import com.google.gson.Gson;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.atilika.kuromoji.Token;
import org.atilika.kuromoji.Tokenizer;

public class Siritori {
	public static final String NOT_MATCH = "**前の解答と合致しません. もういちどお答えください. (前の回答は「%s」, 最後の文字は「%s」です.)**";
	public static final String USED_WORD = "**すでに使われたワードです.もういちどお答えください. (解答は%d回目に%sさんが使いました.)**";
	public static final String WIN = "**最後に「ん」がついたのであなたの負けです.**";
	public static final String LOSE = "**負けてしまいました... 強いですね. つぎはがんばります.**";
	public static final String CLEAN_HISTORY = "**経歴を削除します...**";

	private static Gson gson = new Gson();
	private static Tokenizer tokenizer = Tokenizer.builder().build();
	private static Pattern hiraganaPattern = Pattern.compile("^\\p{InHIRAGANA}+$");
	private static HttpClient httpClient = HttpClientBuilder.create()
			.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build()).build();

	private String gooAppId;
	private Connection connection;
	private PreparedStatement historyInsertStatement;
	private PreparedStatement historySelectStatement;
	private PreparedStatement lastHistoryStatement;
	/*private PreparedStatement offSetSelectStatement;
	private PreparedStatement offSetInsertStatement;
	private PreparedStatement offSetIncrementStatement;*/
	private Map<Character, Integer> offset = new HashMap<>();

	public Siritori(String gooAppId) throws SQLException {
		this.gooAppId = gooAppId;
		init();
	}

	public void init() throws SQLException {
		if (connection != null) connection.close();
		connection = DriverManager.getConnection("jdbc:sqlite::memory:");
		Statement statement = connection.createStatement();
		statement.executeUpdate("CREATE TABLE History(id INTEGER PRIMARY KEY, sender TEXT NOT NULL, word TEXT NOT NULL UNIQUE, phonetic TEXT NOT NULL)");
		historyInsertStatement = connection.prepareStatement("INSERT INTO History(sender, word, phonetic) VALUES (?, ?, ?)");
		historySelectStatement = connection.prepareStatement("SELECT * FROM History WHERE word LIKE ?");
		lastHistoryStatement = connection.prepareStatement("SELECT * FROM History ORDER BY id DESC LIMIT 1");
	}

	public boolean isFollowed(String word) throws SQLException {
		String lastWordPhonetic = getLastWordPhonetic();
		return lastWordPhonetic.isEmpty() || toHiragana(word).startsWith(toHiragana(getLastChar(lastWordPhonetic)));
	}

	public boolean isFinished(String word) {
		return getLastChar(word).equals("ん");
	}

	public HistoryData getHistory(String word) throws SQLException {
		historySelectStatement.setString(1, word);
		ResultSet resultSet = historySelectStatement.executeQuery();
		if (resultSet.next()) {
			return new HistoryData(resultSet.getInt(1),
					resultSet.getString(2),
					resultSet.getString(3),
					resultSet.getString(4));
		} else {
			return null;
		}
	}

	public void insertHistory(String sender, String word) throws SQLException {
		historyInsertStatement.setString(1, sender);
		historyInsertStatement.setString(2, word);
		historyInsertStatement.setString(3, toHiragana(word));
		historyInsertStatement.executeUpdate();
	}

	public List<String> getReturnWords(String word) throws SQLException {
		String last = getLastChar(word);
		String url = "https://ja.wikipedia.org/w/api.php?format=json&action=query&list=search&srprop=sectiontitle" +
				"&srsearch=prefix:" + last +
				"&sroffset=" + getOffset(last);
		HttpGet httpGet = new HttpGet(url);
		ArrayList<String> list = new ArrayList<>();
		try {
			HttpResponse response = httpClient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() == 200) {
				String body = EntityUtils.toString(response.getEntity());
				MediaWikiData mediaWikiData = gson.fromJson(body, MediaWikiData.class);
				mediaWikiData.query.search.forEach((v) -> list.add(v.title));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}

	public HistoryData getLastHistory() throws SQLException {
		ResultSet resultSet = lastHistoryStatement.executeQuery();
		while (resultSet.next()) {
			return new HistoryData(resultSet.getInt(1),
					resultSet.getString(2),
					resultSet.getString(3),
					resultSet.getString(4));
		}
		return null;
	}

	public String getLastWordPhonetic() throws SQLException {
		HistoryData lastHistory = getLastHistory();
		if (lastHistory == null) return "";
		return lastHistory.getPhonetic();
	}

	public String toHiragana(String word) {
		StringBuilder sb = new StringBuilder();
		for (Token token : tokenizer.tokenize(word)) {
			if (token.getSurfaceForm().contains("ー")) {
			}
			if (token.getReading() != null) {
				sb.append(token.getReading());
			} else {
				sb.append(token.getSurfaceForm());
			}
		}
		return katakana2Hiragana(sb.toString());
	}

	public String getLastChar(String word) {
		if (!isHiragana(word)) word = toHiragana(word);
		for (int i = word.length(); i >= 1; i--) {
			String s = word.substring(i - 1, i);
			if (isHiragana(s)) return normalizeJP(s);
		}
		return "";
	}

	private boolean isHiragana(String word) {
		return hiraganaPattern.matcher(word).find();
	}

	private static final String lower = "ぁぃぅぇぉっゃゅょゎァィゥェォヵヶッャュョヮゑゐ";
	private static final String upper = "あいうえおつやゆよわアイウエオカケツヤユヨワえい";

	private static String normalizeJP(String word) {
		StringBuilder sb = new StringBuilder();
		for (char c : word.toCharArray()) {
			int i = lower.indexOf((int) c);
			if (i == -1) {
				sb.append(c);
			} else {
				sb.append(upper.charAt(i));
			}
		}
		return sb.toString();
	}

	public int getOffset(String last) throws SQLException {
		char c = last.toCharArray()[0];
		if (offset.containsKey(c)) {
			return offset.get(c);
		} else {
			offset.put(c, 0);
		}
		return 0;
	}

	public void incrementOffset(String last) throws SQLException {
		char c = last.toCharArray()[0];
		if (offset.containsKey(c)) {
			offset.put(c, offset.get(c) + 1);
		} else {
			offset.put(c, 1);
		}
	}

	public String katakana2Hiragana(String str) {
		StringBuilder sb = new StringBuilder();
		for (char c : str.toCharArray()) {
			if (('ァ' <= c) && (c <= 'ヶ')) {
				sb.append((char) (c - 0x60));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public class HistoryData {
		private int id;
		private String sender;
		private String word;
		private String phonetic;

		public HistoryData(int id, String sender, String word, String phonetic) {
			this.id = id;
			this.sender = sender;
			this.word = word;
			this.phonetic = phonetic;
		}

		public int getId() {
			return id;
		}

		public String getSender() {
			return sender;
		}

		public String getWord() {
			return word;
		}

		public String getPhonetic() {
			return phonetic;
		}

		@Override
		public String toString() {
			return "HistoryData{" +
					"id=" + id +
					", sender='" + sender + '\'' +
					", word='" + word + '\'' +
					", phonetic='" + phonetic + '\'' +
					'}';
		}
	}

	private class MediaWikiData {
		MediaWikiQuery query;

		class MediaWikiQuery {
			List<MediaWikiSearchData> search;

			class MediaWikiSearchData {
				String title;
			}
		}
	}

	private class GooLabData {
		String converted;
	}
}
