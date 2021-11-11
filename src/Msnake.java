import javax.swing.JFrame;

public class Msnake {

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setBounds(10, 10, 900, 720);	//窗口大小
		frame.setResizable(false);			//是否可拖动
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//关闭按钮
		frame.add(new MPanel());			//加入MPanel对象

		frame.setVisible(true);				//Visible出现
	}

}
