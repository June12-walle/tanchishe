import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

public class MPanel extends JPanel implements KeyListener, ActionListener {

	ImageIcon title; // 导入图片
	ImageIcon body;
	ImageIcon up;
	ImageIcon down;
	ImageIcon left;
	ImageIcon right;
	ImageIcon food;

	int len = 3;
	int score = 0;
	int[] snakex = new int[750];
	int[] snakey = new int[750];
	String fx = "R"; // 当前执行方向（U/D/R/L），只能由时钟拍从队列消费
	final Deque<String> dirQueue = new ArrayDeque<>(); // 按键方向队列：每拍最多消费一个，入队时校验反向
	boolean isStarted = false;		//没开始
	boolean isFailed = false;		//没失败
	String currentPlayer; // 当前登录玩家（null 表示无档案模式）
	Runnable exitCallback; // 游戏窗口关闭后回到主菜单的回调
	boolean wrapWalls = GameSettings.get().wrapWalls; // 边界模式快照（开局时生效）
	Timer timer; // 间隔在构造器里按设置创建
	int foodx;
	int foody;
	Random rand = new Random();
	
	//背景音乐
	Clip bgm2;

	public MPanel() {
		loodImages();
		initSnake();
		this.setFocusable(true); // 可以获取焦点（键盘事件
		this.addKeyListener(this); // 自己监听键盘事件
		timer = new Timer(GameSettings.get().tickMs, this); // 速度设置在开局时读取
		timer.start();
		loodBGM();
	}

	public MPanel(String player) { // 带玩家名的入口：死亡时自动记录成绩
		this();
		this.currentPlayer = player;
	}

	public void paintComponent(Graphics g) { // 画笔
		super.paintComponent(g);
		this.setBackground(Color.white); // 背景颜色
		title.paintIcon(this, g, 25, 11); // 标题（画在哪，画笔，x坐标，Y坐标

		g.fillRect(25, 75, 850, 600); // 游戏活动区域 （x,y,宽度，高度

		g.setColor(Color.WHITE);
		g.drawString("长度:" + len, 750, 35);
		g.drawString("分数:" + score, 750, 50);

		if (fx == "R") { // 按蛇头的方向，画
			right.paintIcon(this, g, snakex[0], snakey[0]);
		} else if (fx == "L") {
			left.paintIcon(this, g, snakex[0], snakey[0]);
		} else if (fx == "U") {
			up.paintIcon(this, g, snakex[0], snakey[0]);
		} else if (fx == "D") {
			down.paintIcon(this, g, snakex[0], snakey[0]);
		}
		for (int i = 1; i < len; i++) { // 画出 蛇的身体
			body.paintIcon(this, g, snakex[i], snakey[i]);
		}

		food.paintIcon(this, g, foodx, foody); // 画出 食物

		if (isStarted == false && isFailed == false) { // 画出 开始的文字(没开始 并且 没失败
			g.setColor(Color.white);
			g.setFont(new Font("微软雅黑", Font.BOLD, 40));// （字体名、字体样式、字体大小。
			g.drawString("按空格开始游戏", 300, 350);// Press Space to Start
		}
		if (isFailed == true) { // 重新游戏的文字
			g.setColor(Color.red);
			g.setFont(new Font("微软雅黑", Font.BOLD, 40));
//			Font f = g.getFont();//保持原来格式
			g.drawString("游戏结束，按空格重新开始游戏", 150, 350);// Failed: Press Space to Restart
		}
	}

	public void initSnake() { // 每次开始游戏，蛇的数据初始化
		fx = "R"; // 初始化蛇头朝右
		dirQueue.clear(); // 清空上一局残留的按键队列
		len = 3;
		snakex[0] = 100;
		snakey[0] = 100;
		snakex[1] = 75;
		snakey[1] = 100;
		snakex[2] = 50;
		snakey[2] = 100;
		foodx = 25 + 25 * rand.nextInt(34); // （随机数0~33
		foody = 75 + 25 * rand.nextInt(24);
	}

	@Override
	public void keyTyped(KeyEvent e) { // 敲键了，要做什么
		// TODO Auto-generated method stub

	}

	@Override
	public void keyPressed(KeyEvent e) { // 敲到底，
		int keyCode = e.getKeyCode(); // 获取 键的数字

		if (keyCode == KeyEvent.VK_SPACE) { // 如果按的是 空格
			isStarted = !isStarted; // 取反
			repaint(); // 重画
			
			if(isStarted) {
				playBGM();
			}else {stopBGM();}
			
			if (isFailed) { // 按空格重新开始
				isFailed = false;
				initSnake();
			}
		}
		if (isFailed == false && isStarted == true) {

			// 方向键不再直接改 fx，而是进入队列（每拍消费一个，禁止相对"最近已接受方向"的回转）
			switch (keyCode) {
				case KeyEvent.VK_LEFT -> tryQueue("L");
				case KeyEvent.VK_RIGHT -> tryQueue("R");
				case KeyEvent.VK_UP -> tryQueue("U");
				case KeyEvent.VK_DOWN -> tryQueue("D");
			}
			repaint();
		}
//		if(keyCode == KeyEvent.VK_S) {
//			
//		}
	}

