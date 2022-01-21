package clients

import MESSAGE
import READ
import WRITE
import detectCommand
import mapper.Mappers
import model.ACTION
import model.Actions
import model.User
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger

/**
 *
 * @author Steven
 */

fun main() {
    try {
        val messageWritten = AtomicInteger(0)
        val messageRead = AtomicInteger(0)
        EchoClient4("127.0.0.1", 2000, "echo test", messageWritten, messageRead)
        while (messageRead.get() != 1000) {
            Thread.sleep(1000)
            // System.out.println( "message write:" + messageWritten );
            // System.out.println( "message read:" + messageRead );
        }
    } catch (ex: Exception) {
        Logger.getLogger(EchoClient4::class.java.name).log(Level.SEVERE, null, ex)
    }
}

class EchoClient4(
    host: String?,
    port: Int,
    message: String?,
    messageWritten: AtomicInteger,
    messageRead: AtomicInteger
) {
    val user = User("Client_4", "", "111", "")
    private var socketChannel: AsynchronousSocketChannel? = null

    private fun startRead(sockChannel: AsynchronousSocketChannel, messageRead: AtomicInteger) {
        val buf = ByteBuffer.allocate(10000)
        sockChannel.read(buf, sockChannel, object : CompletionHandler<Int?, AsynchronousSocketChannel?> {
            override fun completed(result: Int?, channel: AsynchronousSocketChannel?) {
                //message is read from server
                messageRead.getAndIncrement()
                //print the message
                val readInfo: HashMap<String?, Any?> = HashMap()
                readInfo[ACTION] = READ
                readInfo[MESSAGE] = buf
                //println("Read message:" + String(buf.array()))
                channel?.let {
                    readWrite(readInfo, it.remoteAddress, it)
                }
            }

            override fun failed(exc: Throwable, channel: AsynchronousSocketChannel?) {
                println("fail to read message from server")
            }
        })
    }

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

            }
            is Actions.Offer -> {

            }
            is Actions.Round -> {
                println(
                    "Round Completed!!!\n" +
                            "${actions.desc} founded!"
                )
            }
            is Actions.Auction -> {
                println(
                    "${actions.product} received !! with BasePrice: ${actions.basePrice}" +
                            "\n If You Want This Product, Please Send Your Offer :)"
                )
            }
            is Actions.UnKnown -> {
                println()
            }
            is Actions.End -> {
                if (actions.user.id == user.id)
                    println(
                        "Auction Completed!!!\n" +
                                "Congratulations You Win!!" +
                                "\n Now You Should Pay ${actions.amount}$ :)"
                    )
                else
                    println(
                        "Auction Completed!!!\n" +
                                "The Winner is: ${actions.user.name} with id:${actions.user.id}" +
                                "\n${actions.user.name} bought this item with ${actions.amount}$"
                    )
            }
        }
    }

    private fun startWrite(sockChannel: AsynchronousSocketChannel, message: String, messageWritten: AtomicInteger) {
        val buf = ByteBuffer.allocate(10000)
        buf.put(message.toByteArray())
        buf.flip()
        messageWritten.getAndIncrement()
        sockChannel.write(buf, sockChannel, object : CompletionHandler<Int?, AsynchronousSocketChannel?> {
            override fun completed(result: Int?, channel: AsynchronousSocketChannel?) {
                socketChannel?.let {
                    startRead(it, messageWritten)
                }
            }

            override fun failed(exc: Throwable, channel: AsynchronousSocketChannel?) {
                println("Fail to write the message to server")
            }
        })
    }

    fun login(user: User): String {
        val mappers = Mappers()
        val message = mappers.login(user)
        return message.length.toString() + "*" + mappers.login(user)
    }

    fun offer(user: User, amount: String): String {
        val mappers = Mappers()
        val message = mappers.offer(user, amount)
        return message.length.toString() + "*" + message
    }

    init {
        //create a socket channel
        val sockChannel = AsynchronousSocketChannel.open()

        //try to connect to the server side
        sockChannel.connect(
            InetSocketAddress(host, port),
            sockChannel,
            object : CompletionHandler<Void?, AsynchronousSocketChannel> {
                override fun completed(result: Void?, channel: AsynchronousSocketChannel) {
                    var readInfo: HashMap<String?, Any?> = HashMap()
                    var byteBuffer: ByteBuffer = ByteBuffer.allocate(10000)
                    readInfo[ACTION] = READ
                    readInfo[MESSAGE] = byteBuffer
                    readInfo[ACTION] = WRITE
                    socketChannel = channel
                    startWrite(channel, login(user), messageWritten)
                    //channel?.write(byteBuffer, readInfo, handler)
                    //write an message to server side
                }

                override fun failed(exc: Throwable, channel: AsynchronousSocketChannel) {
                    println("fail to connect to server")
                }
            })
        while (true) {
            val sc = Scanner(System.`in`)
            val m = sc.next()
            if (m.startsWith("Offer")){
                val offers  = m.split(":")
                startWrite(sockChannel, offer(user, offers[1]), messageWritten)
            }else println("Is Not Valid :(")

        }
    }
}

