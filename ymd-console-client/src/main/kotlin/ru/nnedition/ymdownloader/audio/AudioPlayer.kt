package ru.nnedition.ymdownloader.audio

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineEvent

object AudioPlayer {
    private val sounds: MutableMap<AudioType, ByteArray> = HashMap()

    fun preloadAudio() {
        AudioType.entries.forEach { type ->
            val input = this.javaClass.classLoader.getResourceAsStream("audio/${type.fileName}")!!
            sounds[type] = input.readAllBytes()
        }
    }

    fun play(type: AudioType) {
        try {
            val bufferedInput = BufferedInputStream(ByteArrayInputStream(sounds[type]!!))
            val audioStream = AudioSystem.getAudioInputStream(bufferedInput)

            AudioSystem.getClip().apply {
                open(audioStream)
                addLineListener { event ->
                    if (event.type == LineEvent.Type.STOP)
                        close()
                }
                start()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}