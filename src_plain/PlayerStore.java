import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

/**
 * PlayerStore — 玩家档案本地持久化（无外部依赖，纯 JDK 实现）
 *
 * 存储位置：%USERPROFILE%/.tanchishe/players.txt（可用系统属性 tanchishe.data.dir 重定向，便于测试）
 * 文件格式：每行一个玩家，Tab 分隔：
 *   用户名 \t saltHex:sha256Hex(salt+password) \t 总局数 \t 最高分 \t 成绩历史
 *   其中成绩历史为逗号分隔的 "分数@时间戳毫秒" 条目，只保留最近 HIST_LIMIT 条。
 * 安全说明：SHA-256 加盐足以防明文泄露；作为单机演示项目不追求对抗性安全。
 * 并发说明：所有读写都在同一 EDT 线程内顺序执行，另加类级锁兜底双开窗口场景。
 */
public final class PlayerStore {

	public static final int LOGIN_OK = 1;        // 老用户密码正确
	public static final int LOGIN_NEW = 0;       // 新用户，自动注册成功
	public static final int LOGIN_WRONG = -1;    // 用户存在但密码错误
	public static final int LOGIN_ERROR = -2;    // 读写出错

	public static final int HIST_LIMIT = 50;     // 每个玩家最多保留最近 50 局记录

	private static final Object LOCK = new Object();

	/** 单个玩家的完整档案 */
	public static final class PlayerRecord {
		public String user;
		public String passStored;            // "saltHex:sha256Hex"
		public int plays;
		public int best;
		public final List<long[]> hist = new ArrayList<>(); // 每项 [score, epochMillis]

		String serialize() {
			StringBuilder sb = new StringBuilder();
			sb.append(user).append('\t').append(passStored).append('\t')
			  .append(plays).append('\t').append(best).append('\t');
			for (int i = 0; i < hist.size(); i++) {
				if (i > 0) sb.append(',');
				sb.append(hist.get(i)[0]).append('@').append(hist.get(i)[1]);
			}
			return sb.toString();
		}

		static PlayerRecord parse(String line) {
			String[] f = line.split("\t", -1); // -1 保留末尾空字段（无成绩历史的注册行末尾带空制表符）
			if (f.length < 5) return null;
			PlayerRecord r = new PlayerRecord();
			r.user = f[0].trim();
			if (r.user.isEmpty()) return null;
			r.passStored = f[1];
			try {
				r.plays = Integer.parseInt(f[2]);
				r.best = Integer.parseInt(f[3]);
			} catch (NumberFormatException e) { return null; }
			if (!f[4].isEmpty()) {
				for (String h : f[4].split(",")) {
					int at = h.indexOf('@');
					if (at <= 0 || at == h.length() - 1) continue;
					try {
						r.hist.add(new long[]{ Integer.parseInt(h.substring(0, at)), Long.parseLong(h.substring(at + 1)) });
					} catch (NumberFormatException ignored) { }
				}
			}
			return r;
		}
	}

	private PlayerStore() { }

	/** 数据文件路径 */
	private static Path dataFile() {
		String base = System.getProperty("tanchishe.data.dir",
				Paths.get(System.getProperty("user.home"), ".tanchishe").toString());
		return Paths.get(base, "players.txt");
	}

