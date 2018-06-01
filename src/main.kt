import javax.swing.UIManager

fun main(args: Array<String>){
    try {
        org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper.launchBeautyEyeLNF()
        UIManager.put("RootPane.setupButtonVisible", false)
    } catch (e: Exception) {
        //TODO exception
    }
    var users = 2
    Server()
    for(i in 0..(users-1)) {
        Client()
    }
}