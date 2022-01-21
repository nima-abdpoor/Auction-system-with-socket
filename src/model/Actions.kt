package model

const val LOGIN = "LOGIN"
const val OFFER = "OFFER"
const val HELLO = "HELLO"
const val WINNER = "WINNER"
const val ROUND = "ROUND"
const val ACTION = "ACTION"
const val AUCTION = "AUCTION"
const val SUGGESTION = "SUGGESTION"
const val END = "END"

sealed class Actions {
    data class Login(val user: User) : Actions()
    data class Offer(val user: User, val amount: String) : Actions()
    data class Round(val suggestions: List<Suggestion>? ,val desc : String) : Actions()
    data class Auction(val product: String, val basePrice: String?) : Actions()
    data class UnKnown(val message: String) : Actions()
    data class End(val user: User , val amount : String) : Actions()
}