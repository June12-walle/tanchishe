import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.CardLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * MenuFrame — 唯一主窗口（单窗口多页面，CardLayout 切换）。
 *
 * 页面：MENU 主菜单 / LOGIN 登录 / RANK 排行榜 / SETTINGS 设置 / GAME 游戏
 * 按键约定：↑↓/W S 选择，空格/回车 确认，Esc/Backspace 返回；游戏页按键归 MPanel。
 * 鼠标：悬停选中、点击确认；左上角返回按钮按页面显示"退出游戏"/"← 返回"。
 */
public class MenuFrame extends JFrame {

	private static MenuFrame active; // 静态引用：供子页面回调返回主菜单
	public static void requestBackToMenu() {
		if (active != null) SwingUtilities.invokeLater(active::backToMenu);
	}

	private static final String[] ITEMS = { "开始游戏", "登录查看排名", "设置" };
	private static final String[] ITEM_DESC = { "登录后开始一局贪吃蛇", "查看玩家排行榜与历史成绩", "音乐 / 速度 / 边界模式" };

	private static final String PAGE_MENU = "menu";
	private static final String PAGE_LOGIN = "login";
	private static final String PAGE_RANK = "rank";
	private static final String PAGE_SETTINGS = "settings";
	private static final String PAGE_GAME = "game";

	private String page = PAGE_MENU;
	private int sel = 0;
	private String loginPurpose = "game"; // 登录成功后的去向：game / rank

	private final CardLayout cards = new CardLayout();
	private JPanel cardHost;      // CardLayout 宿主（show() 只能用它）
	private RankPanel rankPanel;
	private LoginPanel loginPanel;
	private JPanel gameHost;
	private MPanel currentGame;   // 当前游戏页实例
	private JButton backBtn;

