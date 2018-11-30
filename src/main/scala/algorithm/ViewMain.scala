package algorithm
import java.awt._
import java.awt.event._

import org.opencv.core.Core
import javax.swing._
import javax.swing.border.EmptyBorder
object ViewMain {

  class AlgorithmFrame extends JFrame("AlgorithmBachelorThesis\u2122") {
    val screenSize = Toolkit.getDefaultToolkit.getScreenSize
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    this.setLayout(new BorderLayout)
    /* collocate fullscreen size*/
    this.setSize(1280,710)
    this.setLocationRelativeTo(null)

    /* here add menu of application */
    val mainMenuBar = new JMenuBar()
    val fileMenu = new JMenu("File")
    val openMenuItem = new JMenuItem("open...")
    openMenuItem.addActionListener(new ActionListener {
      override def actionPerformed(actionEvent: ActionEvent): Unit = {
        val fc = new JFileChooser("/home/ernesto/Documents/imagesTheshis/examScanned/")
        val result = fc.showOpenDialog(AlgorithmFrame.this)
        if( result == JFileChooser.APPROVE_OPTION) { canvas.loadFile(fc.getSelectedFile.getPath) }
        /*
        * else if (result == JFileChooser.CANCEL_OPTION){
        }
        * */
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
    controls.setLayout(new GridLayout(0, 3, 10 , 30))
    controls.setBorder(new EmptyBorder(10, 10, 10, 10))
    operationsPanel.add(controls, BorderLayout.NORTH)


   /* val controlsBottom = new JPanel
    controls.setLayout(new GridLayout(2, 1, 10 , 10))
    controls.setBorder(new EmptyBorder(10, 10, 10, 10))
    operationsPanel.add(controlsBottom, BorderLayout.SOUTH)
*/
    /* here add component to left panel*/
    // upload exam pattern
    val uploadPatternLabel = new JLabel("Upload Pattern : ")
    uploadPatternLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK))
    controls.add(uploadPatternLabel)

    val uploadPatternButton = new JButton("upload")
    uploadPatternButton.addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = {
        val fc = new JFileChooser("/home/ernesto/Documents/imagesTheshis/examScanned/")
        if( fc.showOpenDialog(AlgorithmFrame.this) == JFileChooser.APPROVE_OPTION) canvas.uploadPattern(fc.getSelectedFile.getPath)
        uploadPatternIcon.setText(" successfully ")
      }
    })
    uploadPatternButton.setForeground(Color.BLUE)
    uploadPatternButton.setToolTipText("exam pattern : It's used to evaluate the others exams")
    controls.add(uploadPatternButton)

    val uploadPatternIcon = new JLabel(" := empty pattern ")
    uploadPatternIcon.setBorder(BorderFactory.createLineBorder(Color.BLACK))
    controls.add(uploadPatternIcon)


    // upload exams
    val uploadExams = new JLabel("Upload Exams : ")
    uploadExams.setBorder(BorderFactory.createLineBorder(Color.BLACK))
    controls.add(uploadExams)

    val uploadExamsButton = new JButton("upload")
    uploadExamsButton.addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = {
        val fc = new JFileChooser("/home/ernesto/Documents/imagesTheshis/")
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        fc.setAcceptAllFileFilterUsed(false)
        fc.setMultiSelectionEnabled(true)
        if( fc.showOpenDialog(AlgorithmFrame.this) == JFileChooser.APPROVE_OPTION) {
          val paths = fc.getSelectedFile.listFiles().map(_.getPath)
          canvas.uploadExams(paths)
          uploadExamsIcon.setText(s" ${paths.length} Exams Uploaded")
        }
      }
    })
    uploadExamsButton.setForeground(Color.BLUE)
    controls.add(uploadExamsButton)


    val uploadExamsIcon = new JLabel(" 0 Exams ")
    uploadExamsIcon.setBorder(new EmptyBorder(5,5,5,5))
    controls.add(uploadExamsIcon)

    // results
    val line1 = new JLabel(" ------------------ ")
    controls.add(line1)

    val qualifyButton = new JButton("qualify")
    qualifyButton.addActionListener(e => canvas.applyQualify())
    controls.add(qualifyButton)

    val line2 = new JLabel(" ------------------ ")
    controls.add(line2)

    val rotateOpButton = new JButton("apply rotate")
    rotateOpButton.addActionListener(new ActionListener {
      override def actionPerformed(actionEvent: ActionEvent): Unit = canvas.applyRotateOperation()
    })
    controls.add(rotateOpButton)

    val qualifyOpButton = new JButton("apply qualify")
    qualifyOpButton.addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit = {
        canvas.applyGrayScaleOperation()
      }
    })
    controls.add(qualifyOpButton)



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