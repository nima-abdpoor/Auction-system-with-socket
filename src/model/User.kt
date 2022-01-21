package model

data class User(
    var name : String ="",
    var address : String ="",
    var id : String ="",
    var description : String =""
){
    override fun toString(): String {
        return "User(name='$name', address='$address', description='$description')"
    }
}