	public MenuFrame() {
		setTitle("贪吃蛇");
		setSize(560, 460);
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		active = this;

		JPanel root = new JPanel(new BorderLayout());
		root.add(buildTopBar(), BorderLayout.NORTH);
		root.add(buildCards(), BorderLayout.CENTER);
		setContentPane(root);

		addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) { handleKey(e); }
		});
		setFocusable(true);
		SwingUtilities.invokeLater(this::requestFocusInWindow);
	}

	/** 左上角返回按钮（不抢键盘焦点） */
	private JPanel buildTopBar() {
		JPanel bar = new JPanel(new BorderLayout());
		bar.setBackground(new Color(0x14181F));
		bar.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 8));
		backBtn = new JButton("退出游戏");
		backBtn.setFocusable(false);
		backBtn.setBackground(new Color(0x232A38));
		backBtn.setForeground(new Color(0xC9D6F2));
		backBtn.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
		backBtn.addActionListener(e -> goBack());
		JPanel left = new JPanel();
		left.setOpaque(false);
		left.add(backBtn);
		bar.add(left, BorderLayout.WEST);
		return bar;
	}

	private JPanel buildCards() {
		JPanel container = cardHost = new JPanel(cards);
		container.setBackground(new Color(0x14181F));
		container.add(buildMenuCard(), PAGE_MENU);
		container.add(buildLoginCard(), PAGE_LOGIN);
		container.add(buildRankCard(), PAGE_RANK);
		container.add(buildSettingsCard(), PAGE_SETTINGS);
		container.add(buildGameCard(), PAGE_GAME);
		return container;
	}

	// ---------- 卡片构建 ----------
	private JPanel buildMenuCard() {
		JPanel p = new JPanel() {
			@Override protected void paintComponent(Graphics g) { drawMenu(g); }
		};
		MouseAdapter ma = new MouseAdapter() {
			@Override public void mouseMoved(MouseEvent e) {
				int i = menuRowAt(e.getPoint());
				if (i >= 0 && i != sel) { sel = i; p.repaint(); }
			}
			@Override public void mouseClicked(MouseEvent e) {
				int i = menuRowAt(e.getPoint());
				if (i >= 0) { sel = i; activate(); }
			}
		};
		p.addMouseListener(ma);
		p.addMouseMotionListener(ma);
		return p;
	}

	private JPanel buildLoginCard() {
		loginPanel = new LoginPanel(() -> onLoginSuccess());
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(new Color(0x14181F));
		p.add(loginPanel, BorderLayout.CENTER);
		JLabel hint = new JLabel("Esc/退格 返回主菜单", JLabel.CENTER);
		hint.setForeground(new Color(0x8A93A8));
		hint.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));
		p.add(hint, BorderLayout.SOUTH);
		return p;
	}

	private JPanel buildRankCard() {
		rankPanel = new RankPanel();
		JPanel p = new JPanel(new BorderLayout());
		p.setBackground(new Color(0x14181F));
		p.add(rankPanel, BorderLayout.CENTER);
		JLabel hint = new JLabel("↑↓/W S 选择　空格/回车 查看详情　Esc/退格 返回主菜单", JLabel.CENTER);
		hint.setForeground(new Color(0x8A93A8));
		hint.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));
		p.add(hint, BorderLayout.SOUTH);
		return p;
	}

	private JPanel buildSettingsCard() {
		JPanel p = new JPanel() {
			@Override protected void paintComponent(Graphics g) { drawSettings(g); }
		};
		MouseAdapter ma = new MouseAdapter() {
			@Override public void mouseMoved(MouseEvent e) {
				int i = settingsRowAt(e.getPoint());
				if (i >= 0 && i != sel) { sel = i; p.repaint(); }
			}
			@Override public void mouseClicked(MouseEvent e) {
				int i = settingsRowAt(e.getPoint());
				if (i >= 0) { sel = i; toggleSetting(i); p.repaint(); }
			}
		};
		p.addMouseListener(ma);
		p.addMouseMotionListener(ma);
		return p;
	}

	private JPanel buildGameCard() {
		gameHost = new JPanel(new BorderLayout());
		gameHost.setBackground(Color.WHITE);
		JLabel placeholder = new JLabel("从主菜单选择「开始游戏」进入", JLabel.CENTER);
		placeholder.setForeground(new Color(0x8A93A8));
		gameHost.add(placeholder, BorderLayout.CENTER);
		return gameHost;
	}

	// ---------- 命中检测 ----------
	private Rectangle menuRect(int i) { return new Rectangle(140, 165 + i * 70 - 34, 280, 54); }
	private Rectangle settingsRect(int i) { return new Rectangle(120, 150 + i * 64 - 30, 320, 48); }

	private int menuRowAt(Point pt) {
		for (int i = 0; i < ITEMS.length; i++) if (menuRect(i).contains(pt)) return i;
		return -1;
	}

	private int settingsRowAt(Point pt) {
		for (int i = 0; i < 3; i++) if (settingsRect(i).contains(pt)) return i;
		return -1;
	}

	// ---------- 按键路由 ----------
	private void handleKey(KeyEvent e) {
		int code = e.getKeyCode();
		if (PAGE_GAME.equals(page)) {
			// 游戏页：其余按键归 MPanel（焦点在它身上时本监听器通常收不到）
			if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) exitGameToMenu();
			return;
		}
		switch (page) {
			case PAGE_MENU -> menuKey(code);
			case PAGE_SETTINGS -> settingsKey(code);
			case PAGE_RANK -> rankKey(code);
			case PAGE_LOGIN -> { if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) backToMenu(); }
			default -> { }
		}
		repaint();
	}

	private void menuKey(int code) {
		switch (code) {
			case KeyEvent.VK_UP, KeyEvent.VK_W -> sel = (sel + ITEMS.length - 1) % ITEMS.length;
			case KeyEvent.VK_DOWN, KeyEvent.VK_S -> sel = (sel + 1) % ITEMS.length;
			case KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> activate();
			case KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE -> System.exit(0);
			default -> { }
		}
	}

	private void settingsKey(int code) {
		switch (code) {
			case KeyEvent.VK_UP, KeyEvent.VK_W -> sel = (sel + 2) % 3;
			case KeyEvent.VK_DOWN, KeyEvent.VK_S -> sel = (sel + 1) % 3;
			case KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> toggleSetting(sel);
			case KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE -> backToMenu();
			default -> { }
		}
	}

	private void rankKey(int code) {
		if (rankPanel.isDetailOpen()) {
			if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) rankPanel.closeDetail();
			return;
		}
		switch (code) {
			case KeyEvent.VK_UP, KeyEvent.VK_W -> rankPanel.moveSelection(-1);
			case KeyEvent.VK_DOWN, KeyEvent.VK_S -> rankPanel.moveSelection(1);
			case KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> rankPanel.openSelected();
			case KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE -> backToMenu();
			default -> { }
		}
	}

	// ---------- 页面动作 ----------
	private void activate() {
		switch (sel) {
			case 0 -> startGameFlow();
			case 1 -> {
				if (LoginPanel.lastLoginUser != null) enterRank();
				else { loginPurpose = "rank"; loginPanel.prefill(LoginPanel.lastLoginUser); showPage(PAGE_LOGIN, "贪吃蛇 · 登录（查看排名）"); }
			}
			case 2 -> showPage(PAGE_SETTINGS, "贪吃蛇 · 设置");
			default -> { }
		}
	}

	private void startGameFlow() {
		if (LoginPanel.lastLoginUser != null) { startGame(LoginPanel.lastLoginUser); return; }
		loginPurpose = "game";
		loginPanel.clearPassword();
		showPage(PAGE_LOGIN, "贪吃蛇 · 登录（开始游戏）");
	}

	/** 登录成功：按进入登录页前的目的跳转 */
	private void onLoginSuccess() {
		if ("rank".equals(loginPurpose)) enterRank();
		else startGame(LoginPanel.lastLoginUser);
	}

	private void enterRank() {
		rankPanel.reload();
		showPage(PAGE_RANK, "贪吃蛇 · 排行榜");
	}

	/** 同一窗口内开一局：每次都换新的 MPanel 实例（新开局） */
	private void startGame(String user) {
		if (currentGame != null) currentGame.shutdown();
		currentGame = new MPanel(user);
		gameHost.removeAll();
		gameHost.add(currentGame, BorderLayout.CENTER);
		gameHost.revalidate();
		gameHost.repaint();
		showPage(PAGE_GAME, "贪吃蛇 · 玩家：" + user);
		SwingUtilities.invokeLater(currentGame::requestFocusInWindow);
	}

	/** 游戏中返回主菜单：停时钟停音乐，回菜单页 */
	private void exitGameToMenu() {
		if (currentGame != null) currentGame.shutdown();
		backToMenu();
	}

	private void toggleSetting(int row) {
		GameSettings gs = GameSettings.get();
		switch (row) {
			case 0 -> gs.bgmOn = !gs.bgmOn;
			case 1 -> gs.tickMs = GameSettings.SPEED_OPTIONS[(gs.speedIndex() + 1) % GameSettings.SPEED_OPTIONS.length];
			case 2 -> gs.wrapWalls = !gs.wrapWalls;
			default -> { }
		}
		gs.save();
	}

	private void showPage(String card, String title) {
		page = card;
		cards.show(cardHost, card); // parent 必须是 cards 的宿主容器
		backBtn.setText(PAGE_MENU.equals(card) ? "退出游戏" : "← 返回");
		setTitle(title);
		// 按页面调整窗口尺寸并居中
		Dimension d = switch (card) {
			case PAGE_GAME -> new Dimension(906, 762);
			case PAGE_RANK -> new Dimension(780, 520);
			default -> new Dimension(560, 460);
		};
		if (!d.equals(getSize())) { setSize(d); setLocationRelativeTo(null); }
		repaint();
		requestFocusInWindow();
	}

	private void backToMenu() {
		rankPanel.closeDetail();
		sel = 0;
		showPage(PAGE_MENU, "贪吃蛇");
	}

	/** 左上角按钮 / Esc / Backspace 的统一返回行为 */
	private void goBack() {
		if (PAGE_GAME.equals(page)) { exitGameToMenu(); return; }
		if (PAGE_MENU.equals(page)) System.exit(0);
		else backToMenu();
	}

	// ---------- 绘制 ----------
	private void drawMenu(Graphics g) {
		g.setColor(new Color(0x14181F));
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setColor(Color.WHITE);
		g.setFont(new Font("微软雅黑", Font.BOLD, 34));
		g.drawString("贪 吃 蛇", 210, 80);

		g.setFont(new Font("微软雅黑", Font.PLAIN, 13));
		g.setColor(new Color(0x8A93A8));
		g.drawString("↑↓/W S 选择　空格/回车 确认　Esc/退格 返回　鼠标点击亦可", 128, 112);

		for (int i = 0; i < ITEMS.length; i++) {
			Rectangle r = menuRect(i);
			boolean active = (i == sel);
			g.setColor(active ? new Color(0x2F6FED) : new Color(0x232A38));
			g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
			if (active) {
				g.setColor(new Color(0x7EA6FF));
				g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
			}
			g.setColor(active ? Color.WHITE : new Color(0xAAB4C8));
			g.setFont(new Font("微软雅黑", Font.BOLD, active ? 20 : 18));
			g.drawString(ITEMS[i], 250, r.y + 26);
			g.setFont(new Font("微软雅黑", Font.PLAIN, 12));
			g.setColor(new Color(active ? 0xC9D6F2 : 0x6E7890));
			g.drawString(ITEM_DESC[i], 168, r.y + 46);
		}
	}

	private void drawSettings(Graphics g) {
		g.setColor(new Color(0x14181F));
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.WHITE);
		g.setFont(new Font("微软雅黑", Font.BOLD, 24));
		g.drawString("设  置", 235, 60);
		g.setFont(new Font("微软雅黑", Font.PLAIN, 13));
		g.setColor(new Color(0x8A93A8));
		g.drawString("↑↓/W S 选行　空格/回车/点击 切换　Esc/退格 保存并返回", 138, 88);

		GameSettings gs = GameSettings.get();
		String[][] rows = {
				{ "背景音乐", gs.bgmOn ? "开" : "关" },
				{ "速度", gs.speedName() + " (" + gs.tickMs + "ms/拍)" },
				{ "边界模式", gs.wrapWalls ? "穿墙环绕" : "撞墙结束" },
		};
		for (int i = 0; i < rows.length; i++) {
			Rectangle r = settingsRect(i);
			boolean active = (i == sel);
			g.setColor(active ? new Color(0x2F6FED) : new Color(0x232A38));
			g.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
			g.setColor(active ? Color.WHITE : new Color(0xAAB4C8));
			g.setFont(new Font("微软雅黑", Font.PLAIN, 17));
			g.drawString(rows[i][0], r.x + 30, r.y + 30);
			g.setFont(new Font("微软雅黑", Font.BOLD, 17));
			g.setColor(active ? new Color(0x9FE0B8) : new Color(0x8A93A8));
			g.drawString(rows[i][1], r.x + 210, r.y + 30);
		}
		g.setFont(new Font("微软雅黑", Font.PLAIN, 12));
		g.setColor(new Color(0x6E7890));
		g.drawString("设置对下一局生效", 235, 385);
	}
}
