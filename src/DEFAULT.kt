object DEFAULT {
    const val PORT: Int = 6789
    const val IP: String = "127.0.0.1"
    const val USER_COUNT : Int = 10
    const val NEWLINE:String = "^"
    const val DELIM: String = "#"
    const val FONT: String = "Consolas"

    val MANUAL : String =
        "*******************************************************${NEWLINE}" +
        "���Ǳ�����Ĳ����ֲ�:${NEWLINE}" +
        "�ڷ��Ϳ��������ָ����÷������ķ���${NEWLINE}" +
        "�������        ${COMMAND.HELP}${NEWLINE}" +
        "ע��            ${COMMAND.REGISTER}${NEWLINE}" +
        "˽����Ϣ        ${COMMAND.TO}${DEFAULT.DELIM}����id${DEFAULT.DELIM}�����Ϣ����${NEWLINE}" +
        "                       e.g.   -to#10002# How are you?${NEWLINE}" +
        "��ϵ��ʽ        ante.jin.chin@gmail.com${NEWLINE}" +
        "*******************************************************${NEWLINE}"
        /*

           var s =  "���Ǳ�����Ĳ����ֲ�:" +
                    "\n �ڷ��Ϳ��������ָ����÷������ķ���" +
                    "\n �������        ${COMMAND.HELP}" +
                    "\n ע��               ${COMMAND.REGISTER}" +
                    "\n ˽����Ϣ        ${COMMAND.TO}${DEFAULT.DELIM}����id${DEFAULT.DELIM}�����Ϣ����" +
                    "\n ��ϵ��ʽ        ante.jin.chin@gmail.com"*/
}