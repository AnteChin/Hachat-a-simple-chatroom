fun main(args: Array<String>){
    var users = 2
    Server()
    for(i in 0..(users-1)) {
        Client()
    }
}