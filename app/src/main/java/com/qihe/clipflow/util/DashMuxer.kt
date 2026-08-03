package com.qihe.clipflow.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/** Remuxes Bilibili AVC/AAC DASH fragments without re-encoding. */
object DashMuxer {
    fun mux(videoFile: File, audioFile: File, outputFile: File): Result<File> = runCatching {
        outputFile.parentFile?.mkdirs()
        outputFile.delete()
        val video = MediaExtractor()
        val audio = MediaExtractor()
        try {
            video.setDataSource(videoFile.absolutePath)
            audio.setDataSource(audioFile.absolutePath)
            val videoTrack = findTrack(video, "video/") ?: error("Unsupported video stream")
            val audioTrack = findTrack(audio, "audio/") ?: error("Unsupported audio stream")
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            try {
                val muxVideo = muxer.addTrack(video.getTrackFormat(videoTrack))
                val muxAudio = muxer.addTrack(audio.getTrackFormat(audioTrack))
                muxer.start()
                copyTrack(video, videoTrack, muxer, muxVideo)
                copyTrack(audio, audioTrack, muxer, muxAudio)
                muxer.stop()
            } finally { muxer.release() }
        } finally { video.release(); audio.release() }
        outputFile
    }

    private fun findTrack(extractor: MediaExtractor, prefix: String): Int? = (0 until extractor.trackCount).firstOrNull {
        extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith(prefix) == true
    }

    private fun copyTrack(extractor: MediaExtractor, sourceTrack: Int, muxer: MediaMuxer, outputTrack: Int) {
        extractor.selectTrack(sourceTrack)
        val buffer = ByteBuffer.allocate(2 * 1024 * 1024)
        val info = MediaCodec.BufferInfo()
        while (true) {
            info.offset = 0
            info.size = extractor.readSampleData(buffer, 0)
            if (info.size < 0) break
            info.presentationTimeUs = extractor.sampleTime
            info.flags = extractor.sampleFlags
            muxer.writeSampleData(outputTrack, buffer, info)
            extractor.advance()
        }
        extractor.unselectTrack(sourceTrack)
    }
}
