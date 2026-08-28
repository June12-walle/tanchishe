import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * LoginFrame — 登录窗口：左侧登录表单，右侧排行榜面板。
 * 通过回调 onSuccess 通知调用方；wasSuccessful() 供关闭兜底判断。
 * lastLoginUser 记录最近一次登录成功的用户名。
 */
public class LoginFrame extends JFrame {

	public static String lastLoginUser = null;

	private final JTextField userField = new JTextField(14);
	private final JPasswordField passField = new JPasswordField(14);
	private final JLabel msgLabel = new JLabel(" ");
	private final JButton loginBtn = new JButton("登录 / 注册");
	private final JButton backBtn = new JButton("← 返回");

	private final RankPanel rankPanel;
	private final Runnable onSuccess;
	private boolean success = false;

	public LoginFrame(Runnable onSuccess) {
		this.onSuccess = onSuccess;
		this.rankPanel = new RankPanel();

		setTitle("贪吃蛇 · 登录");
		setSize(780, 460);
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel root = new JPanel(new BorderLayout(16, 0));
		root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
		root.add(buildLoginForm(), BorderLayout.WEST);
		root.add(rankPanel, BorderLayout.CENTER);
		setContentPane(root);

		userField.addActionListener(e -> doLogin());
		passField.addActionListener(e -> doLogin());
		loginBtn.addActionListener(e -> doLogin());
		backBtn.addActionListener(e -> dispose());
	}

	/** 左侧登录表单 */
	private JPanel buildLoginForm() {
		JPanel p = new JPanel(new GridBagLayout());
		p.setPreferredSize(new Dimension(300, 320));
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
		gc.anchor = GridBagConstraints.WEST;
		gc.insets = new Insets(0, 0, 14, 0);

		JLabel title = new JLabel("欢迎来到贪吃蛇");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 19f));
		p.add(title, gc);

		JLabel tips = new JLabel("<html>已有账号直接登录；<br>输入新用户名将自动注册。</html>");
		tips.setForeground(Color.GRAY);
		gc.gridy = 1; gc.insets = new Insets(4, 0, 8, 0);
		p.add(tips, gc);

		gc.gridwidth = 1; gc.insets = new Insets(4, 0, 4, 8);
		p.add(new JLabel("用户名:"), gc);
		gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1;
		p.add(userField, gc);

		gc.gridx = 0; gc.gridy = 3; gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
		p.add(new JLabel("密  码:"), gc);
		gc.gridx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1;
		p.add(passField, gc);

		gc.gridx = 0; gc.gridy = 4; gc.gridwidth = 2; gc.insets = new Insets(14, 0, 4, 0);
		gc.fill = GridBagConstraints.HORIZONTAL;
		loginBtn.setFont(loginBtn.getFont().deriveFont(Font.BOLD, 14f));
		p.add(loginBtn, gc);

		backBtn.setFont(backBtn.getFont().deriveFont(13f));
		gc.gridy = 5; gc.insets = new Insets(6, 0, 4, 0);
		p.add(backBtn, gc);

		msgLabel.setForeground(new Color(0xC0392B));
		msgLabel.setPreferredSize(new Dimension(280, 36));
		gc.gridy = 6; gc.insets = new Insets(4, 0, 0, 0);
		p.add(msgLabel, gc);
		return p;
	}

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
		msgLabel.setForeground(new Color(0xC0392B));
	}

	private void succeed(String user, String tip) {
		lastLoginUser = user;
		success = true;
		if (tip != null) {
			msgLabel.setText(tip);
			msgLabel.setForeground(new Color(0x1A9E6E));
		}
		rankPanel.reload();
		if (onSuccess != null) onSuccess.run();
		dispose();
	}

	public boolean wasSuccessful() { return success; }
}
