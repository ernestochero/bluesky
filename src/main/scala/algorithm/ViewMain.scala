package algorithm
import java.awt.{BorderLayout, Dimension, GridLayout, Toolkit}
import java.awt.event._
import org.opencv.core.Core
import javax.swing._
object ViewMain {

  class AlgorithmFrame extends JFrame("AlgorithmBachelorThesis\u2122") {
    val screenSize = Toolkit.getDefaultToolkit.getScreenSize
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    this.setLayout(new BorderLayout)
    /* collocate fullscreen size*/
    this.setSize(1300,1000)
    this.setLocationRelativeTo(null)

    /* here add menu of application */
    val mainMenuBar = new JMenuBar()
    val fileMenu = new JMenu("File")
    val openMenuItem = new JMenuItem("open...")
    openMenuItem.addActionListener(new ActionListener {
      override def actionPerformed(actionEvent: ActionEvent): Unit = {
        val fc = new JFileChooser()
        if( fc.showOpenDialog(AlgorithmFrame.this) == JFileChooser.APPROVE_OPTION) canvas.loadFile(fc.getSelectedFile.getPath)
      }
    })
    fileMenu.add(openMenuItem)

    val exitMenuItem = new JMenuItem("Exit")
    exitMenuItem.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        sys.exit(0)
      }
    })
    fileMenu.add(exitMenuItem)
    mainMenuBar.add(fileMenu)

    val helpMenu = new JMenu("Help")
    val aboutMenuItem = new JMenuItem("About")
    aboutMenuItem.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        JOptionPane.showMessageDialog(null, "AlgorithmBachelorThesis - Ernesto Chero, 2018")
      }
    })
    helpMenu.add(aboutMenuItem)
    mainMenuBar.add(helpMenu)
    setJMenuBar(mainMenuBar)

    /* here add control panel on where we put the operations */
    val operationsPanel = new JPanel
    operationsPanel.setSize(new Dimension(operationsPanel.getWidth, operationsPanel.getHeight))
    operationsPanel.setBorder(BorderFactory.createEtchedBorder(border.EtchedBorder.LOWERED))
    operationsPanel.setLayout(new BorderLayout)
    this.add(operationsPanel,BorderLayout.EAST)

    val controls = new JPanel
    controls.setLayout(new GridLayout(0, 2))
    operationsPanel.add(controls, BorderLayout.NORTH)

    val execute = new JLabel("execute")
    controls.add(execute)

    val executeOpButton = new JButton("execute")
    executeOpButton.addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = {
        canvas.applyGrayScaleOperation()
      }
    })
    controls.add(executeOpButton)

    val canvas = new PhotoCanvas
    val scrollPane = new JScrollPane(canvas)
    this.add(scrollPane,BorderLayout.CENTER)
    setVisible(true)
  }

  try
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
  catch {
    case _: Exception => println("Cannot set look and feel, using the default one")
  }

  val frame = new AlgorithmFrame

  def main(args: Array[String]): Unit = {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    println(s"Welcome to OpenCv ${Core.VERSION}")
    frame.repaint()
  }

}


/*
*
* import org.opencv.core.{Core, CvType, Mat, Scalar}

object Main extends App {

  override def main(args:Array[String]) = {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    println(s"Welcome to OpenCv ${Core.VERSION}")
    val mat = Mat.eye(3, 3, CvType.CV_8UC1)
    println(mat.`type`())
  }

}
*
* */