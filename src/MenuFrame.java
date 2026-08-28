import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * MenuFrame — 主菜单（键盘驱动）。
 *
 * 页面流：MENU → (1)开始游戏 → 登录窗(带回调) → MPanel
 *                → (2)登录查看排名 → 登录窗 → 排行榜窗（可点玩家看详情，Esc 返回菜单）
 *                → (3)设置 → 设置页（↑/↓ 选行，空格/回车切换值，Esc 保存返回）
 * 全局按键：↑/↓ 选择，空格/回车 确认，Esc 返回/退出。
 */
public class MenuFrame extends JFrame {

	private static final String[] ITEMS = { "开始游戏", "登录查看排名", "设置" };
	private static final String[] ITEM_DESC = { "登录后开始一局贪吃蛇", "查看玩家排行榜与历史成绩", "音乐 / 速度 / 边界模式" };

	private static final int PAGE_MENU = 0;
	private static final int PAGE_SETTINGS = 1;
	private static final int PAGE_RANK_EMPTY = 2; // 未登录直接看榜单（游客模式）

	private int page = PAGE_MENU;
	private int sel = 0;          // 菜单/设置里的当前选中行

	private RankPanel rankPanel;  // 排行榜页面（登录后或游客模式共用）

	public MenuFrame() {
		setTitle("贪吃蛇 · 主菜单");
		setSize(560, 420);
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setContentPane(buildContent());
		addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) { handleKey(e); }
		});
		setFocusable(true);
		SwingUtilities.invokeLater(this::requestFocusInWindow);
	}

	private JPanel buildContent() {
		JPanel root = new JPanel(new BorderLayout()) {
			@Override protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				setBackground(new Color(0x14181F));
			}
		};
		rankPanel = new RankPanel();
		root.add(rankPanel, BorderLayout.CENTER);
		return root;
	}

	// ---------- 按键路由 ----------
	private void handleKey(KeyEvent e) {
		int code = e.getKeyCode();
		// 排行榜浏览页：详情弹窗打开时只响应 Esc 关弹窗；否则 ↑/↓/空格 操作列表，Esc 回菜单
		if (page == PAGE_RANK_EMPTY) {
			if (rankPanel.isDetailOpen()) {
				if (code == KeyEvent.VK_ESCAPE) rankPanel.closeDetail();
				return;
			}
			switch (code) {
				case KeyEvent.VK_UP -> rankPanel.moveSelection(-1);
				case KeyEvent.VK_DOWN -> rankPanel.moveSelection(1);
				case KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> rankPanel.openSelected();
				case KeyEvent.VK_ESCAPE -> backToMenu();
				default -> { }
			}
			repaint();
			return;
		}
		switch (page) {
			case PAGE_MENU -> menuKey(code);
			case PAGE_SETTINGS -> settingsKey(code);
			default -> { }
		}
		repaint();
	}

	private void menuKey(int code) {
		switch (code) {
			case KeyEvent.VK_UP -> sel = (sel + ITEMS.length - 1) % ITEMS.length;
			case KeyEvent.VK_DOWN -> sel = (sel + 1) % ITEMS.length;
			case KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> activate();
			case KeyEvent.VK_ESCAPE -> System.exit(0);
			default -> { }
		}
	}

	private void activate() {
		switch (sel) {
			case 0 -> { // 开始游戏
				LoginFrame login = new LoginFrame(() -> {
					openGameWindow();
					showAgain(); // 游戏窗口关闭后回到菜单
				});
				login.setVisible(true);
				setVisible(false);
			}
			case 1 -> { // 登录查看排名
				LoginFrame login = new LoginFrame(this::showRankDialogMode);
				login.setVisible(true);
				setVisible(false);
			}
			case 2 -> { page = PAGE_SETTINGS; sel = 0; }
			default -> { }
		}
	}

	private void settingsKey(int code) {
		GameSettings gs = GameSettings.get();
		int speedIdx = gs.speedIndex();
		switch (code) {
			case KeyEvent.VK_UP -> sel = (sel + 2) % 3;
			case KeyEvent.VK_DOWN -> sel = (sel + 1) % 3;
			case KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> {
				switch (sel) {
					case 0 -> gs.bgmOn = !gs.bgmOn;
					case 1 -> gs.tickMs = GameSettings.SPEED_OPTIONS[(speedIdx + 1) % GameSettings.SPEED_OPTIONS.length];
					case 2 -> gs.wrapWalls = !gs.wrapWalls;
					default -> { }
				}
				gs.save();
			}
			case KeyEvent.VK_ESCAPE -> { page = PAGE_MENU; sel = 0; }
			default -> { }
		}
	}

	private void backToMenu() {
		page = PAGE_MENU;
		sel = 0;
		rankPanel.setDialogMode(false);
		rankPanel.closeDetail();
		setTitle("贪吃蛇 · 主菜单");
		repaint();
		requestFocusInWindow();
	}

	private void showAgain() {
		setVisible(true);
		requestFocus();
	}

	/** 登录成功后的排行榜页：读档并进入浏览模式（↑/↓+空格 查看，Esc 返回菜单） */
	private void showRankDialogMode() {
		setVisible(true);
		requestFocusInWindow();
		rankPanel.reload();
		rankPanel.setDialogMode(true);
		page = PAGE_RANK_EMPTY;
		setTitle("贪吃蛇 · 排行榜（Esc 返回）");
		repaint();
	}

	/** 从菜单直接开一局（已登录用户） */
	private void openGameWindow() {
		String user = LoginFrame.lastLoginUser;
		JFrame game = new JFrame("贪吃蛇 · 玩家：" + user);
		game.setBounds(10, 10, 900, 720);
		game.setResizable(false);
		game.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		MPanel panel = new MPanel(user);
		panel.setExitCallback(this::showAgain);
		game.add(panel);
		game.setLocationRelativeTo(null);
		game.setVisible(true);
	}

	// ---------- 绘制 ----------
	@Override public void paint(Graphics g) {
		super.paint(g);
		if (page == PAGE_MENU) drawMenu(g);
		else if (page == PAGE_SETTINGS) drawSettings(g);
		else if (page == PAGE_RANK_EMPTY) drawRankPageHint(g);
	}

	private void drawMenu(Graphics g) {
		g.setColor(new Color(0x14181F));
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setColor(Color.WHITE);
		g.setFont(new Font("微软雅黑", Font.BOLD, 34));
		g.drawString("贪 吃 蛇", 210, 80);

		g.setFont(new Font("微软雅黑", Font.PLAIN, 13));
		g.setColor(new Color(0x8A93A8));
		g.drawString("↑/↓ 选择    空格/回车 确认    Esc 退出", 185, 112);

		for (int i = 0; i < ITEMS.length; i++) {
			int y = 165 + i * 70;
			boolean active = (i == sel);
			g.setColor(active ? new Color(0x2F6FED) : new Color(0x232A38));
			g.fillRoundRect(140, y - 34, 280, 54, 14, 14);
			if (active) {
				g.setColor(new Color(0x7EA6FF));
				g.drawRoundRect(140, y - 34, 280, 54, 14, 14);
			}
			g.setColor(active ? Color.WHITE : new Color(0xAAB4C8));
			g.setFont(new Font("微软雅黑", Font.BOLD, active ? 20 : 18));
			g.drawString(ITEMS[i], 250, y - 8);
			g.setFont(new Font("微软雅黑", Font.PLAIN, 12));
			g.setColor(new Color(active ? 0xC9D6F2 : 0x6E7890));
			g.drawString(ITEM_DESC[i], 168, y + 12);
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
		g.drawString("↑/↓ 选行    空格 切换    Esc 保存并返回", 190, 88);

		GameSettings gs = GameSettings.get();
		String[][] rows = {
				{ "背景音乐", gs.bgmOn ? "开" : "关" },
				{ "速度", gs.speedName() + " (" + gs.tickMs + "ms/拍)" },
				{ "边界模式", gs.wrapWalls ? "穿墙环绕" : "撞墙结束" },
		};
		for (int i = 0; i < rows.length; i++) {
			int y = 150 + i * 64;
			boolean active = (i == sel);
			g.setColor(active ? new Color(0x2F6FED) : new Color(0x232A38));
			g.fillRoundRect(120, y - 30, 320, 48, 12, 12);
			g.setColor(active ? Color.WHITE : new Color(0xAAB4C8));
			g.setFont(new Font("微软雅黑", Font.PLAIN, 17));
			g.drawString(rows[i][0], 150, y);
			g.setFont(new Font("微软雅黑", Font.BOLD, 17));
			g.setColor(active ? new Color(0x9FE0B8) : new Color(0x8A93A8));
			g.drawString(rows[i][1], 330, y);
		}
		g.setFont(new Font("微软雅黑", Font.PLAIN, 12));
		g.setColor(new Color(0x6E7890));
		g.drawString("设置对下一局生效", 235, 370);
	}

	private void drawRankPageHint(Graphics g) {
		g.setColor(new Color(0x14181F));
		g.fillRect(0, 0, getWidth(), getHeight());
		if (rankPanel.isEmpty()) {
			g.setColor(new Color(0xAAB4C8));
			g.setFont(new Font("微软雅黑", Font.PLAIN, 16));
			g.drawString("排行榜为空 —— 去玩第一局吧！", 195, 200);
		}
		g.setColor(new Color(0x6E7890));
		g.setFont(new Font("微软雅黑", Font.PLAIN, 13));
		g.drawString("Esc 返回主菜单", 230, 380);
	}
}
