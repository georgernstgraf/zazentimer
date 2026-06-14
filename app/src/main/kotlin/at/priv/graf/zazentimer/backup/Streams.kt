package at.priv.graf.zazentimer.backup

import at.priv.graf.zazentimer.Constants
import java.io.InputStream
import java.io.OutputStream

object Streams {
    fun copy(
        input: InputStream,
        output: OutputStream,
        bufferSize: Int = Constants.IO_BUFFER_SIZE,
    ) {
        val buffer = ByteArray(bufferSize)
        var read = input.read(buffer)
        while (read != -1) {
            output.write(buffer, 0, read)
            read = input.read(buffer)
        }
    }
}
