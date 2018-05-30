import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;


class KTextPane() : JTextPane(){
    private val imageIcon = ImageIcon("C:\\Users\\asus\\Desktop\\Hachat\\src\\background.jpg")
    private val image = imageIcon.image
    val styleContext = StyleContext()
    val style : Style = styleContext.addStyle("time",null)

    init {
        isEditable = false
        foreground = Color.blue
        isOpaque = false

        style.addAttribute(StyleConstants.Foreground, Color.red);
        style.addAttribute(StyleConstants.FontSize, 16);
        style.addAttribute(StyleConstants.FontFamily, "serif");
        style.addAttribute(StyleConstants.Bold, true);
    }

    override fun paint(g: Graphics?) {
//        g!!.drawImage(image,0,0,this)
        /*g!!.drawString(DEFAULT.MANUAL,width -150, 50)*/
        super.paint(g)
    }
}