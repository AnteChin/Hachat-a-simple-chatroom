# Hachat-a-simple-chatroom

This is a simple chatroom including server and clients.<br>   
Using commands to achieve group chat, private chat, banned words and other functions. <br>
Text and comments in code are written by Chinese, If you need English edition, contact me: ante.jin.chin@gmail.com <br>
I will reply in time

---
## Version Information 
Kotlin: 1.2.24 <br>
JVM: 1.8 <br>

Run `main.kt` to test, if you don't download [beautyeye](https://github.com/JackJiang2011/beautyeye),  you have to delete the *try-catch* block in `main.kt`

## Code Structure 
The `Server` class is a server that contains a Server thread and an array of Client threads. Each user logs in, opens a user thread for the server, and sends messages to the client. <br>
The `Client` class is a client that contains a message thread that is responsible for receiving messages sent from the server <br>
Use Socket to connect with each other <br>
Special instructions will start with a unique prefix. If the receiving party detects the prefix, it will perform the corresponding operation. Otherwise, it will be treated as a normal message. <br>

## Features

To implement functions using the instruction set, see `COMMAND.KT`<br>
The implemented client instructions are: <br>
* `-reg`: Registration <br>
* `-help`: Get Help <br>
* `-to`: private chat, format `-to# id# your message, `#` is delimiter, if you want to modify, find `DELIM` under `DEFAULT.kt`<br>

Server-side features already implemented: <br>
* `-ban`: banned, in the format `-ban#other id` <br>
* `-open`: Unlock the file in `-open# counterpart id` <br>

Add a new instruction: Write the form of the command sent on the client/server send, write in the server/client message handler how to deal with the received instruction

---
## 版本信息

Kotlin: 1.2.24 <br>
JVM: 1.8 <br>

运行`main.kt`来测试，如果你没有装beautyeye，那么你得注释掉main.kt中的try-block区域

## 代码结构

`Server` 类是服务端， 内含一个Server线程和一个Client线程数组， 每有一个用户登录，为其在服务端开辟一个用户线程，负责向用户端发送消息 <br>
`Client` 类是客户端，内含一个消息线程，负责接收从服务端发来的消息 <br>
使用Socket连接彼此 <br>
特殊的指令会以特有的前缀开头，如果接收的一方检测到了这种前缀，则会做出相应的操作，否则视为普通消息 <br>

## 功能

用指令集的方式实现功能，参见`COMMAND.KT`<br>
已经实现的客户端指令有: <br>
* `-reg`: 注册 <br>
* `-help`: 获得帮助 <br>
* `-to`: 私聊, 格式为`-to#对方id#你的消息`， `#`为分隔符，如果想修改，找到`DEFAULT.kt`下的`DELIM`<br>

已经实现的服务端功能: <br>
* `-ban`: 禁言，格式为 `-ban#对方id` <br>
* `-open`: 解禁，格式为 `-open#对方id` <br>

添加新指令: 在客户端/服务端的send写上发送的指令形式， 在服务端/客户端的消息处理函数中写上如何处理收到的指令
