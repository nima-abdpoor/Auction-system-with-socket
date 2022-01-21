import mapper.Mappers
import model.*
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

fun String.detectCommand(): Actions {
    val mapper = Mappers()
    val suggestions = listOf<Suggestion>()
    val jsonObject = JSONParser().parse(this) as JSONObject
    return when (jsonObject[model.ACTION].toString()) {
        LOGIN -> {
            Actions.Login(mapper.getUser(this))
        }
        OFFER -> {
            Actions.Offer(mapper.getUser(this), jsonObject["amount"].toString())
        }
        ROUND -> {
            Actions.Round(suggestions,jsonObject[SUGGESTION].toString())
        }
        AUCTION ->{
            Actions.Auction(jsonObject["productName"].toString(), jsonObject["basePrice"].toString())
        }
        END ->{
            Actions.End(mapper.getUser(this), jsonObject["amount"].toString())
        }
        else -> Actions.UnKnown("")
    }
}
private val products = listOf("Apple", "Pride", "ASUS_S5533EQ")


fun User.detectCommand(sc: String): Actions {
    val commands = sc.split(":")
    return when (commands[0]) {
        "login", "LOGIN", "Login" -> Actions.Login(this)
        "offer", "OFFER", "Offer" -> Actions.Offer(this, commands[1])
        "round", "ROUND", "Round" -> Actions.Round(null,"")
        "END"  , "end", "End" -> Actions.End(this, "")
        "auction", "AUCTION", "Action" -> {
            if (sc.contains(":")){
                Actions.Auction(commands[1], null)
            }
            else Actions.Auction(products[0], null)
        }
        else -> Actions.UnKnown("Unknown Command!")
    }
}

fun Actions.runCommand(clientSocket: ClientSocket) {
    when (this) {
        is Actions.Login -> {
            clientSocket.login(user)
        }
        is Actions.Offer -> {
            clientSocket.offer(user, amount)
        }
        is Actions.Round -> {
            println("Only SERVER!")
        }
        is Actions.UnKnown -> {
            println(message)
        }
    }
}