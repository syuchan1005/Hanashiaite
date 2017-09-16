package com.github.syuchan1005.hanashiaite;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

class SiritoriTest {
	private static Siritori siritori;

	@BeforeAll
	public static void setUp() {
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream("config.properties"));
			siritori = new Siritori(properties.getProperty("GooLabsAppID"));
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
	}

	@AfterEach
	public void after() throws SQLException {
		siritori.init();
	}

	@Test
	public void testToHiragana() {
		assertEquals("てすと", siritori.toHiragana("テスト"));
	}

	@Test
	public void testGetLastChar() {
		assertEquals("と", siritori.getLastChar("てすと"));
		assertEquals("と", siritori.getLastChar("てすと?"));
	}

	@Test
	public void testInsertHistory() throws SQLException {
		siritori.insertHistory("test", "テスト");
		Siritori.HistoryData history = siritori.getHistory("テスト");
		assertEquals(1, history.getId());
		assertEquals("test", history.getSender());
		assertEquals("テスト", history.getWord());
		assertEquals("てすと", history.getPhonetic());
	}

	@Test
	public void testIsFollowed() throws SQLException {
		siritori.insertHistory("test", "テスト");
		assertEquals(true, siritori.isFollowed("とら"));
		assertEquals(false, siritori.isFollowed("ライオン"));
	}

	@Test
	public void isIsFinished() {
		assertEquals(false, siritori.isFinished("テスト"));
		assertEquals(true, siritori.isFinished("みかん"));
		assertEquals(true, siritori.isFinished("ン"));
	}

	@Test
	public void testIsHiragana() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Method isHiragana = getPrivateMethod(Siritori.class, "isHiragana", String.class);
		assertEquals(true, isHiragana.invoke(siritori, "ひらがな"));
		assertEquals(false, isHiragana.invoke(siritori, "カタカナ"));
		assertEquals(false, isHiragana.invoke(siritori, "漢字"));
		assertEquals(false, isHiragana.invoke(siritori, "ひらがなと漢字のテストです"));
	}

	@Test
	public void testGetReturnWords() throws SQLException {
		siritori.insertHistory("test", "しりとり");
	}

	@Test
	public void testGetOffset() throws SQLException {
		String last = "か";
		assertEquals(0, siritori.getOffset(last));
		siritori.incrementOffset(last);
		assertEquals(1, siritori.getOffset(last));
		siritori.incrementOffset(last);
		assertEquals(2, siritori.getOffset(last));
		siritori.incrementOffset(last);
		assertEquals(3, siritori.getOffset(last));
	}

	@Test
	public void testToUpperCaseJP() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Method toUpperCaseJP = getPrivateMethod(Siritori.class, "toUpperCaseJP", String.class);
		assertEquals("あいうえおつやゆよわアイウエオカケツヤユヨワ",
				toUpperCaseJP.invoke(siritori, "ぁぃぅぇぉっゃゅょゎァィゥェォヵヶッャュョヮ"));
	}

	public static Method getPrivateMethod(Class clazz, String name, Class... param) throws NoSuchMethodException {
		Method method = clazz.getDeclaredMethod(name, param);
		method.setAccessible(true);
		return method;
	}
}