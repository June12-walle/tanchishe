import javax.swing.JFrame;

public class Msnake {

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setBounds(10, 10, 900, 720);	//���ڴ�С
		frame.setResizable(false);			//�Ƿ���϶�
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//�رհ�ť
		frame.add(new MPanel());			//����MPanel����

		frame.setVisible(true);				//Visible����
	}

}
