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

	ImageIcon title; // ����ͼƬ
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
	String fx = "R"; // ����U, D, R, L, //�򿪳���ʱ ��ͷ����
	String fxed;		//������һ��ˢ�µķ���
	boolean isStarted = false;		//û��ʼ
	boolean isFailed = false;		//ûʧ��
	Timer timer = new Timer(100, this); // ʱ�ӣ����ټ��ʱ�䣬ʱ�䵽����˭����
	int foodx;
	int foody;
	Random rand = new Random();
	
	//��������
	Clip bgm2;

	public MPanel() {
		loodImages();
		initSnake();
		this.setFocusable(true); // ���Ի�ȡ���㣨�����¼�
		this.addKeyListener(this); // �Լ����������¼�
		timer.start();
		loodBGM();
	}

	public void paintComponent(Graphics g) { // ����
		super.paintComponent(g);
		this.setBackground(Color.white); // ������ɫ
		title.paintIcon(this, g, 25, 11); // ���⣨�����ģ����ʣ�x���꣬Y����

		g.fillRect(25, 75, 850, 600); // ��Ϸ����� ��x,y,��ȣ��߶�

		g.setColor(Color.WHITE);
		g.drawString("����:" + len, 750, 35);
		g.drawString("����:" + score, 750, 50);

		if (fx == "R") { // ����ͷ�ķ��򣬻�
			right.paintIcon(this, g, snakex[0], snakey[0]);
		} else if (fx == "L") {
			left.paintIcon(this, g, snakex[0], snakey[0]);
		} else if (fx == "U") {
			up.paintIcon(this, g, snakex[0], snakey[0]);
		} else if (fx == "D") {
			down.paintIcon(this, g, snakex[0], snakey[0]);
		}
		for (int i = 1; i < len; i++) { // ���� �ߵ�����
			body.paintIcon(this, g, snakex[i], snakey[i]);
		}

		food.paintIcon(this, g, foodx, foody); // ���� ʳ��

		if (isStarted == false && isFailed == false) { // ���� ��ʼ������(û��ʼ ���� ûʧ��
			g.setColor(Color.white);
			g.setFont(new Font("΢���ź�", Font.BOLD, 40));// ����������������ʽ�������С��
			g.drawString("���ո�ʼ��Ϸ", 300, 350);// Press Space to Start
		}
		if (isFailed == true) { // ������Ϸ������
			g.setColor(Color.red);
			g.setFont(new Font("΢���ź�", Font.BOLD, 40));
//			Font f = g.getFont();//����ԭ����ʽ
			g.drawString("��Ϸ���������ո����¿�ʼ��Ϸ", 150, 350);// Failed: Press Space to Restart
		}
	}

	public void initSnake() { // ÿ�ο�ʼ��Ϸ���ߵ����ݳ�ʼ��
		fx = "R"; // ÿ�ο�ʼ��Ϸ ��ʼ����ͷ
		len = 3;
		snakex[0] = 100;
		snakey[0] = 100;
		snakex[1] = 75;
		snakey[1] = 100;
		snakex[2] = 50;
		snakey[2] = 100;
		foodx = 25 + 25 * rand.nextInt(34); // �������0~33
		foody = 75 + 25 * rand.nextInt(24);
	}

	@Override
	public void keyTyped(KeyEvent e) { // �ü��ˣ�Ҫ��ʲô
		// TODO Auto-generated method stub

	}

	@Override
	public void keyPressed(KeyEvent e) { // �õ��ף�
		int keyCode = e.getKeyCode(); // ��ȡ ��������

		if (keyCode == KeyEvent.VK_SPACE) { // ��������� �ո�
			isStarted = !isStarted; // ȡ��
			repaint(); // �ػ�
			
			if(isStarted) {
				playBGM();
			}else {stopBGM();}
			
			if (isFailed) { // ���ո����¿�ʼ
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
	public void keyReleased(KeyEvent e) { // ̧�����ˣ�Ҫ��ʲô
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent e) { // ���ݴ����仯����---ʱ��ʱ�䵽�˾͵����������
		if (isStarted && !isFailed) { // ʵ�� ���¿ո�ʼ��ͣ
			
			fxed = fx;		//��ֹ������ײ�Լ�
			if(fx=="L"&&fxed=="R"||fx=="U"&&fxed=="D"||fx=="R"&&fxed=="L"||fx=="D"&&fxed=="U") {		
				fx=fxed;
			}
			
			for (int i = len - 1; i > 0; i--) { // �ߵ�������ƶ�
				snakex[i] = snakex[i - 1];
				snakey[i] = snakey[i - 1];
			}
			
			if (fx == "R") {
				snakex[0] = snakex[0] + 25; // �ߵ�ͷ���ƶ�
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

			if (snakex[0] == foodx && snakey[0] == foody) { // �߳�ʳ�����䳤��ˢ��ʳ��
				len++;
				score++;
				foodx = 25 + 25 * rand.nextInt(34);
				foody = 75 + 25 * rand.nextInt(24);
			}

			for (int i = 1; i < len; i++) { // ͷ��������ͽ���
				if (snakex[i] == snakex[0] && snakey[i] == snakey[0]) {
					isFailed = true;
					isStarted = false;
				}
			}
			
			if(isFailed){		//��Ϸ����������ֹͣ
				stopBGM();
			}
			
			repaint();
			// timer.start(); //����ʱ��
		}

	}
	private void loodBGM() {			//����BGM
		try {
			bgm2 = AudioSystem.getClip();
			InputStream is = this.getClass().getClassLoader().getResourceAsStream("sound/bgm2.wav");
			AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is));		//BufferedInputStream�ܼ���һ�������ݽ��뻺������������
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
		//bgm.start();		/*����һ��*/
		bgm2.loop(Clip.LOOP_CONTINUOUSLY);	/*ѭ������*/
	}
	
	private void stopBGM() {
		bgm2.stop();
	}
	
	private void loodImages() {		// ����ͼƬ
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