	/** 读取全部玩家档案（损坏行自动跳过；空集合视为无档案） */
	public static List<PlayerRecord> loadAll() {
		synchronized (LOCK) {
			List<PlayerRecord> out = new ArrayList<>();
			Path p = dataFile();
			if (!Files.exists(p)) return out;
			try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
				String line;
				while ((line = br.readLine()) != null) {
					PlayerRecord r = PlayerRecord.parse(line);
					if (r != null) out.add(r);
				}
			} catch (IOException e) {
				System.err.println("[PlayerStore] 读取失败: " + e);
			}
			return out;
		}
	}

	/** 写回全部档案（整文件覆盖，原子性靠临时文件+移动保证） */
	private static void saveAll(List<PlayerRecord> recs) throws IOException {
		Path p = dataFile();
		Files.createDirectories(p.getParent());
		Path tmp = p.resolveSibling("players.tmp");
		try (BufferedWriter bw = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
			for (PlayerRecord r : recs) bw.write(r.serialize() + System.lineSeparator());
		}
		Files.move(tmp, p, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
				java.nio.file.StandardCopyOption.ATOMIC_MOVE);
	}

	private static PlayerRecord find(List<PlayerRecord> recs, String user) {
		for (PlayerRecord r : recs) if (r.user.equals(user)) return r;
		return null;
	}

	/** 用户名合法性：1~16 位中文/字母/数字/下划线 */
	public static boolean validUser(String u) {
		return u != null && u.matches("[\\u4e00-\\u9fa5A-Za-z0-9_]{1,16}");
	}

	/** 校验口令：合法返回 0，否则返回最小长度要求提示 */
	public static String passProblem(char[] pw) {
		if (pw.length < 3) return "密码至少 3 位";
		return null;
	}

	/** 登录：新用户自动注册，老用户校验密码（返回 LOGIN_* 常量） */
	public static int login(String user, String password) {
		if (!validUser(user)) return LOGIN_WRONG; // 由调用方提前校验并给友好提示，这里兜底
		synchronized (LOCK) {
			try {
				List<PlayerRecord> recs = loadAll();
				PlayerRecord me = find(recs, user);
				if (me == null) {
					me = new PlayerRecord();
					me.user = user;
					me.passStored = hash(password);
					me.plays = 0;
					me.best = 0;
					recs.add(me);
					saveAll(recs);
					return LOGIN_NEW;
				}
				return me.passStored.equals(hashWith(password, saltOf(me.passStored))) ? LOGIN_OK : LOGIN_WRONG;
			} catch (Exception e) {
				System.err.println("[PlayerStore] 登录异常: " + e);
				return LOGIN_ERROR;
			}
		}
	}

	/** 记录一局成绩并返回更新后的最高分 */
	public static int recordScore(String user, int score) {
		synchronized (LOCK) {
			List<PlayerRecord> recs = loadAll();
			PlayerRecord me = find(recs, user);
			if (me == null) { // 极端情况：档案被手动删除，现场重建
				me = new PlayerRecord();
				me.user = user;
				me.passStored = hash("");
				recs.add(me);
			}
			me.plays++;
			me.hist.add(new long[]{ score, System.currentTimeMillis() });
			while (me.hist.size() > HIST_LIMIT) me.hist.remove(0);
			if (score > me.best) me.best = score;
			try {
				saveAll(recs);
			} catch (IOException e) {
				System.err.println("[PlayerStore] 写档失败: " + e);
			}
			return me.best;
		}
	}

	/** 取某玩家当前最高分（无档案返回 0） */
	/** 校验某用户密码（不自动注册；档案不存在返回 false） */
	public static boolean verifyPassword(String user, String password) {
		PlayerRecord me = find(loadAll(), user);
		return me != null && me.passStored.equals(hashWith(password, saltOf(me.passStored)));
	}

	/** 修改密码：校验旧密码后更新；返回 0 成功 / -1 旧密码错误 / -2 用户不存在 / -3 写档失败 */
	public static int changePassword(String user, String oldPw, String newPw) {
		synchronized (LOCK) {
			List<PlayerRecord> recs = loadAll();
			PlayerRecord me = find(recs, user);
			if (me == null) return -2;
			if (!me.passStored.equals(hashWith(oldPw, saltOf(me.passStored)))) return -1;
			me.passStored = hash(newPw);
			try {
				saveAll(recs);
				return 0;
			} catch (IOException e) {
				System.err.println("[PlayerStore] 改密码写档失败: " + e);
				return -3;
			}
		}
	}

	public static int bestOf(String user) {
		PlayerRecord me = find(loadAll(), user);
		return me == null ? 0 : me.best;
	}

	/** 排行榜：按最高分降序、总局数次序、名称升序取前 limit 名 */
	public static List<PlayerRecord> topPlayers(int limit) {
		List<PlayerRecord> all = loadAll();
		all.sort(Comparator.<PlayerRecord>comparingInt(r -> r.best).reversed()
				.thenComparing(Comparator.<PlayerRecord>comparingInt(r -> r.plays).reversed())
				.thenComparing(r -> r.user));
		return all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
	}

	// ---- 口令哈希工具 ----
	private static String hash(String password) {
		byte[] salt = new byte[8];
		new SecureRandom().nextBytes(salt);
		return hashWith(password, HexFormat.of().formatHex(salt));
	}

	private static String hashWith(String password, String saltHex) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(HexFormat.of().parseHex(saltHex));
			return saltHex + ":" + HexFormat.of().formatHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String saltOf(String stored) {
		int c = stored.indexOf(':');
		return c < 0 ? "" : stored.substring(0, c);
	}
}
