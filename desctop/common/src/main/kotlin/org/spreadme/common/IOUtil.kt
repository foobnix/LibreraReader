package org.spreadme.common

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * copy inputsteam to outputstream
 *
 * @param input inputsteam
 * @param output outputstream
 * @throws IOException IOException
 */
@Throws(IOException::class)
fun copy(input: InputStream, output: OutputStream) {
    val readableChannel = Channels.newChannel(input)
    val writableChannl = Channels.newChannel(output)
    copy(readableChannel, writableChannl)
}

/**
 * copy ReadableByteChannel to WritableByteChannel
 *
 * @param readableChan ReadableByteChannel
 * @param writableChan WritableByteChannel
 * @throws IOException IOException
 */
@Throws(IOException::class)
fun copy(readableChan: ReadableByteChannel, writableChan: WritableByteChannel) {
    val byteBuffer = ByteBuffer.allocate(8092)
    while (readableChan.read(byteBuffer) != -1) {
        byteBuffer.flip()
        writableChan.write(byteBuffer)
        byteBuffer.compact()
    }
    byteBuffer.flip()
    while (byteBuffer.hasRemaining()) {
        writableChan.write(byteBuffer)
    }
}

/**
 * create file or directories
 *
 * @param path path
 * @param isFile is file?
 * @return File
 * @throws IOException IOException
 */
@Throws(IOException::class)
fun createFile(path: Path, isFile: Boolean): File? {
    val filePath = path.normalize()
    if (!Files.exists(filePath)) {
        if (isFile) {
            val dirPath = filePath.parent
            Files.createDirectories(dirPath)
            Files.createFile(filePath)
            return filePath.toFile()
        }
        Files.createDirectories(filePath)
    }
    return filePath.toFile()
}

fun toByteBuffer(intArray: IntArray): ByteBuffer {
    val byteBuffer = ByteBuffer.allocate(intArray.size * 4)
    val intBuffer = byteBuffer.asIntBuffer()
    intBuffer.put(intArray)
    return byteBuffer
}

fun IntBuffer.toIntArray(): IntArray {
    if (this.hasArray()) {
        return if (this.arrayOffset() == 0) {
            this.array()
        } else {
            Arrays.copyOfRange(
                this.array(),
                this.arrayOffset(),
                this.array().size
            )
        }
    }

    this.rewind()
    val intArray = IntArray(this.remaining())
    this.get(intArray)

    return intArray
}

fun toFile(path: Path, byteBuffer: ByteBuffer) {
    FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE).use {
        val mappedByteBuffer = it.map(FileChannel.MapMode.READ_WRITE, 0, byteBuffer.remaining().toLong())
        mappedByteBuffer.put(byteBuffer)
    }
}

fun toMappedByteBuffer(path: Path): ByteBuffer =
    FileChannel.open(path, StandardOpenOption.READ).use {
        val mappedByteBuffer = it.map(FileChannel.MapMode.READ_ONLY, 0, it.size())
        val bytes = ByteArray(mappedByteBuffer.remaining())
        mappedByteBuffer.get(bytes)
        ByteBuffer.wrap(bytes)
    }