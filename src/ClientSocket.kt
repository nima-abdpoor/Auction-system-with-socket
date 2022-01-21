import mapper.Mappers
import model.User
import java.io.DataInputStream
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.atomic.AtomicInteger

class ClientSocket {
    private val socket: Socket = Socket(InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT)
    private var outputStream = socket.getOutputStream()
    private var inputStream = socket.getInputStream()
    private val mappers = Mappers()

    var din = DataInputStream(inputStream)
    var writer: PrintWriter = PrintWriter(outputStream)

    fun read(): String {
        return din.readLine()
    }

    private fun write(message: String) {
        writer.write(message)
        writer.flush()
    }

    fun getLocalPort() = socket.localPort

    fun login(user: User) {
        val message = mappers.login(user)
        write(message.length.toString() + mappers.login(user))
    }


    fun offer(user: User, amount: String) {
        val message = mappers.offer(user, amount)
        write(message.length.toString() + message)
    }
}