	@Override
	public void keyReleased(KeyEvent e) { // 抬起来了，要做什么
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent e) { // 数据处理（变化），---时钟时间到了就调用这个方法
		if (isStarted && !isFailed) { // 实现 按下空格开始暂停
			
			if (!dirQueue.isEmpty()) fx = dirQueue.poll(); // 本拍最多接受一次转向
			int tailX = snakex[len - 1]; // 记录本拍即将收回的尾巴位置
			int tailY = snakey[len - 1];
			for (int i = len - 1; i > 0; i--) { // 蛇的身体的移动
				snakex[i] = snakex[i - 1];
				snakey[i] = snakey[i - 1];
			}
			
			if (fx == "R") {
				snakex[0] = snakex[0] + 25; // 蛇的头的移动
				if (snakex[0] > 850) {
					if (wrapWalls) snakex[0] = 25; else { isFailed = true; isStarted = false; stopBGM(); }
				}
			} else if (fx == "L") {
				snakex[0] = snakex[0] - 25;
				if (snakex[0] < 25) {
					if (wrapWalls) snakex[0] = 850; else { isFailed = true; isStarted = false; stopBGM(); }
				}
			} else if (fx == "U") {
				snakey[0] = snakey[0] - 25;
				if (snakey[0] < 75) {
					if (wrapWalls) snakey[0] = 650; else { isFailed = true; isStarted = false; stopBGM(); }
				}
			} else if (fx == "D") {
				snakey[0] = snakey[0] + 25;
				if (snakey[0] > 650) {
					if (wrapWalls) snakey[0] = 75; else { isFailed = true; isStarted = false; stopBGM(); }
				}
			}

			if (snakex[0] == foodx && snakey[0] == foody) { // 蛇吃食物：身体变长且尾巴原地保留，刷新食物并避开蛇身
				score++;
				if (len < snakex.length) { // 容量保护：达到数组上限时只加分不再变长
					len++;
					snakex[len - 1] = tailX; // 恢复刚被收缩掉的尾巴，蛇整体加长一格
					snakey[len - 1] = tailY;
				}
				do { // 重新生成食物，避开蛇身所有格子
					foodx = 25 + 25 * rand.nextInt(34);
					foody = 75 + 25 * rand.nextInt(24);
				} while (onSnake(foodx, foody));
			}

			for (int i = 1; i < len; i++) { // 头碰到身体就结束
				if (snakex[i] == snakex[0] && snakey[i] == snakey[0]) {
					isFailed = true;
					isStarted = false;
				}
			}

			if (currentPlayer != null && score > 0) { // 游戏结束立即把本局成绩写入本地档案
				PlayerStore.recordScore(currentPlayer, score);
			}
			
			if(isFailed){		//游戏结束，音乐停止
				stopBGM();
			}
			
			repaint();
			// timer.start(); //调用时钟
		}

	}
	private void tryQueue(String cand) { // 校验并接收一个方向按键
		if (dirQueue.size() >= 2) return; // 最多缓存两拍转向
		String last = dirQueue.isEmpty() ? fx : dirQueue.peekLast();
		if (cand.equals(last) || cand.equals(opposite(last))) return;
		dirQueue.offer(cand);
	}

	private static String opposite(String d) { // 相反方向
		return switch (d) {
			case "L" -> "R";
			case "R" -> "L";
			case "U" -> "D";
			default -> "U";
		};
	}

	public void setExitCallback(Runnable r) { this.exitCallback = r; }

	private boolean onSnake(int x, int y) { // 判断某格子是否被蛇身占用
		for (int i = 0; i < len; i++) {
			if (snakex[i] == x && snakey[i] == y) {
				return true;
			}
		}
		return false;
	}

	private void loodBGM() {			//加载BGM
		try {
			bgm2 = AudioSystem.getClip();
			InputStream is = this.getClass().getClassLoader().getResourceAsStream("sound/bgm2.wav");
			AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is));		//BufferedInputStream能加载一部分数据进入缓存区，更流畅
			bgm2.open(ais);
			
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void playBGM() {
		//bgm.start();		/*播放一次*/
		bgm2.loop(Clip.LOOP_CONTINUOUSLY);	/*循环播放*/
	}
	
	private void stopBGM() {
		bgm2.stop();
	}
	
	private void loodImages() {		// 加载图片
		InputStream is;
		try {
			is = getClass().getClassLoader().getResourceAsStream("images/title.jpg");
			title = new ImageIcon(ImageIO.read(is));
			
			is = getClass().getClassLoader().getResourceAsStream("images/body.png");
			body = new ImageIcon(ImageIO.read(is));
			
			is = getClass().getClassLoader().getResourceAsStream("images/up.png");
			up = new ImageIcon(ImageIO.read(is));
			
			is = getClass().getClassLoader().getResourceAsStream("images/down.png");
			down = new ImageIcon(ImageIO.read(is));
			
			is = getClass().getClassLoader().getResourceAsStream("images/left.png");
			left = new ImageIcon(ImageIO.read(is));
			
			is = getClass().getClassLoader().getResourceAsStream("images/right.png");
			right = new ImageIcon(ImageIO.read(is));
			
			is = getClass().getClassLoader().getResourceAsStream("images/food.png");
			food = new ImageIcon(ImageIO.read(is));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}
