package server

import ACTION
import MESSAGE
import READ
import SERVER_ADDRESS
import SERVER_PORT
import WRITE
import detectCommand
import mapper.Mappers
import model.Actions
import model.Suggestion
import model.User
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


fun main() {
    ServerO()
}

class ServerO {
    @Volatile
    private var canAccept = true
    private val MAX_CLIENTS_ACCEPTED = 10
    private var serverChannel: AsynchronousServerSocketChannel = AsynchronousServerSocketChannel.open()
    private lateinit var channel: AsynchronousSocketChannel
    private val user = User(name = "SERVER", address = "127.0.0.1", id = "0000", description = "")
    private val roundNumber = 0
    private val mappers = Mappers()
    private val validClients = ArrayList<Map<User, AsynchronousSocketChannel>>()
    private val currentClients = ArrayList<Map<User, AsynchronousSocketChannel>>()
    private val suggestions = ArrayList<Suggestion>()
    private val lastSuggestions = ArrayList<Suggestion>()
    private val removedClients = ArrayList<Map<User, AsynchronousSocketChannel>>()
    private var validUsers = ArrayList<User>()
    private val atomic = AtomicInteger(roundNumber)
    private var winnerUser = User()
    private var winnerUserFound = false

    fun readWrite(
        buf: HashMap<String?, Any?>,
        remoteAddress: SocketAddress,
        sockChannel: AsynchronousSocketChannel
    ) {
        buf.apply {
            when (get(ACTION)) {
                READ -> {
                    val m = get(MESSAGE) as ByteBuffer
                    val m2 = String(m.array()).trim()
                    val messages = m2.split("*")
                    val messageLength = messages[0]
                    val actions = messages[1].substring(0, messageLength.toInt()).detectCommand()
                    detectAction(actions, remoteAddress, sockChannel)
                }
                WRITE -> {
                    val m = get(MESSAGE) as ByteBuffer
                    val m2 = String(m.array()).trim()
                    println("has wrote : $m2")
                }
                else -> {
                }
            }
        }
    }

    private fun detectAction(actions: Actions, remoteAddress: SocketAddress, sockChannel: AsynchronousSocketChannel) {
        when (actions) {
            is Actions.Login -> {
                validClients.add(mapOf(actions.user to sockChannel))
                println(
                    "${actions.user.name} " +
                            "logged in with $remoteAddress address " +
                            "and the id: ${actions.user.id}"
                )

            }
            is Actions.Offer -> {
                addUserToClients(actions, sockChannel)
                println("${actions.user.name} offers ${actions.amount}$")
            }
            is Actions.Round -> {

            }
            is Actions.UnKnown -> {

            }
        }
    }

    private fun addUserToClients(action: Actions.Offer, sockChannel: AsynchronousSocketChannel) {
        val ids = suggestions.map { it.id }
        val userIds = validUsers.map { it.id }
        action.apply {
            if (!ids.contains(action.user.id)) {
                suggestions.add(Suggestion(user.name, amount, user.id))
            } else {
                val s = suggestions.find { suggestion -> suggestion.id == user.id }
                suggestions.remove(s)
                suggestions.add(Suggestion(user.name, amount, user.id))
            }
            if (!userIds.contains(action.user.id)) {
                validUsers.add(action.user)
            }
            currentClients.add(mapOf(user to sockChannel))
        }
    }

    private fun startWrite(sockChannel: AsynchronousSocketChannel, message: String, messageWritten: AtomicInteger) {
        val buf = ByteBuffer.allocate(10000)
        buf.put(message.toByteArray())
        buf.flip()
        messageWritten.getAndIncrement()
        sockChannel.write(buf, sockChannel, object : CompletionHandler<Int?, AsynchronousSocketChannel?> {
            override fun completed(result: Int?, channel: AsynchronousSocketChannel?) {
                channel?.let { startRead(it, messageWritten) }
            }

            override fun failed(exc: Throwable, channel: AsynchronousSocketChannel?) {
                println("Fail to write the message to server")
            }
        })
    }

