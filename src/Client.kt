import java.awt.BorderLayout
import java.awt.Color
import java.awt.GridLayout
import java.awt.Toolkit
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.*

import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.border.TitledBorder

class Client {
    lateinit var me :User

    private val frame = JFrame("客户端")
    private val listModel = DefaultListModel<String>()
    private val userList = JList(listModel)

    private val textArea = JTextArea()
    private val textField = JTextField()
    private val txtPort = JTextField(DEFAULT.PORT.toString())
    private val txtHostIp = JTextField(DEFAULT.IP)
    private val txtName = JTextField("用户名")
    private val btnStart = JButton()
    private val btnStop = JButton()
    private val btnSend = JButton("      ")
    private val northPanel = JPanel()
    private val southPanel = JPanel(BorderLayout())
    private val rightScroll = JScrollPane(textArea)
    private val leftScroll = JScrollPane(userList)
    private val centerSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll)

    private var isConnected = false
    private lateinit var socket: Socket
    private lateinit var writer: PrintWriter
    private lateinit var reader: BufferedReader
    private var messageThread: MessageThread? = null// 负责接收消息的线程
    private val onLineUsers = HashMap<String, User>()// 所有在线用户

    init {
        textArea.isEditable = false
        textArea.foreground = Color.blue

        btnStart.background = Color.GREEN
        btnStop.background = Color.RED.darker()
        btnSend.background = Color.BLUE.brighter()

        with(northPanel) {
            layout = GridLayout(1, 7)
            add(JLabel("端口"))
            add(txtPort)
            add(JLabel("服务器IP"))
            add(txtHostIp)
            add(JLabel("姓名"))
            add(txtName)
            add(btnStart)
            add(btnStop)
            border = TitledBorder("连接信息")
        }

        rightScroll.border = TitledBorder("消息显示区")
        leftScroll.border = TitledBorder("在线用户")

        southPanel.add(textField, "Center")
        southPanel.add(btnSend, "East")
        southPanel.border = TitledBorder("写消息")
        centerSplit.dividerLocation = 100

        with(frame) {
            layout = BorderLayout()
            add(northPanel, "North")
            add(centerSplit, "Center")
            add(southPanel, "South")
            setSize(800, 600)
            val screen = Toolkit.getDefaultToolkit().screenSize
            frame.setLocation((screen.width - frame.width) / 3,
                    (screen.width - frame.height) / 3)
            frame.isVisible = true
        }

        // 写消息的文本框中按回车键时事件
        textField.addActionListener { send() }

        // 单击发送按钮时事件
        btnSend.addActionListener { send() }

        // 单击连接按钮时事件
        btnStart.addActionListener(ActionListener {
            val port: Int
            if (isConnected) {
                JOptionPane.showMessageDialog(frame, "已处于连接上状态，不要重复连接!",
                        "Error", JOptionPane.ERROR_MESSAGE)
                return@ActionListener
            }
            try {
                try {
                    port = txtPort.text.trim().toInt()
                } catch (e2: NumberFormatException) {
                    throw Exception("端口号不符合要求")
                }

                val hostIp = txtHostIp.text.trim()
                val name = txtName.text.trim()
                if (name == "" || hostIp == "") {
                    throw Exception("姓名、服务器IP不能为空!")
                }
                me = User(name, hostIp)//创建用户
                User.createUser()
                if (!connectServer(port, me)) {
                    throw Exception("与服务器连接失败!")
                }

                JOptionPane.showMessageDialog(frame, "成功连接!")
            } catch (exc: Exception) {
                JOptionPane.showMessageDialog(frame, exc.message,
                        "Error", JOptionPane.ERROR_MESSAGE)
            }
        })

        // 单击断开按钮时事件
        btnStop.addActionListener(ActionListener {
            if (!isConnected) {
                JOptionPane.showMessageDialog(frame, "已处于断开状态，不要重复断开!",
                        "Error", JOptionPane.ERROR_MESSAGE)
                return@ActionListener
            }
            try {
                if (!closeConnection()) { // 如果断开连接不成功
                    throw Exception("断开连接发生异常！")
                }
                JOptionPane.showMessageDialog(frame, "成功断开!")
            } catch (exc: Exception) {
                JOptionPane.showMessageDialog(frame, exc.message,
                        "Error", JOptionPane.ERROR_MESSAGE)
            }
        })

        // 关闭窗口时事件
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                if (isConnected) {
                    closeConnection()// 关闭连接
                }
                frame.dispose()
            }
        })
    }

    // 向服务器发送指令
    private fun send() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(frame, "还没有连接服务器，无法发送消息！", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        val message: String? = textField.text
        if (message == null || message == "") {
            JOptionPane.showMessageDialog(frame, "消息不能为空！", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        if (message.startsWith(DEFAULT.DELIM)){
            JOptionPane.showMessageDialog(frame, "不要以${DEFAULT.DELIM}开头", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        val stringTokenizer = StringTokenizer(message, DEFAULT.DELIM)
        var temp = stringTokenizer.nextToken()
        when(temp){
            COMMAND.HELP ->{
                sendMessage(me.name + DEFAULT.DELIM + "HELP" + DEFAULT.DELIM + " ")
            }
            COMMAND.TO ->{
                var destination = stringTokenizer.nextToken() // destination id
                var message = stringTokenizer.nextToken()
                sendMessage(me.name + DEFAULT.DELIM + "CHAT" + DEFAULT.DELIM + destination + DEFAULT.DELIM + message)
            }
            COMMAND.REGISTER ->{
                sendMessage(me.name + DEFAULT.DELIM + "REGISTER" + DEFAULT.DELIM + " ")
            }
            else -> {
                sendMessage(me.name + DEFAULT.DELIM + "ALL" + DEFAULT.DELIM + message)
            }
        }
        textField.text = null
    }

    private fun sendMessage(message: String) {
        writer.println(message)
        writer.flush()
    }

    private fun connectServer(port: Int, user : User): Boolean {
        try {
            socket = Socket(user.ip, port)// 根据端口号和服务器ip建立连接
            writer = PrintWriter(socket.getOutputStream())
            reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // 发送客户端用户基本信息(用户名和ip地址)
            sendMessage(user.name + DEFAULT.DELIM + socket.localAddress.toString() + DEFAULT.DELIM + user.id)
            // 开启接收消息的线程
            messageThread = MessageThread(reader, textArea)
            messageThread!!.start()
            isConnected = true// 已经连接上了
            return true
        } catch (e: Exception) {
            textArea.append("${Calendar.getInstance().time}\n" +
                    "与端口号为：" + port + ",    IP地址为：${user.ip} 的服务器连接失败!" + "\r\n")
            isConnected = false// 未连接上
            return false
        }
    }

    //客户端主动关闭连接
    @Synchronized
    fun closeConnection(): Boolean {
        try {
            sendMessage("CLOSE")// 发送断开连接命令给服务器
            messageThread!!.stop()// 停止接受消息线程
            // 释放资源
            reader.close()
            writer.close()
            socket.close()
            isConnected = false
        } catch (e1: IOException) {
            e1.printStackTrace()
            isConnected = true
        }
        return isConnected
    }

    internal inner class MessageThread(private val reader: BufferedReader?, private val textArea: JTextArea) : Thread() {
        // 被动的关闭连接
        @Synchronized
        @Throws(Exception::class)
        fun closeCon() {
            // 清空用户列表
            listModel.removeAllElements()
            // 被动的关闭连接释放资源
            reader?.close()
            if (writer != null) {
                writer.close()
            }
            if (socket != null) {
                socket.close()
            }
            isConnected = false// 修改状态为断开
        }

        override fun run() {
            var message: String
            while (true) {
                try {
                    message = reader!!.readLine()
                    val stringTokenizer = StringTokenizer(message, DEFAULT.DELIM)
                    if (stringTokenizer.hasMoreTokens()) {
                        val command = stringTokenizer.nextToken()// 服务器的命令
                        when (command) { //处理服务器发回来的指令
                            "CLOSE" -> {
                                textArea.append("服务器已关闭!\r\n")
                                closeCon()// 被动的关闭连接
                                return // 结束线程
                            }
                            "ADD" -> {// 有用户上线更新在线列表
                                val username: String? = stringTokenizer.nextToken()
                                val userIp: String? = stringTokenizer.nextToken()
                                val userId : String? = stringTokenizer.nextToken()
                                if (username != null && userIp != null && userId != null) {
                                    val user = User(username, userIp, userId)
                                    onLineUsers[username] = user
                                    listModel.addElement("$username($userId)")
                                }
                            }
                            "DELETE" -> {// 有用户下线更新在线列表
                                val username = stringTokenizer.nextToken()
                                onLineUsers.remove(username)
                                listModel.removeElement(username)
                            }
                            "USERLIST" -> {// 加载在线用户列表
                                val size = stringTokenizer.nextToken().toInt()
                                var username: String?
                                var userIp: String?
                                var userId : String?
                                for (i in 0 until size) {
                                    username = stringTokenizer.nextToken()
                                    userIp = stringTokenizer.nextToken()
                                    userId = stringTokenizer.nextToken()
                                    val user = User(username!!, userIp!!,userId)
                                    onLineUsers[username] = user
                                    listModel.addElement("$username($userId)")
                                }
                            }
                            "MAX" -> {// 人数已达上限
                                textArea.append(stringTokenizer.nextToken()
                                        + stringTokenizer.nextToken() + "\r\n")
                                closeCon()// 被动的关闭连接
                                JOptionPane.showMessageDialog(frame, "服务器缓冲区已满！", "错误",
                                        JOptionPane.ERROR_MESSAGE)
                                return // 结束线程
                            }
                            else -> textArea.append(message + "\r\n")
                        }
                    } else
                        continue
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }
}