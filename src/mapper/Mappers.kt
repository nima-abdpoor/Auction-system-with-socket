package mapper

import model.*
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

class Mappers {
    private val splitWord = ","
    private val separator = ":"
    private val obj = JSONObject()
    private val jsonArray = JSONArray()

    fun userToString(user: User, action: String): String {
        val sb = StringBuilder()
        sb.apply {
            append(action)
            append(splitWord)
        }
        return sb.toString()
    }

    fun getUser(message: String): User {
        val jsonObject = JSONParser().parse(message) as JSONObject
        return User(
            name = jsonObject["name"].toString(),
            address = jsonObject["address"].toString(),
            description = jsonObject["description"].toString(),
            id = jsonObject["id"].toString()
        )

    }

    fun login(user: User): String {
        obj.clear()
        obj[ACTION] = LOGIN
        obj["name"] = user.name
        obj["id"] = user.id
        obj["address"] = user.address
        obj["description"] = user.description
        return obj.toJSONString()
    }

    fun offer(user: User, amount: String): String {
        obj.clear()
        obj[ACTION] = OFFER
        obj["name"] = user.name
        obj["id"] = user.id
        obj["address"] = user.address
        obj["description"] = user.description
        obj["amount"] = amount
        return obj.toJSONString()
    }

    fun round(suggestions: List<Suggestion>, desc : String): String {
        obj.clear()
        jsonArray.clear()
        obj[ACTION] = ROUND
        obj[SUGGESTION] = suggestions.toString()
        return obj.toJSONString()
    }

    fun end(user: User, amount: String): String {
        obj.clear()
        jsonArray.clear()
        obj[ACTION] = END
        obj["name"] = user.name
        obj["id"] = user.id
        obj["address"] = user.address
        obj["description"] = user.description
        obj["amount"] = amount
        return obj.toJSONString()
    }

    fun auction(productName: String, basePrice: String): String {
        obj.clear()
        obj[ACTION] = AUCTION
        obj["productName"] =productName
        obj["basePrice"] =basePrice
        return obj.toJSONString()
    }

}