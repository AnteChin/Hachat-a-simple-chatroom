class User(var name: String,var ip: String){
    companion object {
        var counts = 0

        fun createUser(){
            counts++
        }
    }
    var id: Int = 10000 + counts

    constructor(name :String, ip: String,id : Int):this(name, ip){
        this.id = id
    }

    constructor(name :String, ip: String,id : String):this(name, ip){
        this.id = id.toInt()
    }
}