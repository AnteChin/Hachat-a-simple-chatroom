/*
* ����û�����Ĳ���:
* 1. ��COMMAND����д��ָ��
* 2. Clent �е�when(temp)ָ����д�� �����������ָ��ʱ, ������������ǰ׺��
* 3. ��Server�е�dispatcherMessage������д�ô�����Ӧǰ׺��Ĳ���
*
* ��ӿͻ�����Ĳ���:
* 1. д��ָ��
* 2. ��Server�е�send�����������޸�
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