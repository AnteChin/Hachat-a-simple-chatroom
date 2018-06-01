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
    private var btnStart = JButton("��ʼ")
    private var btnStop = JButton("��ֹ")
    private var btnSend = JButton("����")
    private var listModel = DefaultListModel<String>()  //�û��б�
    private var userList = JList(listModel)
    private var southPanel = JPanel(BorderLayout())
    private var leftPanel = JScrollPane(userList)
    private var rightPanel = JScrollPane(contentArea)
    private var centerSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel)
    private var northPanel = JPanel()

    private lateinit var serverSocket: ServerSocket
    private lateinit var serverThread: ServerThread
    private var clients: ArrayList<ClientThread> = java.util.ArrayList() //װ���û��̵߳�����

    var isStart = false

    init {
        contentArea.isEditable = false
        contentArea.foreground = Color.BLUE

        btnStart.background = Color.GREEN
        btnStop.isEnabled = false
        btnStop.background = Color.RED.darker()
        btnSend.background = Color.BLUE.brighter().brighter()

        southPanel.border = TitledBorder("д��Ϣ")
        southPanel.add(txtMessage, "Center")
        southPanel.add(btnSend, "East")

        leftPanel.border = TitledBorder("�����û�")
        rightPanel.border = TitledBorder("��Ϣ")

        centerSplit.dividerLocation = 100

        with(northPanel) {
            layout = GridLayout(1, 6)
            add(JLabel("��������"))
            add(txtMaxUserNumber)
            add(JLabel("�˿�"))
            add(txtPort)
            add(btnStart)
            add(btnStop)
            border = TitledBorder("������Ϣ")
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

        //�������صļ�����
        btnStart.addActionListener {
            if (isStart) {
                JOptionPane.showMessageDialog(frame, "������������,�����ظ�", "Error", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            val max: Int
            val port: Int
            try {
                try {
                    max = txtMaxUserNumber.text.toInt()
                } catch (e1: Exception) {
                    throw Exception("�����û�������������һ��������")
                }
                if (max <= 0) {
                    throw Exception("��������Ϊ��������")
                }
                try {
                    port = txtPort.text.toInt()
                } catch (e1: Exception) {
                    throw Exception("���ڶ˿ڿ�������һ��������")
                }
                if (port <= 0) {
                    throw Exception("�˿ں� Ϊ��������")
                }
                serverStart(max, port)
                contentArea.append("${Calendar.getInstance().time}\n" +
                        "������������\n" +
                        "��������:$max, �˿ں�:$port\n" +
                        "ףʹ����죡\n")
                JOptionPane.showMessageDialog(frame, "�����������ɹ�")
                btnStart.isEnabled = false
                txtMaxUserNumber.isEnabled = false
                txtPort.isEnabled = false
                btnStop.isEnabled = true
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(frame, e.message, "Error", JOptionPane.ERROR_MESSAGE)
            }
        }

        //ֹͣ���صļ�����
        btnStop.addActionListener {
            if (!isStart) {
                JOptionPane.showMessageDialog(frame, "��������δ����", "Error",
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
                        "�������ɹ�ֹͣ!\r\n")
                JOptionPane.showMessageDialog(frame, "�������ɹ�ֹͣ��")
            } catch (exc: Exception) {
                JOptionPane.showMessageDialog(frame, "ֹͣ�����������쳣��", "Error",
                        JOptionPane.ERROR_MESSAGE)
            }
        }
    }

    // server send message
    private fun send() {
        if (!isStart) {
            JOptionPane.showMessageDialog(frame, "��������δ����,���ܷ�����Ϣ��", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        if (clients.size == 0) {
            JOptionPane.showMessageDialog(frame, "û���û�����,���ܷ�����Ϣ��", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        val message: String? = txtMessage.text.trim()
        if (message == null || message == "") {
            JOptionPane.showMessageDialog(frame, "��Ϣ����Ϊ�գ�", "Error", JOptionPane.ERROR_MESSAGE)
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
                            clients[i].writer!!.println("�������������${DEFAULT.NEWLINE}") //�û�����ʾ���
                            clients[i].writer!!.flush()
                            clients[i].isBan = true
                            found = true
                            break
                        }
                    }
                    if (found) {
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n������id $id ���û�")
                    } else {
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n�Ҳ�����id")
                    }
                }
                COMMAND.OPEN -> {
                    var found = false
                    for (i in clients.indices.reversed()) {
                        if (clients[i].user!!.id == id!!.toInt()) {
                            if (clients[i].isBan) {  //���ȷʵ��������
                                clients[i].writer!!.println("������������${DEFAULT.NEWLINE}") //�û�����ʾ���
                                clients[i].writer!!.flush()
                                clients[i].isBan = false
                                contentArea.append("\n${Calendar.getInstance().time}" +
                                        "\n�ѽ��id${id}���û�")
                            } else {  //�����ʵ��û�б�����
                                contentArea.append("\n${Calendar.getInstance().time}" +
                                        "\nid${id}���û�δ������")
                            }
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\nδ�ҵ�id $id ���û�")
                    }
                }
            }
        } else {
            sendServerMessage(message)// Ⱥ����������Ϣ
            contentArea.append("\n${Calendar.getInstance().time}" +
                    "\n���Ⱥ����Ϣ:  $message")
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
            throw BindException("�˿ں��ѱ�ռ�ã��뻻һ����")
        } catch (e1: Exception) {
            e1.printStackTrace()
            isStart = false
            throw BindException("�����������쳣��")
        }

    }

    fun closeServer() {
        try {
            serverThread.stop()// ֹͣ�������߳�

            for (i in clients.size - 1 downTo 0) {
                // �����������û����͹ر�����
                clients[i].writer!!.println("CLOSE")
                clients[i].writer!!.flush()
                // �ͷ���Դ
                clients[i].stop()// ֹͣ����Ϊ�ͻ��˷�����߳�
                clients[i].reader!!.close()
                clients[i].writer!!.close()
                clients[i].socket!!.close()
                clients.removeAt(i)
            }
            serverSocket.close()// �رշ�����������
            listModel.removeAllElements()// ����û��б�
            isStart = false
        } catch (e: IOException) {
            e.printStackTrace()
            isStart = true
        }
    }

    private fun sendServerMessage(message: String) {//���û�����������д����Ϣ
        for (i in clients.size - 1 downTo 0) {
            clients[i].writer!!.println("������(Ⱥ����Ϣ): ${DEFAULT.NEWLINE}" +
                                        "   $message${DEFAULT.NEWLINE}")
            clients[i].writer!!.flush()
        }
    }

    //�������߳�
    inner class ServerThread(var serverSocket: ServerSocket, var max: Int) : Thread() {
        override fun run() {
            super.run()
            while (true) {
                try {
                    val socket = serverSocket.accept()
                    if (clients.size == max) {// ����Ѵ���������
                        val r = BufferedReader(InputStreamReader(socket.getInputStream()))
                        val w = PrintWriter(socket.getOutputStream())

                        // ���տͻ��˵Ļ����û���Ϣ
                        val inf = r.readLine()
                        val st = StringTokenizer(inf, DEFAULT.DELIM) //�ַ����ָ���������, delim ����ָ���
                        val user = User(st.nextToken(), st.nextToken())
                        // ����������Ϣ
                        //  MAX + DELIM ���ڷ�����������Ϣ����
                        w.println("MAX" + DEFAULT.DELIM + "${Calendar.getInstance().time}\n" +
                                "�Բ���, ����ip ${user.ip} �� ${user.name},\n" +
                                "��ǰ���������������Ѵ�����, ���Ժ�����\n")
                        w.flush()
                        // �ͷ���Դ
                        r.close()
                        w.close()
                        socket.close()
                        continue
                    }

                    val client = ClientThread(socket)
                    client.start()// �����Դ˿ͻ��˷�����߳�
                    clients.add(client)
                    listModel.addElement("${client.user!!.name}(${client.user!!.id})")// ���������б�
                    contentArea.append("\n${Calendar.getInstance().time}" +
                            "\nid ${client.user!!.id}, ����ip ${client.user!!.ip}�� ${client.user!!.name} ����" +
                            "\n�����û�${clients.size}��")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
    //�������˵����ָ����� contentArea.append() �ķ�ʽ����
    //�ͻ��˵�������д�뵽���Ƕ�Ӧ���������
    //writer!!.println �Ķ�����󶼻��ڿͻ�����ʾ����
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

        //����Ϣ�Ĵ�����Client��, �����Ϣ��ǰ׺��+�ָ�������ʽ,��ô���д���, ��������������ת��
        init {
            try {
                this.socket = socket
                reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer = PrintWriter(socket.getOutputStream())
                // ���տͻ��˵Ļ����û���Ϣ
                val inf = reader!!.readLine()
                val st = StringTokenizer(inf, DEFAULT.DELIM)
                user = User(st.nextToken(), st.nextToken(), st.nextToken().toInt()) //Client���Լ���������˿���Ϣд�˽�ȥ
                // �ͻ������ӳɹ���Ϣ
                writer!!.println(
                        "����������ӳɹ�!${DEFAULT.NEWLINE}" +
                        "IP: ${user!!.ip}${DEFAULT.NEWLINE} " +
                        "�û���: ${user!!.name}${DEFAULT.NEWLINE}" +
                        "id: ${user!!.id}${DEFAULT.NEWLINE}" +
                        "����-help���Ի��ʹ��˵��${DEFAULT.NEWLINE}")
                writer!!.flush()
                // ������ǰ�����û���Ϣ
                if (clients.size > 0) {
                    var temp = ""
                    for (i in clients.indices.reversed()) {
                        temp += clients[i].user!!.name + DEFAULT.DELIM + clients[i].user!!.ip + DEFAULT.DELIM + clients[i].user!!.id
                    }
                    writer!!.println("USERLIST" + DEFAULT.DELIM + clients.size + DEFAULT.DELIM + temp)
                    writer!!.flush()
                }
                // ��һ������������ʱ, �����������û����͸��û���������
                for (i in clients.indices.reversed()) {
                    clients[i].writer!!.println("ADD" + DEFAULT.DELIM + user!!.name + DEFAULT.DELIM + user!!.ip + DEFAULT.DELIM + user!!.id)
                    clients[i].writer!!.flush()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // �߳�һֱ������, ���Ͻ��տͻ��˵���Ϣ
        override fun run() {
            var message: String?
            while (true) {
                try {
                    message = reader!!.readLine()// ���տͻ�����Ϣ
                    dispatcherMessage(message)// ������ǶϿ�����,��ת����Ϣ
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        // ת����Ϣ,����������Ϣ��ʽΪ (����������,ָ��,����)
        private fun dispatcherMessage(message: String) {
            val stringTokenizer = StringTokenizer(message, DEFAULT.DELIM)
            val speaker = stringTokenizer.nextToken()
            val owner = stringTokenizer.nextToken()
            val content = stringTokenizer.nextToken()
            //һ��ָ��,���� ע�� �� �˳�
            when(owner){
                "REGISTER" -> {
                    if (!isRegister) {
                        isRegister = true
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n${speaker}ע����")
                        writer!!.println("ע��ɹ�${DEFAULT.NEWLINE}")
                        writer!!.flush()
                        return
                    } else { //����Ѿ�ע��
                        writer!!.println("�벻Ҫ�ظ�ע��${DEFAULT.NEWLINE}")
                        writer!!.flush()
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n${speaker}��ͼ�ظ�ע��")
                    }
                }
                "CLOSE" ->{
                    if(isRegister){
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n����ip ${user!!.ip} ��${user!!.name}����" +
                                "\n �����û�${clients.size - 1}��\n")
                    } else {
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\nδע���û�${speaker}�뿪�˷�����" +
                                "\n �����û�${clients.size - 1}��\n")
                    }
                    reader!!.close()
                    writer!!.close()
                    socket!!.close()

                    // �����������û����͸��û�����������
                    for (i in clients.indices.reversed()) {
                        clients[i].writer!!.println("DELETE" + DEFAULT.DELIM + user!!.name + DEFAULT.DELIM + user!!.id)
                        clients[i].writer!!.flush()
                    }

                    listModel.removeElement("${speaker}(${content})")// ���������б�
                    // ɾ�������ͻ��˷����߳�
                    for (i in clients.indices.reversed()) {
                        if (clients[i].user!!.name == user!!.name) {
                            val temp = clients[i]
                            temp.stop()// ֹͣ���������߳�
                            clients.removeAt(i)// ɾ�����û��ķ����߳�
                            return
                        }
                    }
                }
            }
            if (!isRegister) {
                contentArea.append("\n${Calendar.getInstance().time}" +
                        "\nδע���û�${speaker}�������")
                writer!!.println("����ע��${DEFAULT.NEWLINE}")
                writer!!.flush()
                return
            } else if (isBan) {
                contentArea.append("\n${Calendar.getInstance().time}" +
                        "\n�����û�${speaker}�������")
                writer!!.println("�㱻������������${DEFAULT.NEWLINE}")
                writer!!.flush()
                return
            } else {
                when (owner) { //����ָ��
                    "ALL" -> {
                        for (i in clients.indices.reversed()) {  //���û��������ʾ���
                            clients[i].writer!!.println("$speaker: ${DEFAULT.NEWLINE}" +
                                                        "        $content${DEFAULT.NEWLINE}") //�û�����ʾ���
                            clients[i].writer!!.flush()
                        }
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n$speaker(Ⱥ����Ϣ): $content")
                    }
                    "HELP" -> {
                        writer!!.println("${DEFAULT.NEWLINE}${DEFAULT.MANUAL}${DEFAULT.NEWLINE}") //�û�����ʾ���
                        writer!!.flush()
                        contentArea.append("\n$speaker ������һ�ΰ���")
                    }
                    "CHAT" -> {
                        //content in this block represent id
                        val message = stringTokenizer.nextToken()
                        var exist = false
                        for (i in clients.indices.reversed()) {  //���û��������ʾ���
                            if (clients[i].user!!.id == content.toInt()) {
                                clients[i].writer!!.println("${speaker}���ĵض���˵: ${DEFAULT.NEWLINE}" +
                                                            "        $message${DEFAULT.NEWLINE}") //�û�����ʾ���
                                clients[i].writer!!.flush()
                                exist = true
                            }
                        }
                        if (exist) {
                            writer!!.println("���${content}˵:${DEFAULT.NEWLINE}" +
                                             "        $message${DEFAULT.NEWLINE}") //���Ͷ�
                            contentArea.append("\n${Calendar.getInstance().time}" +
                                    "\n${speaker}˽��$content: $message")
                        } else {
                            writer!!.println("û������û�${DEFAULT.NEWLINE}")
                            contentArea.append("\n${Calendar.getInstance().time}" +
                                    "\n${speaker}��ͼ��һ�������ڵ��û�����Ϣ")
                        }
                        writer!!.flush()
                    }
                    else -> {
                        contentArea.append("\n${Calendar.getInstance().time}" +
                                "\n$speaker: $content") // ����������ʾ���, Client�е�send�������ᵽ���ﴦ��
                    }
                }
            }
        }
    }
}