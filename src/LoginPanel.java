import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * LoginPanel — 登录页（内嵌主窗口的卡片页面，不再是独立窗口）。
 * 登录成功回调 onLoginSuccess 由 MenuFrame 决定去向。
 */
public class LoginPanel extends JPanel {

	public static String lastLoginUser = null;

	private final JTextField userField = new JTextField(14);
	private final JPasswordField passField = new JPasswordField(14);
	private final JLabel msgLabel = new JLabel(" ");
	private final JButton loginBtn = new JButton("登录 / 注册");
	private final JButton backBtn = new JButton("← 返回主菜单");

	private final RankPanel rankPanel; // 右侧榜单（只读展示）
	private final Runnable onLoginSuccess;

	public LoginPanel(Runnable onLoginSuccess) {
		this.onLoginSuccess = onLoginSuccess;
		this.rankPanel = new RankPanel();

		setLayout(new BorderLayout(16, 0));
		setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
		setBackground(new Color(0x14181F));
		add(buildLoginForm(), BorderLayout.WEST);
		add(rankPanel, BorderLayout.CENTER);

		userField.addActionListener(e -> doLogin());
		passField.addActionListener(e -> doLogin());
		loginBtn.addActionListener(e -> doLogin());
		backBtn.addActionListener(e -> goBack());

		// Esc/退格 返回主菜单（输入框焦点在子组件时，父面板监听器收不到，因此挂在两个输入框上）
		KeyAdapter escBack = new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) {
				int c = e.getKeyCode();
				if (c == KeyEvent.VK_ESCAPE || c == KeyEvent.VK_BACK_SPACE) goBack();
			}
		};
		userField.addKeyListener(escBack);
		passField.addKeyListener(escBack);
	}

	/** 左侧登录表单 */
	private JPanel buildLoginForm() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setOpaque(false);
		p.setPreferredSize(new Dimension(310, 320));
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
		gc.anchor = GridBagConstraints.WEST;
		gc.insets = new Insets(0, 0, 14, 0);

		JLabel title = new JLabel("欢迎来到贪吃蛇");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 19f));
		title.setForeground(new Color(0xE6ECF7));
		p.add(title, gc);

		JLabel tips = new JLabel("<html><span style='color:#8A93A8'>已有账号直接登录；<br>输入新用户名将自动注册。</span></html>");
		gc.gridy = 1; gc.insets = new Insets(4, 0, 8, 0);
		p.add(tips, gc);

		gc.gridwidth = 1; gc.insets = new Insets(4, 0, 4, 8);
		JLabel l1 = new JLabel("用户名:"); l1.setForeground(new Color(0xC9D6F2)); p.add(l1, gc);
		gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1;
		p.add(userField, gc);

		gc.gridx = 0; gc.gridy = 3; gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
		JLabel l2 = new JLabel("密  码:"); l2.setForeground(new Color(0xC9D6F2)); p.add(l2, gc);
		gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1;
		p.add(passField, gc);

		gc.gridx = 0; gc.gridy = 4; gc.gridwidth = 2; gc.insets = new Insets(14, 0, 4, 0);
		gc.fill = GridBagConstraints.HORIZONTAL;
		loginBtn.setFont(loginBtn.getFont().deriveFont(Font.BOLD, 14f));
		p.add(loginBtn, gc);

		backBtn.setFont(backBtn.getFont().deriveFont(13f));
		gc.gridy = 5; gc.insets = new Insets(6, 0, 4, 0);
		p.add(backBtn, gc);

		msgLabel.setPreferredSize(new Dimension(280, 36));
		gc.gridy = 6; gc.insets = new Insets(4, 0, 0, 0);
		p.add(msgLabel, gc);
		return p;
	}

	private void goBack() { MenuFrame.requestBackToMenu(); }

	/** 登录入口：校验 → PlayerStore → 回调 */
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
			case PlayerStore.LOGIN_NEW -> succeed(user, "新账号注册成功！");
			case PlayerStore.LOGIN_OK -> succeed(user, null);
			case PlayerStore.LOGIN_WRONG -> fail("密码错误（该用户名已被注册）");
			default -> fail("本地档案读写失败，请稍后再试");
		}
	}

	private void fail(String text) {
		msgLabel.setText(text);
		msgLabel.setForeground(new Color(0xE58E7E));
	}

	private void succeed(String user, String tip) {
		lastLoginUser = user;
		rankPanel.reload();
		if (onLoginSuccess != null) onLoginSuccess.run();
	}

	/** 进登录页前的预填与清理 */
	public void prefill(String user) { userField.setText(user == null ? "" : user); }
	public void clearPassword() { passField.setText(""); msgLabel.setText(" "); }
}
