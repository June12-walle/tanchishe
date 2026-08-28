import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JLabel;

/**
 * RankPanel — 排行榜面板：按最高分前 10 名，支持鼠标点击与键盘（↑/↓ + 空格）查看玩家详情。
 */
public class RankPanel extends JPanel {

	private final DefaultListModel<String> model = new DefaultListModel<>();
	private final JList<String> list = new JList<>(model);
	private List<PlayerStore.PlayerRecord> cache = List.of();
	private boolean dialogMode = false;   // 主菜单页内的榜单浏览模式（由 MenuFrame 控制）
	private boolean detailOpen = false;   // 详情弹窗是否正在显示（键盘路由用）

	public RankPanel() { this(false); }

	public RankPanel(boolean unused) {
		setLayout(new BorderLayout());
		setBackground(new Color(0x14181F));
		setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

		JLabel head = new JLabel("排行榜 · 按最高分（点击或空格查看玩家详情）");
		head.setFont(head.getFont().deriveFont(Font.BOLD, 15f));
		head.setForeground(new Color(0xC9D6F2));
		add(head, BorderLayout.NORTH);

		list.setBackground(new Color(0x1B212C));
		list.setForeground(new Color(0xE6ECF7));
		list.setSelectionBackground(new Color(0x2F6FED));
		list.setSelectionForeground(Color.WHITE);
		list.setFont(list.getFont().deriveFont(15f));
		list.setFixedCellHeight(30);
		list.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				int i = list.locationToIndex(e.getPoint());
				if (i >= 0 && i < cache.size()) showDetail(cache.get(i));
			}
		});
		add(new JScrollPane(list), BorderLayout.CENTER);
		reload();
	}

	/** 从磁盘重载榜单 */
	public void reload() {
		cache = PlayerStore.topPlayers(10);
		model.clear();
		if (cache.isEmpty()) {
			model.addElement("暂无记录 —— 来玩第一局吧！");
			return;
		}
		int rank = 1;
		for (PlayerStore.PlayerRecord r : cache) {
			model.addElement(rank++ + ".  " + r.user + "   最高 " + r.best + " 分 · " + r.plays + " 局");
		}
	}

	public boolean isEmpty() { return cache.isEmpty(); }

	/** 键盘 ↑/↓ 移动选中行（主菜单页面模式下由 MenuFrame 转发） */
	public void moveSelection(int delta) {
		int n = cache.size();
		if (n == 0) return;
		int i = list.getSelectedIndex();
		i = (i < 0) ? (delta > 0 ? 0 : n - 1) : Math.floorMod(i + delta, n);
		list.setSelectedIndex(i);
		list.ensureIndexIsVisible(i);
	}

	/** 键盘空格/回车：查看当前选中玩家详情 */
	public void openSelected() {
		int i = list.getSelectedIndex();
		if (i >= 0 && i < cache.size()) showDetail(cache.get(i));
	}

	/** 详情弹窗展示（记录打开状态供键盘路由） */
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
		JTextArea area = new JTextArea(sb.toString());
		area.setEditable(false);
		area.setBackground(new Color(0x1B212C));
		area.setForeground(new Color(0xE6ECF7));
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
		detailOpen = true;
		JOptionPane.showMessageDialog(this, new JScrollPane(area),
				"玩家详情 · " + r.user, JOptionPane.PLAIN_MESSAGE);
		detailOpen = false;
	}

	public void setDialogMode(boolean v) { this.dialogMode = v; }
	public boolean isDialogMode() { return dialogMode; }
	public boolean isDetailOpen() { return detailOpen; }

	/** 键盘路由兼容方法：详情弹窗打开时关闭它 */
	public void closeDetail() { /* 详情是模态弹窗，关闭由对话框自身处理；此方法用于状态复位 */ detailOpen = false; }
}
