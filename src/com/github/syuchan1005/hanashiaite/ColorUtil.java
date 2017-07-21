package com.github.syuchan1005.hanashiaite;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;

public class ColorUtil {

	public static String getMainColorHex(URL url) throws IOException {
		BufferedImage image = ImageIO.read(url);
		Map<Integer, Integer> map = new HashMap<>();
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				int rgb = image.getRGB(i, j);
				if (!isGray(getRGBAArr(rgb), 10)) {
					Integer counter = map.get(rgb);
					if (counter == null) counter = 0;
					counter++;
					map.put(rgb, counter);
				}
			}
		}
		return getMostCommonColour(map);
	}


	private static String getMostCommonColour(Map<Integer, Integer> map) {
		Optional<Map.Entry<Integer, Integer>> first = map.entrySet().stream()
				.sorted(Comparator.comparing(Map.Entry::getValue)).findFirst();
		if (first.isPresent()) {
			int[] rgb = getRGBAArr(first.get().getKey());
			return "#" + Integer.toHexString(rgb[0]) + Integer.toHexString(rgb[1]) + Integer.toHexString(rgb[2]);
		} else {
			return "#ffffff";
		}
	}

	private static int[] getRGBAArr(int pixel) {
		int alpha = (pixel >> 24) & 0xff;
		int red = (pixel >> 16) & 0xff;
		int green = (pixel >> 8) & 0xff;
		int blue = (pixel) & 0xff;
		return new int[]{red, green, blue, alpha};

	}

	private static boolean isGray(int[] rgbArr, int tolerance) {
		int rgDiff = rgbArr[0] - rgbArr[1];
		int rbDiff = rgbArr[0] - rgbArr[2];
		return (rgDiff <= tolerance && rgDiff >= -tolerance) || (rbDiff <= tolerance && rbDiff >= -tolerance);
	}
}
