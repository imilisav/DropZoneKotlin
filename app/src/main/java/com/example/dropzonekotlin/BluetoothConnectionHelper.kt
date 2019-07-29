package com.example.dropzonekotlin

import android.os.Environment
import java.io.File

class BluetoothConnectionHelper {
    companion object {
        fun isExternalStorageWritable(): Boolean {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

        fun getPublicStorageDir(fileName: String): File {
            val mediaType = getFileType(fileName)
            val directoryType: String
            when (mediaType) {
                Constants.MEDIA_TYPE_IMAGE -> directoryType = Environment.DIRECTORY_PICTURES
                Constants.MEDIA_TYPE_VIDEO -> directoryType = Environment.DIRECTORY_MOVIES
                Constants.MEDIA_TYPE_AUDIO -> directoryType = Environment.DIRECTORY_MUSIC
                Constants.MEDIA_TYPE_PDF -> directoryType = Environment.DIRECTORY_DOCUMENTS
                else -> directoryType = Environment.DIRECTORY_DOWNLOADS
            }
            return File(Environment.getExternalStoragePublicDirectory(directoryType), fileName)
        }

        private fun getFileType(fileName: String): Int {
            var extension: String? = null
            val i = fileName.lastIndexOf('.')
            if (i > 0) {
                extension = fileName.substring(i + 1)
            }
            if (extension == null) {
                return Constants.MEDIA_TYPE_UNKNOWN
            }
            when (extension) {
                "png", "jpg" -> return Constants.MEDIA_TYPE_IMAGE
                "pdf" -> return Constants.MEDIA_TYPE_PDF
                "mp3" -> return Constants.MEDIA_TYPE_AUDIO
                "mp4" -> return Constants.MEDIA_TYPE_VIDEO
                else -> return Constants.MEDIA_TYPE_UNKNOWN
            }
        }
    }
}