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
	String fx = "R"; // 方向U, D, R, L, //打开程序时 蛇头向右
	String fxed;		//储存上一次刷新的方向
	boolean isStarted = false;		//没开始
	boolean isFailed = false;		//没失败
	Timer timer = new Timer(100, this); // 时钟（多少间隔时间，时间到了找谁处理
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
		timer.start();
		loodBGM();
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
		fx = "R"; // 每次开始游戏 初始化蛇头
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

			if (keyCode == KeyEvent.VK_LEFT && fxed != "R") {
				fx = "L";
			} else if (keyCode == KeyEvent.VK_RIGHT && fxed != "L") {
				fx = "R";
			} else if (keyCode == KeyEvent.VK_UP && fxed != "D") {
				fx = "U";
			} else if (keyCode == KeyEvent.VK_DOWN && fxed != "U") {
				fx = "D";
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
			
			fxed = fx;		//防止蛇往回撞自己
			if(fx=="L"&&fxed=="R"||fx=="U"&&fxed=="D"||fx=="R"&&fxed=="L"||fx=="D"&&fxed=="U") {		
				fx=fxed;
			}
			
			for (int i = len - 1; i > 0; i--) { // 蛇的身体的移动
				snakex[i] = snakex[i - 1];
				snakey[i] = snakey[i - 1];
			}
			
			if (fx == "R") {
				snakex[0] = snakex[0] + 25; // 蛇的头的移动
				if (snakex[0] > 850) {
					snakex[0] = 25;
				}
			} else if (fx == "L") {
				snakex[0] = snakex[0] - 25;
				if (snakex[0] < 25) {
					snakex[0] = 850;
				}
			} else if (fx == "U") {
				snakey[0] = snakey[0] - 25;
				if (snakey[0] < 75) {
					snakey[0] = 650;
				}
			} else if (fx == "D") {
				snakey[0] = snakey[0] + 25;
				if (snakey[0] > 650) {
					snakey[0] = 75;
				}
			}

			if (snakex[0] == foodx && snakey[0] == foody) { // 蛇吃食物，身体变长并刷新食物
				len++;
				score++;
				foodx = 25 + 25 * rand.nextInt(34);
				foody = 75 + 25 * rand.nextInt(24);
			}

			for (int i = 1; i < len; i++) { // 头碰到身体就结束
				if (snakex[i] == snakex[0] && snakey[i] == snakey[0]) {
					isFailed = true;
					isStarted = false;
				}
			}
			
			if(isFailed){		//游戏结束，音乐停止
				stopBGM();
			}
			
			repaint();
			// timer.start(); //调用时钟
		}

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
