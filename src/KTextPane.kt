import javax.swing.*
import javax.swing.text.*
import java.awt.*


class KTextPane() : JTextPane() {
    private val imageIcon = ImageIcon("C:\\Users\\asus\\Desktop\\Hachat\\src\\background.jpg")
    private val image = imageIcon.image
    val styleContext = StyleContext()
    val calander: Style = styleContext.addStyle("time", null)
    val chatting: Style = styleContext.addStyle("chatting", null)
    val friend: Style = styleContext.addStyle("new friend", null)

    init {
        isEditable = false
        foreground = Color.blue
        isOpaque = false

        with(calander) {
            addAttribute(StyleConstants.Foreground, Color.RED)
            addAttribute(StyleConstants.FontSize, 14)
            addAttribute(StyleConstants.FontFamily, "Rockwell")
            addAttribute(StyleConstants.Italic, false)
        }

        with(chatting) {
                addAttribute(StyleConstants.Foreground, Color.BLACK)
            addAttribute(StyleConstants.FontSize, 16)
            addAttribute(StyleConstants.FontFamily, "等线")
            addAttribute(StyleConstants.Italic, false)
        }

        with(friend) {
            addAttribute(StyleConstants.Foreground, Color.DARK_GRAY)
            addAttribute(StyleConstants.FontSize, 14)
            addAttribute(StyleConstants.FontFamily, "微软雅黑")
            addAttribute(StyleConstants.Italic, true)
        }

    }

    override fun paint(g: Graphics?) {
//        g!!.drawImage(image,0,0,this)
        /*g!!.drawString(DEFAULT.MANUAL,width -150, 50)*/
        super.paint(g)
    }
}