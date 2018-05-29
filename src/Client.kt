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

    private val frame = JFrame("�ͻ���")
    private val listModel = DefaultListModel<String>()
    private val userList = JList(listModel)

    private val textArea = JTextArea()
    private val textField = JTextField()
    private val txtPort = JTextField(DEFAULT.PORT.toString())
    private val txtHostIp = JTextField(DEFAULT.IP)
    private val txtName = JTextField("�û���")
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
    private var messageThread: MessageThread? = null// ���������Ϣ���߳�
    private val onLineUsers = HashMap<String, User>()// ���������û�

    init {
        textArea.isEditable = false
        textArea.foreground = Color.blue

        btnStart.background = Color.GREEN
        btnStop.background = Color.RED.darker()
        btnSend.background = Color.BLUE.brighter()

        with(northPanel) {
            layout = GridLayout(1, 7)
            add(JLabel("�˿�"))
            add(txtPort)
            add(JLabel("������IP"))
            add(txtHostIp)
            add(JLabel("����"))
            add(txtName)
            add(btnStart)
            add(btnStop)
            border = TitledBorder("������Ϣ")
        }

        rightScroll.border = TitledBorder("��Ϣ��ʾ��")
        leftScroll.border = TitledBorder("�����û�")

        southPanel.add(textField, "Center")
        southPanel.add(btnSend, "East")
        southPanel.border = TitledBorder("д��Ϣ")
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

        // д��Ϣ���ı����а��س���ʱ�¼�
        textField.addActionListener { send() }

        // �������Ͱ�ťʱ�¼�
        btnSend.addActionListener { send() }

        // �������Ӱ�ťʱ�¼�
        btnStart.addActionListener(ActionListener {
            val port: Int
            if (isConnected) {
                JOptionPane.showMessageDialog(frame, "�Ѵ���������״̬����Ҫ�ظ�����!",
                        "Error", JOptionPane.ERROR_MESSAGE)
                return@ActionListener
            }
            try {
                try {
                    port = txtPort.text.trim().toInt()
                } catch (e2: NumberFormatException) {
                    throw Exception("�˿ںŲ�����Ҫ��")
                }

                val hostIp = txtHostIp.text.trim()
                val name = txtName.text.trim()
                if (name == "" || hostIp == "") {
                    throw Exception("������������IP����Ϊ��!")
                }
                me = User(name, hostIp)//�����û�
                User.createUser()
                if (!connectServer(port, me)) {
                    throw Exception("�����������ʧ��!")
                }

                JOptionPane.showMessageDialog(frame, "�ɹ�����!")
            } catch (exc: Exception) {
                JOptionPane.showMessageDialog(frame, exc.message,
                        "Error", JOptionPane.ERROR_MESSAGE)
            }
        })

        // �����Ͽ���ťʱ�¼�
        btnStop.addActionListener(ActionListener {
            if (!isConnected) {
                JOptionPane.showMessageDialog(frame, "�Ѵ��ڶϿ�״̬����Ҫ�ظ��Ͽ�!",
                        "Error", JOptionPane.ERROR_MESSAGE)
                return@ActionListener
            }
            try {
                if (!closeConnection()) { // ����Ͽ����Ӳ��ɹ�
                    throw Exception("�Ͽ����ӷ����쳣��")
                }
                JOptionPane.showMessageDialog(frame, "�ɹ��Ͽ�!")
            } catch (exc: Exception) {
                JOptionPane.showMessageDialog(frame, exc.message,
                        "Error", JOptionPane.ERROR_MESSAGE)
            }
        })

        // �رմ���ʱ�¼�
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                if (isConnected) {
                    closeConnection()// �ر�����
                }
                frame.dispose()
            }
        })
    }

    // �����������ָ��
    private fun send() {
        if (!isConnected) {
            JOptionPane.showMessageDialog(frame, "��û�����ӷ��������޷�������Ϣ��", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        val message: String? = textField.text
        if (message == null || message == "") {
            JOptionPane.showMessageDialog(frame, "��Ϣ����Ϊ�գ�", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        if (message.startsWith(DEFAULT.DELIM)){
            JOptionPane.showMessageDialog(frame, "��Ҫ��${DEFAULT.DELIM}��ͷ", "Error", JOptionPane.ERROR_MESSAGE)
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
            socket = Socket(user.ip, port)// ���ݶ˿ںźͷ�����ip��������
            writer = PrintWriter(socket.getOutputStream())
            reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // ���Ϳͻ����û�������Ϣ(�û�����ip��ַ)
            sendMessage(user.name + DEFAULT.DELIM + socket.localAddress.toString() + DEFAULT.DELIM + user.id)
            // ����������Ϣ���߳�
            messageThread = MessageThread(reader, textArea)
            messageThread!!.start()
            isConnected = true// �Ѿ���������
            return true
        } catch (e: Exception) {
            textArea.append("${Calendar.getInstance().time}\n" +
                    "��˿ں�Ϊ��" + port + ",    IP��ַΪ��${user.ip} �ķ���������ʧ��!" + "\r\n")
            isConnected = false// δ������
            return false
        }
    }

    //�ͻ��������ر�����
    @Synchronized
    fun closeConnection(): Boolean {
        try {
            sendMessage("CLOSE")// ���ͶϿ����������������
            messageThread!!.stop()// ֹͣ������Ϣ�߳�
            // �ͷ���Դ
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
        // �����Ĺر�����
        @Synchronized
        @Throws(Exception::class)
        fun closeCon() {
            // ����û��б�
            listModel.removeAllElements()
            // �����Ĺر������ͷ���Դ
            reader?.close()
            if (writer != null) {
                writer.close()
            }
            if (socket != null) {
                socket.close()
            }
            isConnected = false// �޸�״̬Ϊ�Ͽ�
        }

        override fun run() {
            var message: String
            while (true) {
                try {
                    message = reader!!.readLine()
                    val stringTokenizer = StringTokenizer(message, DEFAULT.DELIM)
                    if (stringTokenizer.hasMoreTokens()) {
                        val command = stringTokenizer.nextToken()// ������������
                        when (command) { //�����������������ָ��
                            "CLOSE" -> {
                                textArea.append("�������ѹر�!\r\n")
                                closeCon()// �����Ĺر�����
                                return // �����߳�
                            }
                            "ADD" -> {// ���û����߸��������б�
                                val username: String? = stringTokenizer.nextToken()
                                val userIp: String? = stringTokenizer.nextToken()
                                val userId : String? = stringTokenizer.nextToken()
                                if (username != null && userIp != null && userId != null) {
                                    val user = User(username, userIp, userId)
                                    onLineUsers[username] = user
                                    listModel.addElement("$username($userId)")
                                }
                            }
                            "DELETE" -> {// ���û����߸��������б�
                                val username = stringTokenizer.nextToken()
                                onLineUsers.remove(username)
                                listModel.removeElement(username)
                            }
                            "USERLIST" -> {// ���������û��б�
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
                            "MAX" -> {// �����Ѵ�����
                                textArea.append(stringTokenizer.nextToken()
                                        + stringTokenizer.nextToken() + "\r\n")
                                closeCon()// �����Ĺر�����
                                JOptionPane.showMessageDialog(frame, "������������������", "����",
                                        JOptionPane.ERROR_MESSAGE)
                                return // �����߳�
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