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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.CardLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * MenuFrame — 主菜单（键盘 + 鼠标双通道）。
 *
 * 按键约定（全页面统一）：
 *   选择：↑/↓ 或 W/S        确认：空格/回车        返回：Esc/Backspace
 * 鼠标约定：悬停即选中；点击选中的项即确认（设置页点击选中的行 = 切换该行设置）。
 * 左上角返回按钮：菜单页显示"退出游戏"，其余页显示"← 返回"。
 *
 * 页面流：MENU → 开始游戏 → 登录窗 → 游戏窗（关闭后回菜单）
 *                → 登录查看排名 → 登录窗 → 排行榜页（↑/↓+空格或鼠标看详情，Esc/Backspace 回菜单）
 *                → 设置 → 设置页（↑/↓/W/S 选行，空格/回车/点击切换，Esc/Backspace 保存返回）
 */
public class MenuFrame extends JFrame {

	private static final String[] ITEMS = { "开始游戏", "登录查看排名", "设置" };
	private static final String[] ITEM_DESC = { "登录后开始一局贪吃蛇", "查看玩家排行榜与历史成绩", "音乐 / 速度 / 边界模式" };

	private static final String PAGE_MENU = "menu";
	private static final String PAGE_SETTINGS = "settings";
	private static final String PAGE_RANK = "rank";

	private String page = PAGE_MENU;
	private int sel = 0;

	private final CardLayout cards = new CardLayout();
	private RankPanel rankPanel;
	private JButton backBtn;
	private JPanel menuCard;
	private JPanel settingsCard;

	public MenuFrame() {
		setTitle("贪吃蛇 · 主菜单");
		setSize(560, 460);
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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
		backBtn.setFocusable(false); // 点击后键盘事件仍归窗口
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
		JPanel container = new JPanel(cards);
		container.setBackground(new Color(0x14181F));
		menuCard = buildMenuCard();
		settingsCard = buildSettingsCard();
		container.add(menuCard, PAGE_MENU);
		container.add(settingsCard, PAGE_SETTINGS);
		container.add(buildRankCard(), PAGE_RANK);
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
		switch (page) {
			case PAGE_MENU -> menuKey(code);
			case PAGE_SETTINGS -> settingsKey(code);
			case PAGE_RANK -> rankKey(code);
			default -> { }
		}
	}

	private void menuKey(int code) {
		switch (code) {
			case KeyEvent.VK_UP, KeyEvent.VK_W -> sel = (sel + ITEMS.length - 1) % ITEMS.length;
			case KeyEvent.VK_DOWN, KeyEvent.VK_S -> sel = (sel + 1) % ITEMS.length;
			case KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> activate();
			case KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE -> System.exit(0);
			default -> { }
		}
		repaint();
	}

	private void settingsKey(int code) {
		switch (code) {
			case KeyEvent.VK_UP, KeyEvent.VK_W -> sel = (sel + 2) % 3;
			case KeyEvent.VK_DOWN, KeyEvent.VK_S -> sel = (sel + 1) % 3;
			case KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> toggleSetting(sel);
			case KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE -> backToMenu();
			default -> { }
		}
		repaint();
	}

	private void rankKey(int code) {
		if (rankPanel.isDetailOpen()) { // 详情弹窗是模态的；弹窗关闭后状态复位
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
			case 0 -> { // 开始游戏：登录成功 → 开游戏窗（关闭后回菜单；直接关登录窗也回菜单）
				setVisible(false);
				LoginFrame login = new LoginFrame(() -> openGameWindow());
				addCloseFallback(login);
				login.setVisible(true);
			}
			case 1 -> { // 登录查看排名
				setVisible(false);
				LoginFrame login = new LoginFrame(this::showRankPage);
				addCloseFallback(login);
				login.setVisible(true);
			}
			case 2 -> showPage(PAGE_SETTINGS, "贪吃蛇 · 设置");
			default -> { }
		}
	}

	/** 登录窗被用户直接关闭（未登录）时，回主菜单，避免窗口全关进程假死 */
	private void addCloseFallback(LoginFrame login) {
		login.addWindowListener(new WindowAdapter() {
			@Override public void windowClosed(WindowEvent e) {
				if (!login.wasSuccessful()) showAgain();
			}
		});
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
		cards.show(getContentPane(), card);
		backBtn.setText(PAGE_MENU.equals(card) ? "退出游戏" : "← 返回");
		setTitle(title);
		repaint();
		requestFocusInWindow();
	}

	private void backToMenu() {
		rankPanel.closeDetail();
		showPage(PAGE_MENU, "贪吃蛇 · 主菜单");
		sel = 0;
	}

	/** 左上角按钮 / Esc / Backspace 的统一返回行为 */
	private void goBack() {
		if (PAGE_MENU.equals(page)) System.exit(0);
		else backToMenu();
	}

	private void showAgain() {
		SwingUtilities.invokeLater(() -> {
			backToMenu();
			setVisible(true);
			requestFocusInWindow();
		});
	}

	/** 登录成功后的排行榜页 */
	private void showRankPage() {
		setVisible(true);
		rankPanel.reload();
		showPage(PAGE_RANK, "贪吃蛇 · 排行榜");
	}

	/** 从菜单直接开一局（登录成功后）；游戏窗关闭自动回菜单 */
	private void openGameWindow() {
		String user = LoginFrame.lastLoginUser;
		JFrame game = new JFrame("贪吃蛇 · 玩家：" + user);
		game.setBounds(10, 10, 900, 720);
		game.setResizable(false);
		game.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		game.addWindowListener(new WindowAdapter() {
			@Override public void windowClosed(WindowEvent e) { showAgain(); }
		});
		game.add(new MPanel(user));
		game.setLocationRelativeTo(null);
		game.setVisible(true);
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
