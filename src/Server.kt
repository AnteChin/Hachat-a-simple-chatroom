import java.awt.*
import java.awt.event.WindowEvent
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import javax.swing.*
import javax.swing.JList
import javax.swing.DefaultListModel
import javax.swing.border.TitledBorder
import java.awt.event.WindowAdapter
import java.util.*
import javax.swing.JOptionPane
import kotlin.collections.ArrayList
import java.io.IOException
import java.io.InputStreamReader
import java.net.BindException
import java.util.StringTokenizer

class Server {
    private var frame = JFrame("Server")
    var contentArea = JTextArea()
    private var txtMessage = JTextField()
    private var txtMaxUserNumber = JTextField(DEFAULT.USER_COUNT.toString())
    private var txtPort = JTextField(DEFAULT.PORT.toString())
    private var btnStart = JButton("开始")
    private var btnStop = JButton("终止")
    private var btnSend = JButton("发送")
    private var listModel = DefaultListModel<String>()  //用户列表
    private var userList = JList(listModel)
    private var southPanel = JPanel(BorderLayout())
    private var leftPanel = JScrollPane(userList)
    private var rightPanel = JScrollPane(contentArea)
    private var centerSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel)
    private var northPanel = JPanel()

    private lateinit var serverSocket: ServerSocket
    private lateinit var serverThread: ServerThread
    private var clients: ArrayList<ClientThread> = java.util.ArrayList() //装在用户线程的数组

    var isStart = false

    init {
        contentArea.isEditable = false
        contentArea.foreground = Color.BLUE

        btnStart.background = Color.GREEN
        btnStop.isEnabled = false
        btnStop.background = Color.RED.darker()
        btnSend.background = Color.BLUE.brighter().brighter()

        southPanel.border = TitledBorder("写消息")
        southPanel.add(txtMessage, "Center")
        southPanel.add(btnSend, "East")

        leftPanel.border = TitledBorder("在线用户")
        rightPanel.border = TitledBorder("消息")

        centerSplit.dividerLocation = 100

        with(northPanel) {
            layout = GridLayout(1, 6)
            add(JLabel("人数上限"))
            add(txtMaxUserNumber)
            add(JLabel("端口"))
            add(txtPort)
            add(btnStart)
            add(btnStop)
            border = TitledBorder("配置信息")
        }

        with(frame) {
            layout = BorderLayout()
            add(northPanel, "North")
            add(centerSplit, "Center")
            add(southPanel, "South")
            setSize(800, 600)
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            setLocation((screenSize.width - frame.width) / 2, (screenSize.height - frame.height) / 2)
            isVisible = true

            //happened when server is closed
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    if (isStart) {
                        closeServer()
                    }
                    System.exit(0)
                }
            })
        }

        txtMessage.addActionListener { send() } // Action when push enter
        btnSend.addActionListener { send() }

        //启动开关的监听器
        btnStart.addActionListener {
            if (isStart) {
                JOptionPane.showMessageDialog(frame, "服务器已启动,请勿重复", "Error", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            val max: Int
            val port: Int
            try {
                try {
                    max = txtMaxUserNumber.text.toInt()
                } catch (e1: Exception) {
                    throw Exception("请在用户人数框内输入一个正整数")
                }
                if (max <= 0) {
                    throw Exception("人数上限为正整数！")
                }
                try {
                    port = txtPort.text.toInt()
                } catch (e1: Exception) {
                    throw Exception("请在端口框内输入一个正整数")
                }
                if (port <= 0) {
                    throw Exception("端口号 为正整数！")
                }
                serverStart(max, port)
                contentArea.append("${Calendar.getInstance().time}\n" +
                        "服务器启动！\n" +
                        "人数上限:$max, 端口号:$port\n" +
                        "祝使用愉快！\n")
                JOptionPane.showMessageDialog(frame, "服务器启动成功")
                btnStart.isEnabled = false
                txtMaxUserNumber.isEnabled = false
                txtPort.isEnabled = false
                btnStop.isEnabled = true
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(frame, e.message, "Error", JOptionPane.ERROR_MESSAGE)
            }
        }

        //停止开关的监听器
        btnStop.addActionListener {
            if (!isStart) {
                JOptionPane.showMessageDialog(frame, "服务器尚未启动", "Error",
                        JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            try {
                closeServer()
                btnStart.isEnabled = true
                txtMaxUserNumber.isEnabled = true
                txtPort.isEnabled = true
                btnStop.isEnabled = false
                contentArea.append("\n${Calendar.getInstance().time}\n" +
                        "服务器成功停止!\r\n")
                JOptionPane.showMessageDialog(frame, "服务器成功停止！")
            } catch (exc: Exception) {
                JOptionPane.showMessageDialog(frame, "停止服务器发生异常！", "Error",
                        JOptionPane.ERROR_MESSAGE)
            }
        }
    }

    // server send message
    private fun send() {
        if (!isStart) {
            JOptionPane.showMessageDialog(frame, "服务器还未启动,不能发送消息！", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        if (clients.size == 0) {
            JOptionPane.showMessageDialog(frame, "没有用户在线,不能发送消息！", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        val message: String? = txtMessage.text.trim()
        if (message == null || message == "") {
            JOptionPane.showMessageDialog(frame, "消息不能为空！", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        if (message.startsWith(COMMAND.SHUTUP) || message.startsWith(COMMAND.OPEN)) {
            var stringTokenizer = StringTokenizer(message, DEFAULT.DELIM)
            var command = stringTokenizer.nextToken()
            var id = stringTokenizer.nextToken()
            when (command) {
                COMMAND.SHUTUP -> {
                    var found = false
                    for (i in clients.indices.reversed()) {
                        if (clients[i].user!!.id == id!!.toInt()) {
                            clients[i].writer!!.println("服务器将你禁言${DEFAULT.NEWLINE}") //用户端显示这段
                            clients[i].writer!!.flush()
                            clients[i].isBan = true
                            found = true
                            break
                        }
                    }
                    if (found) {
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n禁言了id $id 的用户")
                    } else {
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n找不到此id")
                    }
                }
                COMMAND.OPEN -> {
                    var found = false
                    for (i in clients.indices.reversed()) {
                        if (clients[i].user!!.id == id!!.toInt()) {
                            if (clients[i].isBan) {  //如果确实被禁言了
                                clients[i].writer!!.println("服务器将你解禁${DEFAULT.NEWLINE}") //用户端显示这段
                                clients[i].writer!!.flush()
                                clients[i].isBan = false
                                contentArea.append("\n${Calendar.getInstance().time}" +
                                        "\n已解禁id${id}的用户")
                            } else {  //如果其实并没有被禁言
                                contentArea.append("\n${Calendar.getInstance().time}" +
                                        "\nid${id}的用户未被禁言")
                            }
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n未找到id $id 的用户")
                    }
                }
            }
        } else {
            sendServerMessage(message)// 群发服务器消息
            contentArea.append("\n${Calendar.getInstance().time}" +
                    "\n你的群发消息:  $message")
        }
        txtMessage.text = null
    }

    @Throws(BindException::class)
    private fun serverStart(max: Int, port: Int) {
        try {
            serverSocket = ServerSocket(port)
            serverThread = ServerThread(serverSocket, max)
            serverThread.start()
            isStart = true
        } catch (e: BindException) {
            isStart = false
            throw BindException("端口号已被占用，请换一个！")
        } catch (e1: Exception) {
            e1.printStackTrace()
            isStart = false
            throw BindException("启动服务器异常！")
        }

    }

    fun closeServer() {
        try {
            serverThread.stop()// 停止服务器线程

            for (i in clients.size - 1 downTo 0) {
                // 给所有在线用户发送关闭命令
                clients[i].writer!!.println("CLOSE")
                clients[i].writer!!.flush()
                // 释放资源
                clients[i].stop()// 停止此条为客户端服务的线程
                clients[i].reader!!.close()
                clients[i].writer!!.close()
                clients[i].socket!!.close()
                clients.removeAt(i)
            }
            serverSocket.close()// 关闭服务器端连接
            listModel.removeAllElements()// 清空用户列表
            isStart = false
        } catch (e: IOException) {
            e.printStackTrace()
            isStart = true
        }
    }

    private fun sendServerMessage(message: String) {//在用户的输入流中写入消息
        for (i in clients.size - 1 downTo 0) {
            clients[i].writer!!.println("服务器(群发消息): ${DEFAULT.NEWLINE}" +
                                        "   $message${DEFAULT.NEWLINE}")
            clients[i].writer!!.flush()
        }
    }

    //服务器线程
    inner class ServerThread(var serverSocket: ServerSocket, var max: Int) : Thread() {
        override fun run() {
            super.run()
            while (true) {
                try {
                    val socket = serverSocket.accept()
                    if (clients.size == max) {// 如果已达人数上限
                        val r = BufferedReader(InputStreamReader(socket.getInputStream()))
                        val w = PrintWriter(socket.getOutputStream())

                        // 接收客户端的基本用户信息
                        val inf = r.readLine()
                        val st = StringTokenizer(inf, DEFAULT.DELIM) //字符串分隔解析类型, delim 代表分隔符
                        val user = User(st.nextToken(), st.nextToken())
                        // 反馈连接信息
                        //  MAX + DELIM 用于服务器处理信息类型
                        w.println("MAX" + DEFAULT.DELIM + "${Calendar.getInstance().time}\n" +
                                "对不起, 来自ip ${user.ip} 的 ${user.name},\n" +
                                "当前服务器在线人数已达上线, 请稍后重连\n")
                        w.flush()
                        // 释放资源
                        r.close()
                        w.close()
                        socket.close()
                        continue
                    }

                    val client = ClientThread(socket)
                    client.start()// 开启对此客户端服务的线程
                    clients.add(client)
                    listModel.addElement("${client.user!!.name}(${client.user!!.id})")// 更新在线列表
                    contentArea.append("\n${Calendar.getInstance().time}" +
                            "\nid ${client.user!!.id}, 来自ip ${client.user!!.ip}的 ${client.user!!.name} 上线" +
                            "\n现有用户${clients.size}人")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
    //服务器端的文字更新以 contentArea.append() 的方式更新
    //客户端的文字则写入到他们对应的输出流中
    //writer!!.println 的东西最后都会在客户端显示出来
    internal inner class ClientThread(socket: Socket) : Thread() {
        var socket: Socket? = null
        var reader: BufferedReader? = null
            private set
        var writer: PrintWriter? = null
            private set
        var user: User? = null
            private set
        var isRegister = false
        var isBan = false

        //对信息的处理在Client中, 如果消息是前缀码+分隔符的形式,那么会有处理, 否则是向所有人转发
        init {
            try {
                this.socket = socket
                reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer = PrintWriter(socket.getOutputStream())
                // 接收客户端的基本用户信息
                val inf = reader!!.readLine()
                val st = StringTokenizer(inf, DEFAULT.DELIM)
                user = User(st.nextToken(), st.nextToken(), st.nextToken().toInt()) //Client把自己的姓名与端口信息写了进去
                // 客户端连接成功信息
                writer!!.println(
                        "与服务器连接成功!${DEFAULT.NEWLINE}" +
                        "IP: ${user!!.ip}${DEFAULT.NEWLINE} " +
                        "用户名: ${user!!.name}${DEFAULT.NEWLINE}" +
                        "id: ${user!!.id}${DEFAULT.NEWLINE}" +
                        "输入-help可以获得使用说明${DEFAULT.NEWLINE}")
                writer!!.flush()
                // 反馈当前在线用户信息
                if (clients.size > 0) {
                    var temp = ""
                    for (i in clients.indices.reversed()) {
                        temp += clients[i].user!!.name + DEFAULT.DELIM + clients[i].user!!.ip + DEFAULT.DELIM + clients[i].user!!.id
                    }
                    writer!!.println("USERLIST" + DEFAULT.DELIM + clients.size + DEFAULT.DELIM + temp)
                    writer!!.flush()
                }
                // 当一个进程新上线时, 向所有在线用户发送该用户上线命令
                for (i in clients.indices.reversed()) {
                    clients[i].writer!!.println("ADD" + DEFAULT.DELIM + user!!.name + DEFAULT.DELIM + user!!.ip + DEFAULT.DELIM + user!!.id)
                    clients[i].writer!!.flush()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // 线程一直在运行, 不断接收客户端的消息
        override fun run() {
            var message: String?
            while (true) {
                try {
                    message = reader!!.readLine()// 接收客户端消息
                    dispatcherMessage(message)// 如果不是断开连接,就转发消息
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        // 转发消息,发过来的信息格式为 (发送人名字,指令,内容)
        private fun dispatcherMessage(message: String) {
            val stringTokenizer = StringTokenizer(message, DEFAULT.DELIM)
            val speaker = stringTokenizer.nextToken()
            val owner = stringTokenizer.nextToken()
            val content = stringTokenizer.nextToken()
            //一级指令,包括 注册 和 退出
            when(owner){
                "REGISTER" -> {
                    if (!isRegister) {
                        isRegister = true
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n${speaker}注册了")
                        writer!!.println("注册成功${DEFAULT.NEWLINE}")
                        writer!!.flush()
                        return
                    } else { //如果已经注册
                        writer!!.println("请不要重复注册${DEFAULT.NEWLINE}")
                        writer!!.flush()
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n${speaker}试图重复注册")
                    }
                }
                "CLOSE" ->{
                    if(isRegister){
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n来自ip ${user!!.ip} 的${user!!.name}下线" +
                                "\n 现有用户${clients.size - 1}人\n")
                    } else {
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n未注册用户${speaker}离开了服务器" +
                                "\n 现有用户${clients.size - 1}人\n")
                    }
                    reader!!.close()
                    writer!!.close()
                    socket!!.close()

                    // 向所有在线用户发送该用户的下线命令
                    for (i in clients.indices.reversed()) {
                        clients[i].writer!!.println("DELETE" + DEFAULT.DELIM + user!!.name + DEFAULT.DELIM + user!!.id)
                        clients[i].writer!!.flush()
                    }

                    listModel.removeElement("${speaker}(${content})")// 更新在线列表
                    // 删除此条客户端服务线程
                    for (i in clients.indices.reversed()) {
                        if (clients[i].user!!.name == user!!.name) {
                            val temp = clients[i]
                            temp.stop()// 停止这条服务线程
                            clients.removeAt(i)// 删除此用户的服务线程
                            return
                        }
                    }
                }
            }
            if (!isRegister) {
                contentArea.append("\n${Calendar.getInstance().time}" +
                        "\n未注册用户${speaker}请求服务")
                writer!!.println("请先注册${DEFAULT.NEWLINE}")
                writer!!.flush()
                return
            } else if (isBan) {
                contentArea.append("\n${Calendar.getInstance().time}" +
                        "\n禁言用户${speaker}请求服务")
                writer!!.println("你被服务器禁言了${DEFAULT.NEWLINE}")
                writer!!.flush()
                return
            } else {
                when (owner) { //二级指令
                    "ALL" -> {
                        for (i in clients.indices.reversed()) {  //在用户面板上显示这段
                            clients[i].writer!!.println("$speaker: ${DEFAULT.NEWLINE}" +
                                                        "        $content${DEFAULT.NEWLINE}") //用户端显示这段
                            clients[i].writer!!.flush()
                        }
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n$speaker(群发消息): $content")
                    }
                    "HELP" -> {
                        writer!!.println("${DEFAULT.NEWLINE}${DEFAULT.MANUAL}${DEFAULT.NEWLINE}") //用户端显示这段
                        writer!!.flush()
                        contentArea.append("\n$speaker 请求了一次帮助")
                    }
                    "CHAT" -> {
                        //content in this block represent id
                        val message = stringTokenizer.nextToken()
                        var exist = false
                        for (i in clients.indices.reversed()) {  //在用户面板上显示这段
                            if (clients[i].user!!.id == content.toInt()) {
                                clients[i].writer!!.println("${speaker}悄悄地对你说: ${DEFAULT.NEWLINE}" +
                                                            "        $message${DEFAULT.NEWLINE}") //用户端显示这段
                                clients[i].writer!!.flush()
                                exist = true
                            }
                        }
                        if (exist) {
                            writer!!.println("你对${content}说:${DEFAULT.NEWLINE}" +
                                             "        $message${DEFAULT.NEWLINE}") //发送端
                            contentArea.append("\n${Calendar.getInstance().time}" +
                                    "\n${speaker}私聊$content: $message")
                        } else {
                            writer!!.println("没有这个用户${DEFAULT.NEWLINE}")
                            contentArea.append("\n${Calendar.getInstance().time}" +
                                    "\n${speaker}试图向一个不存在的用户发信息")
                        }
                        writer!!.flush()
                    }
                    else -> {
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n$speaker: $content") // 服务器端显示这段, Client中的send函数最后会到这里处理
                    }
                }
            }
        }
    }
}