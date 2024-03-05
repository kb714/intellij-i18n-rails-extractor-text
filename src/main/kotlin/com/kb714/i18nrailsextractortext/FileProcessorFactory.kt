package com.kb714.i18nrailsextractortext

import com.kb714.i18nrailsextractortext.processors.FileProcessor
import com.kb714.i18nrailsextractortext.processors.RbFileProcessor
import com.kb714.i18nrailsextractortext.processors.ErbFileProcessor

object FileProcessorFactory {
    fun getProcessor(fileExtension: String?): FileProcessor? {
        return when (fileExtension) {
            "rb" -> RbFileProcessor()
            "erb" -> ErbFileProcessor()
            // quizás sea bueno agregar archivos como jbuilder
            else -> null
        }
    }
}