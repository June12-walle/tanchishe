import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * GameSettings — 全局游戏设置（单例），持久化到 .tanchishe/settings.txt
 * 设置对"下一局"生效：速度在创建游戏窗口时读取，音乐在每次播放时判断，边界模式每拍判断。
 */
public final class GameSettings {

	private static final GameSettings INSTANCE = new GameSettings();

	public boolean bgmOn = true;       // 背景音乐：开/关
	public int tickMs = 100;           // 蛇每拍移动间隔：慢150 / 中100 / 快70
	public boolean wrapWalls = true;   // 边界模式：true=穿墙环绕 false=撞墙结束

	public static final int[] SPEED_OPTIONS = { 150, 100, 70 };
	public static final String[] SPEED_NAMES = { "慢", "中", "快" };

	private GameSettings() { load(); }

	public static GameSettings get() { return INSTANCE; }

	private Path file() {
		String base = System.getProperty("tanchishe.data.dir",
				Paths.get(System.getProperty("user.home"), ".tanchishe").toString());
		return Paths.get(base, "settings.txt");
	}

	public synchronized void load() {
		Path p = file();
		if (!Files.exists(p)) return;
		try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
			String line;
			while ((line = br.readLine()) != null) {
				int eq = line.indexOf('=');
				if (eq <= 0) continue;
				String k = line.substring(0, eq).trim();
				String v = line.substring(eq + 1).trim();
				switch (k) {
					case "bgmOn" -> bgmOn = Boolean.parseBoolean(v);
					case "tickMs" -> tickMs = Integer.parseInt(v);
					case "wrapWalls" -> wrapWalls = Boolean.parseBoolean(v);
					default -> { }
				}
			}
		} catch (Exception e) {
			System.err.println("[GameSettings] 读取失败: " + e);
		}
	}

	public synchronized void save() {
		try {
			Path p = file();
			Files.createDirectories(p.getParent());
			Files.writeString(p, "bgmOn=" + bgmOn + "\ntickMs=" + tickMs + "\nwrapWalls=" + wrapWalls + "\n",
					StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("[GameSettings] 写入失败: " + e);
		}
	}

	public int speedIndex() {
		for (int i = 0; i < SPEED_OPTIONS.length; i++) {
			if (SPEED_OPTIONS[i] == tickMs) return i;
		}
		return 1;
	}

	public String speedName() { return SPEED_NAMES[speedIndex()]; }
}
