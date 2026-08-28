import javax.swing.JFrame;

public class Msnake {

	public static void main(String[] args) {
		// 入口：主菜单（↑/↓ 选择，空格确认，Esc 返回）
		// 1.开始游戏 → 登录后开一局  2.登录查看排名 → 排行榜浏览  3.设置 → 音乐/速度/边界
		new MenuFrame().setVisible(true);
	}

}
