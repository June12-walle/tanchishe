import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * LoginFrame — 启动首屏：登录 / 自动注册 + 排行榜浏览。
 * 左侧：用户名 + 密码 表单（新用户名直接注册，老用户名校验密码）
 * 右侧：按最高分排序的排行榜（点击任意玩家查看游玩次数与逐局成绩）
 */
public class LoginFrame extends JFrame {

	private final JTextField userField = new JTextField(14);
	private final JPasswordField passField = new JPasswordField(14);
	private final JLabel msgLabel = new JLabel(" ");
	private final JButton loginBtn = new JButton("进入游戏");

	private final DefaultListModel<String> boardModel = new DefaultListModel<>();
	private final JList<String> boardList = new JList<>(boardModel);
	private List<PlayerStore.PlayerRecord> boardCache = List.of();

	public LoginFrame() {
		setTitle("贪吃蛇 · 登录");
		setSize(760, 430);
		setResizable(false);
		setLocationRelativeTo(null);                 // 屏幕居中
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel root = new JPanel(new BorderLayout(16, 0));
		root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
		root.add(buildLoginForm(), BorderLayout.WEST);
		root.add(buildBoardPanel(), BorderLayout.CENTER);
		setContentPane(root);

		userField.addActionListener(e -> doLogin()); // 回车直达
		passField.addActionListener(e -> doLogin());
		loginBtn.addActionListener(e -> doLogin());
	}

	/** 左侧登录表单 */
	private JPanel buildLoginForm() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setPreferredSize(new Dimension(300, 300));
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
		gc.anchor = GridBagConstraints.WEST;
		gc.insets = new Insets(0, 0, 14, 0);

		JLabel title = new JLabel("欢迎来到贪吃蛇");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 19f));
		p.add(title, gc);

		gc.gridwidth = 1; gc.insets = new Insets(4, 0, 4, 8);
		JLabel tips = new JLabel("<html>已有账号直接登录；<br>输入新用户名将自动注册。</html>");
		tips.setForeground(Color.GRAY);
		gc.gridy = 1; gc.gridwidth = 2;
		p.add(tips, gc);

		gc.gridy = 2; gc.gridwidth = 1;
		p.add(new JLabel("用户名:"), gc);
		gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1;
		p.add(userField, gc);

		gc.gridx = 0; gc.gridy = 3; gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
		p.add(new JLabel("密  码:"), gc);
		gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1;
		p.add(passField, gc);

		gc.gridx = 0; gc.gridy = 4; gc.gridwidth = 2; gc.insets = new Insets(14, 0, 4, 0);
		loginBtn.setFont(loginBtn.getFont().deriveFont(Font.BOLD, 14f));
		p.add(loginBtn, gc);

		msgLabel.setForeground(new Color(0xC0392B));
		msgLabel.setPreferredSize(new Dimension(280, 40));
		gc.gridy = 5; gc.insets = new Insets(4, 0, 0, 0);
		p.add(msgLabel, gc);
		return p;
	}

	/** 右侧排行榜面板（含空态提示） */
	private JPanel buildBoardPanel() {
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(0xD8DEE9)),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)));
		JLabel head = new JLabel("排行榜 · 按最高分（点击玩家查看详情）");
		head.setFont(head.getFont().deriveFont(Font.BOLD, 14f));
		p.add(head, BorderLayout.NORTH);
		boardList.setFont(boardList.getFont().deriveFont(15f));
		boardList.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				int i = boardList.locationToIndex(e.getPoint());
				if (i >= 0 && i < boardCache.size()) showDetail(boardCache.get(i));
			}
		});
		p.add(new JScrollPane(boardList), BorderLayout.CENTER);
		refreshBoard();
		return p;
	}

	/** 从磁盘重载榜单（每次打开窗口都会是最新的本地档案） */
	private void refreshBoard() {
		boardModel.clear();
		boardCache = PlayerStore.topPlayers(10);
		if (boardCache.isEmpty()) {
			boardModel.addElement("暂无记录 —— 来玩第一局吧！");
			return;
		}
		int rank = 1;
		for (PlayerStore.PlayerRecord r : boardCache) {
			boardModel.addElement(rank++ + ".  " + r.user + "   最高 " + r.best + " 分 · " + r.plays + " 局");
		}
	}

	/** 登录入口：校验→调 PlayerStore→成功进游戏，失败亮提示 */
	private void doLogin() {
		String user = userField.getText().trim();
		String pass = new String(passField.getPassword());

		if (!PlayerStore.validUser(user)) {
			fail("用户名需 1~16 位中文/字母/数字/下划线");
			return;
		}
		String problem = PlayerStore.passProblem(pass.toCharArray());
		if (problem != null) { fail(problem); return; }

		int rc = PlayerStore.login(user, pass);
		switch (rc) {
			case PlayerStore.LOGIN_NEW -> succeed(user, "新账号注册成功，开始你的第一局吧！");
			case PlayerStore.LOGIN_OK -> succeed(user, null);
			case PlayerStore.LOGIN_WRONG -> fail("密码错误（该用户名已被注册）");
			default -> fail("本地档案读写失败，请稍后再试");
		}
	}

	private void fail(String text) {
		msgLabel.setText(text);
		msgLabel.setForeground(new Color(0xC0392B));
	}

	private void succeed(String user, String tip) {
		if (tip != null) {
			msgLabel.setText(tip);
			msgLabel.setForeground(new Color(0x1A9E6E));
		}
		openGame(user); // 注册/登录都直接进入游戏
	}

	/** 进入游戏：关闭登录窗，按原窗口参数拉起游戏主面板 */
	private void openGame(String user) {
		JFrame game = new JFrame("贪吃蛇 · 玩家：" + user);
		game.setBounds(10, 10, 900, 720);
		game.setResizable(false);
		game.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		game.add(new MPanel(user));
		game.setLocationRelativeTo(null);
		dispose();                       // 关闭登录窗
		game.setVisible(true);           // 出现游戏窗口
	}

	/** 点击玩家：弹出详情（总局数 + 最近各局成绩，最新在前） */
	private void showDetail(PlayerStore.PlayerRecord r) {
		StringBuilder sb = new StringBuilder();
		sb.append("玩家：").append(r.user)
		  .append("　共玩过 ").append(r.plays).append(" 局")
		  .append("　历史最高 ").append(r.best).append(" 分\n");
		if (r.hist.isEmpty()) {
			sb.append("\n（还没有成绩记录）");
		} else {
			sb.append('\n');
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			int n = r.hist.size();
			for (int i = n - 1; i >= 0; i--) {
				long[] e = r.hist.get(i);
				sb.append("第 ").append(n - i).append(" 局　")
				  .append(fmt.format(new Date(e[1])))
				  .append("　　").append(e[0]).append(" 分\n");
			}
		}
		javax.swing.JTextArea area = new javax.swing.JTextArea(sb.toString());
		area.setEditable(false);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, new JScrollPane(area),
				"玩家详情 · " + r.user, JOptionPane.PLAIN_MESSAGE));
	}
}
