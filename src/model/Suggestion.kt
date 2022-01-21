package model

data class Suggestion(
    var name : String,
    var amount : String,
    var id : String
){
    override fun toString(): String {
        return "$name,$amount$"
    }
}