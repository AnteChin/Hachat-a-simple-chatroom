/*
* 添加用户命令的步骤:
* 1. 在COMMAND类中写好指令
* 2. Clent 中的when(temp)指令中写好 输入框中输入指令时, 传给服务器的前缀码
* 3. 在Server中的dispatcherMessage方法里写好处理相应前缀码的操作
*
* 添加客户命令的步骤:
* 1. 写好指令
* 2. 在Server中的send方法里做好修改
*/

object COMMAND{
    //for user
    const val HELP = "-help"     //-help
    const val TO = "-to"         //-to#id#your message
    const val REGISTER = "-reg"  //-reg

    //for server
    const val SHUTUP = "-ban"   //-ban#id
    const val OPEN = "-open"    //-open#id
}