    private fun startRead(sockChannel: AsynchronousSocketChannel, messageRead: AtomicInteger) {
        val buf = ByteBuffer.allocate(10000)
        try {
            sockChannel.read(buf, sockChannel, object : CompletionHandler<Int?, AsynchronousSocketChannel?> {
                override fun completed(result: Int?, channel: AsynchronousSocketChannel?) {
                    //message is read from server
                    messageRead.getAndIncrement()
                    //println("Read message:" + String(buf.array()))


                    var readInfo: HashMap<String?, Any?> = HashMap()
                    readInfo[ACTION] = READ
                    readInfo[MESSAGE] = buf
                    readWrite(readInfo, sockChannel.remoteAddress, sockChannel)
                    val sc = Scanner(System.`in`)
                    var byteBuffer: ByteBuffer = ByteBuffer.allocate(10000)
                    //readInfo[ACTION] = READ
                    readInfo[MESSAGE] = byteBuffer
                    val user = User("server")
                    readInfo[ACTION] = WRITE
                }

                override fun failed(exc: Throwable, channel: AsynchronousSocketChannel?) {
                    println("fail to read message from server")
                }
            })

        } catch (e: Exception) {
        }

    }

    private fun detectCommand(command: String?): Actions {
        return command?.let {
            user.detectCommand(it)
        } ?: Actions.UnKnown("$command not found!!")
    }

    private fun getAuctionCommand(product: String?): String {
        product?.let {
            val message = mappers.auction(it, "")
            return message.length.toString() + "*" + message
        }
        return ""
    }

    private fun getRoundCommand(suggestions: List<Suggestion>, desc: String): String {
        val message = mappers.round(suggestions, desc)
        return message.length.toString() + "*" + message
    }

    private fun getEndCommand(user: User, amount: String): String {
        val message = mappers.end(user, amount)
        return message.length.toString() + "*" + message
    }

    private fun List<Suggestion>.sort(): List<Suggestion> {
        return sortedByDescending { it.amount.toInt() }
    }

    init {
        val hostAddress = InetSocketAddress(SERVER_ADDRESS, SERVER_PORT)
        serverChannel.bind(hostAddress)
        // try {
        println("server waits...")
        if (canAccept) {
            serverChannel.accept(null, object : CompletionHandler<AsynchronousSocketChannel?, Any?> {
                override fun completed(result: AsynchronousSocketChannel?, attachment: Any?) {
                    if (serverChannel.isOpen) {
                        serverChannel.accept(null, this)
                    }
                    result?.let { socketChannel ->
                        channel = socketChannel
                        val messageRead = AtomicInteger(0)
                        //println("remote Address is: " + socketChannel.remoteAddress)
                        var readInfo: HashMap<String?, Any?> = HashMap()
                        var byteBuffer: ByteBuffer = ByteBuffer.allocate(10000)
                        readInfo[ACTION] = READ
                        readInfo[MESSAGE] = byteBuffer
                        startRead(socketChannel, messageRead)
                    }
                }

                override fun failed(exc: Throwable?, attachment: Any?) {
                    println("failed")
                }
            })
        }
        while (true) {
            val sc = Scanner(System.`in`)
            detectInputCommand(sc.next())
        }
    }

    private fun detectInputCommand(command: String) {
        when (val actions = detectCommand(command)) {
            is Actions.Login -> {
                println("SERVER? ... LOGIN?")
            }
            is Actions.Offer -> {
                println("Cant")
            }
            is Actions.Round -> {
                validClients.clear()
                validClients.addAll(currentClients)
                validUsers.forEach { user ->
                    validClients.forEach { map ->
                        if (map.containsKey(user)) {
                            // println("map: ${map[user]}")
                            map[user]?.let { channel ->
                                startWrite(
                                    channel,
                                    getRoundCommand(suggestions.sort(), suggestions.sort().toString()),
                                    atomic
                                )
                            }
                        }
                    }
                }
                validUsers.clear()
                lastSuggestions.clear()
                lastSuggestions.addAll(suggestions)
                suggestions.clear()
                currentClients.clear()
            }
            is Actions.End -> {
                lastSuggestions.addAll(suggestions)
                val winnerId = lastSuggestions.sort()[0].id
                validClients.forEach { map ->
                    if (!winnerUserFound)
                        map.keys.forEach {
                            if (it.id == winnerId) {
                                winnerUser = it
                                winnerUserFound = true
                            }
                        }
                }
                validClients.forEach { map ->
                    map.forEach {
                        startWrite(
                            it.value,
                            getEndCommand(winnerUser, lastSuggestions.sort()[0].amount),
                            AtomicInteger(roundNumber)
                        )
                    }
                }
                canAccept = true
            }
            is Actions.Auction -> {
                canAccept = false
                validClients.forEach { map ->
                    map.forEach {
                        startWrite(it.value, getAuctionCommand(actions.product), AtomicInteger(roundNumber))
                    }
                }
            }
            is Actions.UnKnown -> {
                println("Unknown Command!")
            }
        }
    }

}