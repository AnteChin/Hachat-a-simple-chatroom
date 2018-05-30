object DEFAULT {
    const val PORT: Int = 6789
    const val IP: String = "127.0.0.1"
    const val USER_COUNT : Int = 10
    const val NEWLINE:String = "@"
    const val DELIM: String = "#"
    const val FONT: String = "Consolas"

    val MANUAL : String ="""
        *******************************************************
        这是本程序的操作手册:
        在发送框键入以下指令会获得服务器的反馈
        请求帮助        ${COMMAND.HELP}
        注册            ${COMMAND.REGISTER}
        私发信息        ${COMMAND.TO}${DEFAULT.DELIM}他的id${DEFAULT.DELIM}你的消息内容
                                e.g.   -to#10002# How are you?
        联系方式        ante.jin.chin@gmail.com
        *******************************************************
    """.trimIndent()
        /*

           var s =  "这是本程序的操作手册:" +
                    "\n 在发送框键入以下指令会获得服务器的反馈" +
                    "\n 请求帮助        ${COMMAND.HELP}" +
                    "\n 注册               ${COMMAND.REGISTER}" +
                    "\n 私发信息        ${COMMAND.TO}${DEFAULT.DELIM}他的id${DEFAULT.DELIM}你的消息内容" +
                    "\n 联系方式        ante.jin.chin@gmail.com"*/
}